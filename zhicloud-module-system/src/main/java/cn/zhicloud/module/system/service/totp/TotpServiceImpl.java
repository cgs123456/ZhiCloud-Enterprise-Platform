package cn.zhicloud.module.system.service.totp;

import cn.hutool.core.util.StrUtil;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.system.enums.ErrorCodeConstants.AUTH_TOTP_CODE_USED;

/**
 * TOTP 双因素认证 Service 实现类
 *
 * 基于 dev.samstevens.totp 库实现，符合 RFC 6238 标准
 *
 * @author 智云
 */
@Service
@Slf4j
public class TotpServiceImpl implements TotpService {

    /**
     * 发行方名称（显示在 Authenticator App 中）
     */
    private static final String ISSUER = "zhicloud";

    /**
     * 防重放 Redis Key 前缀：{@code system:totp:used:{userId}:{code}}
     */
    private static final String TOTP_USED_KEY_PREFIX = "system:totp:used:";

    /**
     * 已使用验证码的保留时长（秒）：验证器允许 ±1 个时间窗口（前后 30 秒），
     * 90 秒可完整覆盖同一验证码的有效生命周期，防止窗口内重放
     */
    private static final Duration TOTP_USED_TTL = Duration.ofSeconds(90);

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    /**
     * 验证器：默认允许 ±1 个时间窗口（即前后 30 秒）的容差
     */
    private final DefaultCodeVerifier verifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public String generateSecret() {
        return secretGenerator.generate();
    }

    @Override
    public boolean verifyTotp(String secret, String code, Long userId) {
        if (StrUtil.isEmpty(secret) || StrUtil.isEmpty(code)) {
            return false;
        }
        if (!verifier.isValidCode(secret, code)) {
            return false;
        }
        // 防重放：验证通过后原子标记该验证码已使用；已存在则说明被重复提交，直接拒绝
        Boolean firstUse = stringRedisTemplate.opsForValue().setIfAbsent(
                TOTP_USED_KEY_PREFIX + userId + ":" + code, "1", TOTP_USED_TTL);
        if (!Boolean.TRUE.equals(firstUse)) {
            log.warn("[verifyTotp][TOTP 验证码重复使用，userId={}]", userId);
            throw exception(AUTH_TOTP_CODE_USED);
        }
        return true;
    }

    @Override
    public String getTotpUri(String secret, String username) {
        QrData data = new QrData.Builder()
                .label(username)
                .secret(secret)
                .issuer(ISSUER)
                .algorithm(HashingAlgorithm.SHA1) // 兼容大部分 Authenticator App（Google Authenticator 等）
                .digits(6)
                .period(30)
                .build();
        return data.getUri();
    }

}
