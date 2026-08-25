package cn.zhicloud.module.qms.framework.electronicsignature;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 电子签名注解（21 CFR Part 11）
 *
 * <p>标注在需要电子签名的方法上，由 {@link ElectronicSignatureAspect} 拦截处理。
 * <p>拦截时从请求头获取 {@code X-Electronic-Signature-Username} 和 {@code X-Electronic-Signature-Password}，
 * 调用 AdminAuthService 验证密码，验证通过后记录签名日志。
 *
 * @author 智云
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ElectronicSignature {

    /**
     * 签名含义（如"批准"、"审核"）
     */
    String meaning();

    /**
     * 是否需要签名理由
     */
    boolean requireReason() default false;

}
