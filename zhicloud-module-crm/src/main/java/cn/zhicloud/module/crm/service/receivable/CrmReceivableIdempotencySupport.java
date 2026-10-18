package cn.zhicloud.module.crm.service.receivable;

import cn.hutool.core.util.StrUtil;
import cn.zhicloud.module.crm.dal.dataobject.receivable.CrmReceivableDO;
import cn.zhicloud.module.crm.dal.mysql.receivable.CrmReceivableMapper;
import org.springframework.dao.DeadlockLoserDataAccessException;

import java.util.Objects;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.*;

/**
 * CRM 回款「幂等支撑」：幂等键规范化、同键裁决与赢家等待（§12.247 / §12.250.5 内核）。
 *
 * <p>从 {@link CrmReceivableServiceImpl} 抽出（§12.267 D1）：D1 的审批事件登记使 Impl 涨到 619 行、
 * 越过 500 行结构红线（D1 前基线 553 行亦已超线），把幂等内核迁到包内静态协作者，Impl 只留编排。
 *
 * <p>载荷指纹（buildReceivablePayloadHash/hashValue/normalizeAmount）仍留在 Impl：既有并发单测
 * 通过反射调用它（§12.247），迁移会破坏测试契约且收益不抵；等待上界/步长同样留在 Impl，
 * 本类 awaitWinner 由调用方传入上界与步长。
 *
 * <p>刻意保持无状态静态（不参与 Spring 装配）：避免既有单测上下文连带改动；只做只读回查与
 * 纯计算，落单与业务写仍在 Impl 的业务事务内完成。
 */
final class CrmReceivableIdempotencySupport {

    private CrmReceivableIdempotencySupport() {
    }

    static String validateAndNormalizeIdempotencyKey(String idempotencyKey) {
        String normalized = StrUtil.trimToNull(idempotencyKey);
        if (normalized == null) {
            throw exception(RECEIVABLE_IDEMPOTENCY_KEY_REQUIRED);
        }
        if (normalized.length() > 128) {
            throw exception(RECEIVABLE_IDEMPOTENCY_KEY_INVALID);
        }
        return normalized;
    }

    /**
     * 等赢家那一行变得可见，上界内做不到就把始异常抛出去（§12.247）。
     *
     * <p>为什么必须自己吞掉回查期间的死锁：锁读同样走 {@code FOR UPDATE}，
     * 多输家排在同一行的 X 锁后面时可能被选为牺牲品。锁读被回滚不改变任何事实——
     * 赢家那一行照样在那里，退避后重试即可；真正需要如实抛出的是超过上界仍等不到，
     * 那说明不是并发竞争而是系统级故障。</p>
     *
     * <p>两条边界，勿当缺陷「修」（§12.250.5-1/-3 登记，§12.251 备案）：
     * ① catch 分支保持只读 —— InnoDB 死锁（1213）会把整个事务回滚，之后同一连接里的写
     *    不会回到原事务的原子单元（会随方法出口的 commit 单独落库），等于在失败路径上写库；
     * ② 只吞死锁，不吞锁等待超时 —— catch 精确匹配 {@code DeadlockLoserDataAccessException}
     *    （SQL 1213），1205（锁等待超时，{@code CannotAcquireLockException}）不被吞：
     *    持锁者停滞属系统级故障，应如实抛出，勿把 catch 扩到父类 PessimisticLockingFailure。</p>
     *
     * <p>形制与 {@code ErpFinancePaymentServiceImpl#awaitWinner} 一致：退避 ×2 封顶 500ms，
     * 抛最初那个异常（调用方关心的是最初为什么失败）。</p>
     *
     * @param idempotencyKey 幂等键（只按它回查，不按业务字段猜测）
     * @param fallback 上界内仍等不到时抛出的原始异常
     * @return 赢家那一行（载荷一致性由调用方裁决）
     */
    static CrmReceivableDO awaitWinner(CrmReceivableMapper receivableMapper,
                                       String idempotencyKey, RuntimeException fallback,
                                       long waitMaxMs, long waitStepMs) {
        long deadline = System.currentTimeMillis() + waitMaxMs;
        // 退避从一步开始；竞争越激烈（回查死锁越多），退避拉得越长
        long backoff = waitStepMs;
        RuntimeException lastDeadlock = null;
        while (true) {
            try {
                CrmReceivableDO existing = receivableMapper.selectByIdempotencyKeyForUpdate(idempotencyKey);
                if (existing != null) {
                    return existing;
                }
            } catch (DeadlockLoserDataAccessException de) {
                // 回查自己被选为牺牲品：与「赢家已提交」这一事实无关，重试即可
                lastDeadlock = de;
            }
            if (System.currentTimeMillis() >= deadline) {
                // 等不到就抛最原始的那个异常：调用方关心的是最初为什么失败
                throw lastDeadlock != null ? lastDeadlock : fallback;
            }
            try {
                Thread.sleep(backoff);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw fallback;
            }
            backoff = Math.min(backoff * 2, 500L);
        }
    }

    static Long resolveIdempotentReceivable(CrmReceivableDO existing, String payloadHash) {
        if (!Objects.equals(existing.getPayloadHash(), payloadHash)) {
            throw exception(RECEIVABLE_IDEMPOTENCY_KEY_CONFLICT);
        }
        return existing.getId();
    }
}
