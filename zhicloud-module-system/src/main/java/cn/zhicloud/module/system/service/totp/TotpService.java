package cn.zhicloud.module.system.service.totp;

/**
 * TOTP 双因素认证 Service 接口
 *
 * 基于 RFC 6238 TOTP（Time-based One-Time Password）算法
 *
 * @author 智云
 */
public interface TotpService {

    /**
     * 生成 TOTP 密钥
     *
     * @return Base32 编码的密钥
     */
    String generateSecret();

    /**
     * 验证 TOTP 验证码（含防重放：验证通过的验证码在 90 秒内不可重复使用）
     *
     * @param secret Base32 编码的密钥
     * @param code   用户输入的验证码
     * @param userId 用户编号（防重放按用户隔离）
     * @return 是否验证通过；验证码已被使用时抛出 ServiceException
     */
    boolean verifyTotp(String secret, String code, Long userId);

    /**
     * 生成 otpauth URI（用于二维码扫描）
     *
     * @param secret Base32 编码的密钥
     * @param username 用户名
     * @return otpauth URI
     */
    String getTotpUri(String secret, String username);

}
