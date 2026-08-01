package cn.iocoder.yudao.framework.tracer.core.annotation;

import java.lang.annotation.*;

/**
 * 打印业务编号 / 业务类型注解
 *
 * <p>O3 修复：已从 SkyWalking 迁移至 OpenTelemetry。{@code biz.id} 和 {@code biz.type}
 * 作为 Span Tag 自动写入 OTel Span，可通过 Jaeger/Tempo 的 Tag 搜索直接查询。
 *
 * @author 麻薯
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface BizTrace {

    /**
     * 业务编号 tag 名
     */
    String ID_TAG = "biz.id";
    /**
     * 业务类型 tag 名
     */
    String TYPE_TAG = "biz.type";

    /**
     * @return 操作名
     */
    String operationName() default "";

    /**
     * @return 业务编号
     */
    String id();

    /**
     * @return 业务类型
     */
    String type();

}
