package cn.zhicloud.framework.integration.enums;

/**
 * 集成事件状态字面量常量（V100 可靠集成数据面）
 *
 * <p>Mapper 的 {@code @Select} / {@code @Update} 注解只能拼接编译期常量，无法引用枚举的 {@code name()}，
 * 因此状态字面量集中在此，避免 SQL 里散落魔法字符串；表结构侧同样是 VARCHAR（见 V100 迁移注释：
 * 不用 ENUM，是为了避免后续加状态时改列，违反「迁移只做加法」原则）。
 *
 * @author 智云集成治理
 */
public interface EventStatusConstants {

    // ========== event_outbox 状态 ==========
    /** 待投递：已随业务事务落库，尚未被中继领取 */
    String OUTBOX_NEW = "NEW";
    /** 投递中：已被中继领取但尚未确认；进程崩溃后仍可被再次领取 */
    String OUTBOX_PUBLISHING = "PUBLISHING";
    /** 已投递：至少一个消费者处理完成 */
    String OUTBOX_PUBLISHED = "PUBLISHED";
    /** 死信：重试次数达到阈值，停止自动重试，需人工重放 */
    String OUTBOX_DEAD = "DEAD";

    // ========== event_inbox 状态 ==========
    /** 已接收：已去重落库，等待业务处理器执行 */
    String INBOX_RECEIVED = "RECEIVED";
    /** 已处理：业务处理器执行成功 */
    String INBOX_PROCESSED = "PROCESSED";
    /** 处理失败：可随下一次投递重试 */
    String INBOX_FAILED = "FAILED";
    /** 死信：重试次数达到阈值，拒绝继续消费 */
    String INBOX_DEAD = "DEAD";

}