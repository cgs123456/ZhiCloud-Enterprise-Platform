package cn.zhicloud.module.system.service.sms;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.map.MapUtil;
import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import cn.zhicloud.module.system.api.sms.dto.code.SmsCodeSendReqDTO;
import cn.zhicloud.module.system.api.sms.dto.code.SmsCodeUseReqDTO;
import cn.zhicloud.module.system.api.sms.dto.code.SmsCodeValidateReqDTO;
import cn.zhicloud.module.system.dal.dataobject.sms.SmsCodeDO;
import cn.zhicloud.module.system.dal.mysql.sms.SmsCodeMapper;
import cn.zhicloud.module.system.enums.sms.SmsSceneEnum;
import cn.zhicloud.module.system.framework.sms.config.SmsCodeProperties;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;

import static cn.hutool.core.util.RandomUtil.randomInt;
import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.framework.common.util.date.DateUtils.isToday;
import static cn.zhicloud.module.system.enums.ErrorCodeConstants.*;

/**
 * 短信验证码 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class SmsCodeServiceImpl implements SmsCodeService {

    /**
     * 无租户上下文时使用的系统默认租户。
     *
     * <p>登录发码等入口可能早于租户解析执行。验证码不能被当作全局共享数据，
     * 因此这里显式回退到默认租户，而不是忽略租户条件。
     */
    private static final long DEFAULT_TENANT_ID = 0L;

    @Resource
    private SmsCodeProperties smsCodeProperties;

    @Resource
    private SmsCodeMapper smsCodeMapper;

    @Resource
    private SmsSendService smsSendService;

    @Override
    public void sendSmsCode(SmsCodeSendReqDTO reqDTO) {
        executeWithTenantContext(() -> {
            SmsSceneEnum sceneEnum = SmsSceneEnum.getCodeByScene(reqDTO.getScene());
            Assert.notNull(sceneEnum, "验证码场景({}) 查找不到配置", reqDTO.getScene());
            // 创建验证码
            String code = createSmsCode(reqDTO.getMobile(), reqDTO.getScene(), reqDTO.getCreateIp());
            // 发送验证码
            smsSendService.sendSingleSms(reqDTO.getMobile(), null, null,
                    sceneEnum.getTemplateCode(), MapUtil.of("code", code));
        });
    }

    private String createSmsCode(String mobile, Integer scene, String ip) {
        // 校验是否可以发送验证码，不用筛选场景
        SmsCodeDO lastSmsCode = smsCodeMapper.selectLastByMobile(mobile, null, null);
        if (lastSmsCode != null) {
            if (LocalDateTimeUtil.between(lastSmsCode.getCreateTime(), LocalDateTime.now()).toMillis()
                    < smsCodeProperties.getSendFrequency().toMillis()) { // 发送过于频繁
                throw exception(SMS_CODE_SEND_TOO_FAST);
            }
            if (isToday(lastSmsCode.getCreateTime()) && // 必须是今天，才能计算超过当天的上限
                    lastSmsCode.getTodayIndex() >= smsCodeProperties.getSendMaximumQuantityPerDay()) { // 超过当天发送的上限。
                throw exception(SMS_CODE_EXCEED_SEND_MAXIMUM_QUANTITY_PER_DAY);
            }
            // TODO 芋艿：提升，每个 IP 每天可发送数量
            // TODO 芋艿：提升，每个 IP 每小时可发送数量
        }

        // 创建验证码记录
        String code = String.format("%0" + smsCodeProperties.getEndCode().toString().length() + "d",
                randomInt(smsCodeProperties.getBeginCode(), smsCodeProperties.getEndCode() + 1));
        SmsCodeDO newSmsCode = SmsCodeDO.builder().mobile(mobile).code(code).scene(scene)
                .todayIndex(lastSmsCode != null && isToday(lastSmsCode.getCreateTime()) ? lastSmsCode.getTodayIndex() + 1 : 1)
                .createIp(ip).used(false).build();
        smsCodeMapper.insert(newSmsCode);
        return code;
    }

    @Override
    public void useSmsCode(SmsCodeUseReqDTO reqDTO) {
        executeWithTenantContext(() -> {
            // 检测验证码是否有效
            SmsCodeDO lastSmsCode = validateSmsCode0(reqDTO.getMobile(), reqDTO.getCode(), reqDTO.getScene());
            // 使用验证码
            smsCodeMapper.updateById(SmsCodeDO.builder().id(lastSmsCode.getId())
                    .used(true).usedTime(LocalDateTime.now()).usedIp(reqDTO.getUsedIp()).build());
        });
    }

    @Override
    public void validateSmsCode(SmsCodeValidateReqDTO reqDTO) {
        executeWithTenantContext(() -> validateSmsCode0(reqDTO.getMobile(), reqDTO.getCode(), reqDTO.getScene()));
    }

    /**
     * 在明确的租户上下文中执行验证码操作，并保留原始业务异常。
     *
     * <p>不能复用 {@code TenantUtils#execute}，因为其 Callable 版本会把
     * {@code SMS_CODE_NOT_FOUND} 等业务异常包装成 INTERNAL_SERVER_ERROR。
     */
    private void executeWithTenantContext(Runnable action) {
        Long oldTenantId = TenantContextHolder.getTenantId();
        boolean oldIgnore = TenantContextHolder.isIgnore();
        try {
            TenantContextHolder.setTenantId(oldTenantId != null ? oldTenantId : DEFAULT_TENANT_ID);
            TenantContextHolder.setIgnore(false);
            action.run();
        } finally {
            TenantContextHolder.setTenantId(oldTenantId);
            TenantContextHolder.setIgnore(oldIgnore);
        }
    }

    private SmsCodeDO validateSmsCode0(String mobile, String code, Integer scene) {
        // 校验验证码
        SmsCodeDO lastSmsCode = smsCodeMapper.selectLastByMobile(mobile, code, scene);
        // 若验证码不存在，抛出异常
        if (lastSmsCode == null) {
            throw exception(SMS_CODE_NOT_FOUND);
        }
        // 超过时间
        if (LocalDateTimeUtil.between(lastSmsCode.getCreateTime(), LocalDateTime.now()).toMillis()
                >= smsCodeProperties.getExpireTimes().toMillis()) { // 验证码已过期
            throw exception(SMS_CODE_EXPIRED);
        }
        // 判断验证码是否已被使用
        if (Boolean.TRUE.equals(lastSmsCode.getUsed())) {
            throw exception(SMS_CODE_USED);
        }
        return lastSmsCode;
    }

}
