package cn.zhicloud.framework.integration.api;

/**
 * 事件业务处理器（V100）
 *
 * <p>允许抛出受检异常：{@code EventInboxService#consume} 会先落库 FAILED / DEAD 再把原异常抛回调用方，
 * 由中继决定发件箱的重试策略，不吞异常。
 *
 * @author 智云集成治理
 */
@FunctionalInterface
public interface EventHandler {

    void handle() throws Exception;

}