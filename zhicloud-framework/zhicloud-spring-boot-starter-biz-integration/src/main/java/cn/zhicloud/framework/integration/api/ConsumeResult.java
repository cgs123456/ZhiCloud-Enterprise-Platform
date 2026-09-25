package cn.zhicloud.framework.integration.api;

/**
 * 消费结果（V100）
 *
 * @author 智云集成治理
 */
public enum ConsumeResult {

    /** 本次真正执行了业务处理器并成功 */
    PROCESSED,
    /** 该消费者此前已处理过该事件（或正被其他线程处理），本次跳过，业务处理器未执行 */
    DUPLICATE,

}