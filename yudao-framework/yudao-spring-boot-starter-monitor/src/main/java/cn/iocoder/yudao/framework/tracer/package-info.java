/**
 * 链路追踪模块（O3 修复：已统一迁移至 OpenTelemetry，移除 SkyWalking 依赖）。
 *
 * <p>通过 micrometer-tracing-bridge-otel 桥接，自动追踪 HTTP/DB/Redis/Kafka 调用链，
 * 上报至 Jaeger/Tempo。业务标签通过 {@link cn.iocoder.yudao.framework.tracer.core.annotation.BizTrace} 注解注入。
 *
 * @author 芋道源码
 */
package cn.iocoder.yudao.framework.tracer;
