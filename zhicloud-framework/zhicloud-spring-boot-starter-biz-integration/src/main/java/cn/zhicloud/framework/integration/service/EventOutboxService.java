package cn.zhicloud.framework.integration.service;

import cn.zhicloud.framework.integration.api.EventAppendReq;
import cn.zhicloud.framework.integration.dal.dataobject.EventOutboxDO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 事件发件箱 Service（V100）
 *
 * <p>{@link #append} 必须在业务事务内调用（与业务表同库同事务），这是 Transactional Outbox 的全部意义：
 * 业务提交则事件必存在，业务回滚则事件一并消失。
 *
 * @author 智云集成治理
 */
public interface EventOutboxService {

    /**
     * 登记事件（幂等：同一 eventId 重复登记返回已有主键，不会产生第二行）
     *
     * @return 发件箱主键
     */
    Long append(EventAppendReq req);

    /**
     * 扫描可被领取的事件
     */
    List<EventOutboxDO> selectClaimable(LocalDateTime now, int limit);

    /**
     * CAS 领取：多节点并发下只有一个节点能领到；成功返回本次 owner，失败返回 {@code null}。
     */
    String claim(Long id, LocalDateTime now);

    /**
     * 标记投递完成（仅当前 owner 持有的 PUBLISHING 可迁移）
     */
    boolean markPublished(Long id, String owner);

    /**
     * 失败结算：只有当前 owner 能按指数退避重新入队或转死信
     */
    void markRetryOrDead(Long id, String owner, Throwable cause);

    /**
     * 待投递堆积量（NEW + PUBLISHING），供监控告警
     */
    long countBacklog();

    /**
     * 死信量，供监控告警
     */
    long countDead();

    /**
     * 最老未投递事件已等待秒数（NEW + PUBLISHING），供监控告警
     *
     * <p>没有待投递事件时返回 0。该值直接反映事件从业务发生到被中继之间的
     * 端到端滞留时间，不能只依赖队列长度判断系统是否健康。
     */
    long oldestPendingSeconds();


}