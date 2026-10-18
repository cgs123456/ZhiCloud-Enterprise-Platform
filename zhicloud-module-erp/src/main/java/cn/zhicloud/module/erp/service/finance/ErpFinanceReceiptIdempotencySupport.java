package cn.zhicloud.module.erp.service.finance;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.zhicloud.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpFinanceReceiptMapper;
import cn.zhicloud.module.erp.service.finance.bo.ErpFinanceReceiptCrmCreateReqBO;
import org.springframework.dao.DeadlockLoserDataAccessException;

import java.math.BigDecimal;
import java.util.Objects;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 收款单「幂等支撑」：幂等键规范化、载荷指纹、同键裁决与赢家等待（§12.248 内核）。
 *
 * <p>从 {@link ErpFinanceReceiptServiceImpl} 抽出（§12.267 D1）：HTTP 建单与 CRM 联动建单
 * 必须共用同一份幂等语义，抽成包内静态协作者后两边调用同一实现，避免语义分叉；
 * 同时把 Impl 控制在 500 行结构红线内（抽出前 567 行）。
 *
 * <p>刻意保持无状态静态（不参与 Spring 装配）：既有单测上下文只 @Import 装配 Impl，
 * 增开 Bean 会连带改测试装配；依赖（Mapper）与等待参数（上界/步长）由调用方传入，
 * 等待字段仍留在 Impl 上，保持既有测试对 Impl 的反射调参能力。
 *
 * <p>本类只做只读回查与纯计算，不写业务表：落单、明细落库与源单回写仍在 Impl 的业务事务内完成。
 */
final class ErpFinanceReceiptIdempotencySupport {

    private ErpFinanceReceiptIdempotencySupport() {
    }

    /**
     * CRM 联动建单请求载荷的 SHA-256，字段集与 {@link #buildReceiptPayloadHash} 同口径：
     * 只含参与建单的字段；同一次审批的重试必须得到同一指纹，同键不同金额必须被拒绝。
     */
    static String buildCrmReceiptPayloadHash(ErpFinanceReceiptCrmCreateReqBO reqBO) {
        StringBuilder payload = new StringBuilder();
        payload.append(hashValue(reqBO.getCustomerId()))
                .append('|').append(hashValue(reqBO.getReceiptTime()))
                .append('|').append(hashValue(normalizeAmount(reqBO.getPrice())))
                .append('|').append(hashValue(reqBO.getReceivableId()))
                .append('|').append(hashValue(reqBO.getReceivableNo()));
        return DigestUtil.sha256Hex(payload.toString());
    }


    /**
     * 等赢家那一行变得可见，上界内做不到就把始异常抛出去。
     *
     * <p>必须吞掉回查期间的死锁并重试：16 线程同键并发时输家们排在同一行 X 锁后面，
     * InnoDB 判定成环后挑牺牲品回滚，抛出的 DeadlockLoserDataAccessException 若直接冒出去
     * 就是 500（实测 348 次）。而回查被回滚不改变「赢家那一行已存在」这个事实，
     * 退避后重试即可。真正该如实抛出的是超过上界仍等不到。</p>
     */
    static ErpFinanceReceiptDO awaitWinner(ErpFinanceReceiptMapper financeReceiptMapper,
                                           String idempotencyKey, RuntimeException fallback,
                                           long waitMaxMs, long waitStepMs) {
        long deadline = System.currentTimeMillis() + waitMaxMs;
        long backoff = waitStepMs;
        RuntimeException lastDeadlock = null;
        while (true) {
            try {
                ErpFinanceReceiptDO existing = financeReceiptMapper.selectByIdempotencyKeyForUpdate(idempotencyKey);
                if (existing != null) {
                    return existing;
                }
            } catch (DeadlockLoserDataAccessException de) {
                lastDeadlock = de;
            }
            if (System.currentTimeMillis() >= deadline) {
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

    /**
     * 校验并规范化客户端幂等键：必填、长度上限 128（与列 varchar(128) 一致）。
     */
    static String validateAndNormalizeIdempotencyKey(String idempotencyKey) {
        String normalized = StrUtil.trimToNull(idempotencyKey);
        if (normalized == null) {
            throw exception(FINANCE_RECEIPT_IDEMPOTENCY_KEY_REQUIRED);
        }
        if (normalized.length() > 128) {
            throw exception(FINANCE_RECEIPT_IDEMPOTENCY_KEY_REQUIRED);
        }
        return normalized;
    }

    /**
     * 命中已有收款单时的裁决：载荷一致才返回原单，不一致必须拒绝。
     *
     * <p>不能无条件返回已有单据：客户端若复用了幂等键但换了金额/客户，
     * 静默返回原单会让调用方以为自己提交的那笔已经生效。</p>
     */
    static Long resolveIdempotentReceipt(ErpFinanceReceiptDO existing, String payloadHash) {
        if (!Objects.equals(existing.getPayloadHash(), payloadHash)) {
            throw exception(FINANCE_RECEIPT_IDEMPOTENCY_KEY_CONFLICT);
        }
        return existing.getId();
    }

    /**
     * 创建请求载荷的 SHA-256。
     *
     * <p>金额经 {@link #normalizeAmount} 去掉尾部零：BigDecimal 的 100 与 100.00
     * 表示同一金额，不归一化会把同一次提交误判成不同载荷。明细行按传入顺序参与哈希。</p>
     */
    static String buildReceiptPayloadHash(ErpFinanceReceiptSaveReqVO reqVO) {
        StringBuilder payload = new StringBuilder();
        payload.append(hashValue(reqVO.getCustomerId()))
                .append('|').append(hashValue(reqVO.getAccountId()))
                .append('|').append(hashValue(reqVO.getFinanceUserId()))
                .append('|').append(hashValue(reqVO.getReceiptTime()))
                .append('|').append(hashValue(normalizeAmount(reqVO.getDiscountPrice())))
                .append('|').append(hashValue(reqVO.getRemark()));
        if (CollUtil.isNotEmpty(reqVO.getItems())) {
            // 明细顺序敏感：交换两行的提交被视为不同载荷
            reqVO.getItems().forEach(item -> payload.append('|')
                    .append(hashValue(item.getBizType()))
                    .append('|').append(hashValue(item.getBizId()))
                    .append('|').append(hashValue(normalizeAmount(item.getReceiptPrice()))));
        }
        return DigestUtil.sha256Hex(payload.toString());
    }

    private static String hashValue(Object value) {
        return value == null ? "<null>" : value.toString();
    }

    private static String normalizeAmount(BigDecimal amount) {
        return amount == null ? null : amount.stripTrailingZeros().toPlainString();
    }
}
