package cn.zhicloud.module.system.controller.admin.user;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.security.config.SecurityProperties;
import cn.zhicloud.module.system.controller.admin.user.vo.totp.TotpDisableReqVO;
import cn.zhicloud.module.system.controller.admin.user.vo.totp.TotpEnableRespVO;
import cn.zhicloud.module.system.controller.admin.user.vo.totp.TotpQrcodeRespVO;
import cn.zhicloud.module.system.dal.dataobject.user.AdminUserDO;
import cn.zhicloud.module.system.service.totp.TotpService;
import cn.zhicloud.module.system.service.user.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.framework.common.pojo.CommonResult.success;
import static cn.zhicloud.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.zhicloud.module.system.enums.ErrorCodeConstants.*;

@Tag(name = "管理后台 - TOTP 双因素认证")
@RestController
@RequestMapping("/system/user/totp")
@Validated
@Slf4j
public class TotpController {

    @Resource
    private TotpService totpService;
    @Resource
    private AdminUserService userService;
    @Resource
    private SecurityProperties securityProperties;

    @PostMapping("/enable")
    @Operation(summary = "绑定 TOTP", description = "生成 TOTP 密钥并绑定，返回二维码 URI；用户需立即使用 Authenticator App 扫描二维码")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    public CommonResult<TotpEnableRespVO> enableTotp() {
        // 校验 TOTP 功能是否开启
        validateTotpFeatureEnabled();
        // 校验用户是否已绑定 TOTP
        AdminUserDO user = userService.getUser(getLoginUserId());
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }
        if (Boolean.TRUE.equals(user.getTotpEnabled())) {
            throw exception(AUTH_TOTP_ALREADY_ENABLED);
        }
        // 生成新密钥，保存到用户（启用 TOTP）
        String secret = totpService.generateSecret();
        userService.updateUserTotp(user.getId(), secret, Boolean.TRUE);
        // 返回密钥和二维码 URI
        String otpauthUrl = totpService.getTotpUri(secret, user.getUsername());
        return success(TotpEnableRespVO.builder()
                .secret(secret)
                .otpauthUrl(otpauthUrl)
                .build());
    }

    @PostMapping("/disable")
    @Operation(summary = "解绑 TOTP", description = "需要验证当前 TOTP 验证码后才能解绑")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    public CommonResult<Boolean> disableTotp(@Valid @RequestBody TotpDisableReqVO reqVO) {
        // 校验用户是否存在且已启用 TOTP
        AdminUserDO user = userService.getUser(getLoginUserId());
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }
        if (!Boolean.TRUE.equals(user.getTotpEnabled()) || user.getTotpSecret() == null) {
            throw exception(AUTH_TOTP_NOT_ENABLED);
        }
        // 校验 TOTP 验证码（totpSecret 由 EncryptTypeHandler 自动解密；含 90 秒防重放）
        if (!totpService.verifyTotp(user.getTotpSecret(), reqVO.getTotpCode(), user.getId())) {
            throw exception(AUTH_TOTP_CODE_ERROR);
        }
        // 清除 TOTP 信息
        userService.updateUserTotp(user.getId(), null, Boolean.FALSE);
        return success(true);
    }

    @GetMapping("/qrcode")
    @Operation(summary = "获取 TOTP 二维码", description = "返回当前用户的 otpauth URI，用于生成二维码")
    public CommonResult<TotpQrcodeRespVO> getTotpQrcode() {
        // 校验 TOTP 功能是否开启
        validateTotpFeatureEnabled();
        // 校验用户是否存在且已绑定 TOTP
        AdminUserDO user = userService.getUser(getLoginUserId());
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }
        if (user.getTotpSecret() == null) {
            throw exception(AUTH_TOTP_SECRET_NOT_EXISTS);
        }
        // 返回二维码 URI（totpSecret 由 EncryptTypeHandler 自动解密）
        String otpauthUrl = totpService.getTotpUri(user.getTotpSecret(), user.getUsername());
        return success(TotpQrcodeRespVO.builder()
                .otpauthUrl(otpauthUrl)
                .build());
    }

    /**
     * 校验 TOTP 功能是否开启
     */
    private void validateTotpFeatureEnabled() {
        Boolean enabled = securityProperties.getTotp() != null
                ? securityProperties.getTotp().getEnabled() : Boolean.TRUE;
        if (!Boolean.TRUE.equals(enabled)) {
            throw exception(AUTH_TOTP_DISABLED);
        }
    }

}
