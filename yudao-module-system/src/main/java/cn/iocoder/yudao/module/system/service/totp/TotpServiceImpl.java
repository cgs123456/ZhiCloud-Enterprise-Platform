package cn.iocoder.yudao.module.system.service.totp;

import cn.hutool.core.util.StrUtil;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * TOTP 双因素认证 Service 实现类
 *
 * 基于 dev.samstevens.totp 库实现，符合 RFC 6238 标准
 *
 * @author 芋道源码
 */
@Service
@Slf4j
public class TotpServiceImpl implements TotpService {

    /**
     * 发行方名称（显示在 Authenticator App 中）
     */
    private static final String ISSUER = "yudao";

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    /**
     * 验证器：默认允许 ±1 个时间窗口（即前后 30 秒）的容差
     */
    private final DefaultCodeVerifier verifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());

    @Override
    public String generateSecret() {
        return secretGenerator.generate();
    }

    @Override
    public boolean verifyTotp(String secret, String code) {
        if (StrUtil.isEmpty(secret) || StrUtil.isEmpty(code)) {
            return false;
        }
        return verifier.isValidCode(secret, code);
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
