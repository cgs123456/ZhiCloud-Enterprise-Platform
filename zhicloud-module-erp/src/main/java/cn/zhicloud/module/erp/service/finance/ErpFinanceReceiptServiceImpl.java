package cn.zhicloud.module.erp.service.finance;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.collection.CollectionUtils;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpFinanceReceiptItemDO;
import cn.zhicloud.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.zhicloud.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpFinanceReceiptItemMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpFinanceReceiptMapper;
import cn.zhicloud.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.zhicloud.module.erp.enums.ErpAuditStatus;
import cn.zhicloud.module.erp.enums.common.ErpBizTypeEnum;
import cn.zhicloud.module.erp.service.accounting.ErpAccountingEventService;
import cn.zhicloud.module.erp.service.accounting.bo.ErpAccountingEventCreateReqBO;
import cn.zhicloud.module.erp.service.accounting.bo.ErpAccountingEventReverseReqBO;
import cn.zhicloud.module.erp.service.sale.ErpCustomerService;
import cn.zhicloud.module.erp.service.sale.ErpSaleOutService;
import cn.zhicloud.module.erp.service.sale.ErpSaleReturnService;
import cn.zhicloud.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.framework.common.util.collection.CollectionUtils.*;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.*;

// 操作日志已通过 @OperateLog 注解自动记录（bizlog-sdk 通过 AOP 自动记录）

/**
 * ERP 收款单 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class ErpFinanceReceiptServiceImpl implements ErpFinanceReceiptService {

    private static final Logger log = LoggerFactory.getLogger(ErpFinanceReceiptServiceImpl.class);
    /**
     * 唯一键冲突后等赢家提交的上界（毫秒）。仅用于 catch (DuplicateKeyException｜DeadlockLoser) 的自旋重试。
     *
     * <p>非 static final：E2E/单测需要把步长压到 0 来瞬跑 21 轮轮询。
     * 生产值经 16 线程 × 20 轮实测：赢家在 100ms 内必然提交，从未触到上界。</p>
     */
    private long idempotentWaitMaxMs = 2000L;

    /**
     * 自旋步长（毫秒）。20 次退避足够覆盖一次本地事务提交。
     */
    private long idempotentWaitStepMs = 100L;

    @Resource
    private ErpFinanceReceiptMapper financeReceiptMapper;
    @Resource
    private ErpFinanceReceiptItemMapper financeReceiptItemMapper;

    @Resource
    private ErpNoRedisDAO noRedisDAO;

    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private ErpSaleOutService saleOutService;
    @Resource
    private ErpSaleReturnService saleReturnService;
    @Resource
    private ErpAccountingEventService accountingEventService;

    @Resource
    private AdminUserApi adminUserApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFinanceReceipt(ErpFinanceReceiptSaveReqVO createReqVO) {
        // 1.1 幂等键与载荷指纹必须先于任何业务副作用计算和校验（同 CRM 回款范式）。
        // 收款单的业务副作用（updateSalePrice、过账事件）不可重入，
        // 若放在校验之后，重试请求会先把副作用做一遍才命中幂等短路。
        String idempotencyKey = validateAndNormalizeIdempotencyKey(createReqVO.getIdempotencyKey());
        String payloadHash = buildReceiptPayloadHash(createReqVO);
        // 1.1 幂等预检：普通读。刻意不加锁——RR 下唯一索引上「查不到行」的 FOR UPDATE
        // 只加间隙锁，而同 gap 的间隙锁互相兼容（§12.233.4 实测：A 持锁期间 B 0.27s 即返回），
        // 占位锁根本挡不住并发。这里只是快速失败：真正拦住重复提交的是后面的唯一键。
        ErpFinanceReceiptDO existing = financeReceiptMapper.selectByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            return resolveIdempotentReceipt(existing, payloadHash);
        }

        // 1.2 校验订单项的有效性
        List<ErpFinanceReceiptItemDO> receiptItems = validateFinanceReceiptItems(
                createReqVO.getCustomerId(), createReqVO.getItems());
        // 1.3 校验客户
        customerService.validateCustomer(createReqVO.getCustomerId());
        // 1.4 校验结算账户
        if (createReqVO.getAccountId() != null) {
            accountService.validateAccount(createReqVO.getAccountId());
        }
        // 1.5 校验财务人员
        if (createReqVO.getFinanceUserId() != null) {
            adminUserApi.validateUser(createReqVO.getFinanceUserId());
        }
        // 1.6 冗余的二次确认：真正拦住重复提交的是 insert 处的唯一键 + catch 分支里的
        // 锁读回查，1.1/1.6 都只是快速失败路径，这里理论上必然查不到。
        // 保留它不为功能，只为防御：万一将来有人调整唯一键或插入顺序，这道兜底仍在。
        // 成本是一次索引点查，相对整个建单事务可忽略。
        existing = financeReceiptMapper.selectByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            return resolveIdempotentReceipt(existing, payloadHash);
        }
        // 1.7 生成收款单号，并校验唯一性
        String no = noRedisDAO.generate(ErpNoRedisDAO.FINANCE_RECEIPT_NO_PREFIX);
        if (financeReceiptMapper.selectByNo(no) != null) {
            throw exception(FINANCE_RECEIPT_NO_EXISTS);
        }

        // 2.1 插入收款单
        ErpFinanceReceiptDO receipt = BeanUtils.toBean(createReqVO, ErpFinanceReceiptDO.class, in -> in
                .setNo(no).setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setIdempotencyKey(idempotencyKey).setPayloadHash(payloadHash));
        calculateTotalPrice(receipt, receiptItems);
        try {
            financeReceiptMapper.insert(receipt);
        } catch (DuplicateKeyException | DeadlockLoserDataAccessException e) {
            // 同键并发的两种失败形态，收敛到同一条幂等回查路径（同付款单，详见其方法注释）：
            //   - DuplicateKeyException：我插入时那一行已提交
            //   - DeadlockLoserDataAccessException：多插入争同一唯一键，insert intention 锁成环
            //
            // 回查必须锁读而不能普通读 + 自旋：RR 下本事务的一致性读视图在第一次普通读
            // （1.1）时就固定了，早于赢家提交，自旋再久也读不到那一行。
            // 锁读本身也会死锁，退避重试的逻辑见 awaitWinner。
            ErpFinanceReceiptDO existing2 = awaitWinner(idempotencyKey, e);
            log.info("[createFinanceReceipt][幂等={} 并发重复提交，返回已有 id={}]",
                    idempotencyKey, existing2.getId());
            return resolveIdempotentReceipt(existing2, payloadHash);
        }
        // 2.2 插入收款单项
        receiptItems.forEach(o -> o.setReceiptId(receipt.getId()));
        financeReceiptItemMapper.insertBatch(receiptItems);

        // 3. 更新销售出库、退货的收款金额情况
        updateSalePrice(receiptItems);
        return receipt.getId();
    }


    /**
     * 等赢家那一行变得可见，上界内做不到就把始异常抛出去。
     *
     * <p>必须吞掉回查期间的死锁并重试：16 线程同键并发时输家们排在同一行 X 锁后面，
     * InnoDB 判定成环后挑牺牲品回滚，抛出的 DeadlockLoserDataAccessException 若直接冒出去
     * 就是 500（实测 348 次）。而回查被回滚不改变「赢家那一行已存在」这个事实，
     * 退避后重试即可。真正该如实抛出的是超过上界仍等不到。</p>
     */
    private ErpFinanceReceiptDO awaitWinner(String idempotencyKey, RuntimeException fallback) {
        long deadline = System.currentTimeMillis() + idempotentWaitMaxMs;
        long backoff = idempotentWaitStepMs;
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
    private String validateAndNormalizeIdempotencyKey(String idempotencyKey) {
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
    private Long resolveIdempotentReceipt(ErpFinanceReceiptDO existing, String payloadHash) {
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
    private String buildReceiptPayloadHash(ErpFinanceReceiptSaveReqVO reqVO) {
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

    private String hashValue(Object value) {
        return value == null ? "<null>" : value.toString();
    }

    private String normalizeAmount(BigDecimal amount) {
        return amount == null ? null : amount.stripTrailingZeros().toPlainString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFinanceReceipt(ErpFinanceReceiptSaveReqVO updateReqVO) {
        // 1.1 校验存在
        ErpFinanceReceiptDO receipt = validateFinanceReceiptExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(receipt.getStatus())) {
            throw exception(FINANCE_RECEIPT_UPDATE_FAIL_APPROVE, receipt.getNo());
        }
        // 1.2 校验客户
        customerService.validateCustomer(updateReqVO.getCustomerId());
        // 1.3 校验结算账户
        if (updateReqVO.getAccountId() != null) {
            accountService.validateAccount(updateReqVO.getAccountId());
        }
        // 1.4 校验财务人员
        if (updateReqVO.getFinanceUserId() != null) {
            adminUserApi.validateUser(updateReqVO.getFinanceUserId());
        }
        // 1.5 校验收款单项的有效性
        List<ErpFinanceReceiptItemDO> receiptItems = validateFinanceReceiptItems(
                updateReqVO.getCustomerId(), updateReqVO.getItems());

        // 2.1 更新收款单
        ErpFinanceReceiptDO updateObj = BeanUtils.toBean(updateReqVO, ErpFinanceReceiptDO.class);
        calculateTotalPrice(updateObj, receiptItems);
        financeReceiptMapper.updateById(updateObj);
        // 2.2 更新收款单项
        updateFinanceReceiptItemList(updateReqVO.getId(), receiptItems);
    }

    private void calculateTotalPrice(ErpFinanceReceiptDO receipt, List<ErpFinanceReceiptItemDO> receiptItems) {
        receipt.setTotalPrice(getSumValue(receiptItems, ErpFinanceReceiptItemDO::getReceiptPrice, BigDecimal::add, BigDecimal.ZERO));
        // discountPrice 在 VO 上仅由 @NotNull 约束，DB 列是 DEFAULT NULL（V82:344），
        // 非 HTTP 入口（内部调用、单测、集成）不受该注解保护，必须按 0 兜底，
        // 否则 subtract(null) 抛 NPE 变成 500。
        receipt.setReceiptPrice(receipt.getTotalPrice().subtract(defaultZero(receipt.getDiscountPrice())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFinanceReceiptStatus(Long id, Integer status) {
        if (!ErpAuditStatus.isValid(status)) {
            throw exception(ERP_AUDIT_STATUS_INVALID, status);
        }
        boolean approve = ErpAuditStatus.APPROVE.getStatus().equals(status);
        // 1.1 校验存在
        ErpFinanceReceiptDO receipt = validateFinanceReceiptExists(id);
        // 1.2 幂等处理：目标状态与当前状态一致时，返回同一业务结果
        if (status.equals(receipt.getStatus())) {
            return;
        }

        // 2. 更新状态
        int updateCount = financeReceiptMapper.updateByIdAndStatus(id, receipt.getStatus(),
                new ErpFinanceReceiptDO().setStatus(status));
        if (updateCount == 0) {
            ErpFinanceReceiptDO latest = financeReceiptMapper.selectOneForUpdate(ErpFinanceReceiptDO::getId, id);
            if (latest != null && status.equals(latest.getStatus())) {
                return;
            }
            throw exception(approve ? FINANCE_RECEIPT_APPROVE_FAIL : FINANCE_RECEIPT_PROCESS_FAIL);
        }

        // 3. 审核通过时生成收款会计事件；事件与收款状态变更处于同一事务。
        // 反审核只对已审核单据生成冲销事件，避免从未过账的单据在驳回时被误冲销。
        if (approve) {
            accountingEventService.postEvent(buildReceiptPostedEvent(receipt));
        } else if (ErpAuditStatus.APPROVE.getStatus().equals(receipt.getStatus())) {
            accountingEventService.reverseEvent(buildReceiptReversalRequest(receipt));
        }
    }

    private ErpAccountingEventCreateReqBO buildReceiptPostedEvent(ErpFinanceReceiptDO receipt) {
        LocalDate eventDate = receipt.getReceiptTime() == null
                ? LocalDate.now() : receipt.getReceiptTime().toLocalDate();
        BigDecimal receiptPrice = defaultZero(receipt.getReceiptPrice());
        return ErpAccountingEventCreateReqBO.builder()
                .eventType("RECEIPT_POSTED")
                .sourceDocType("FINANCE_RECEIPT")
                .sourceDocId(receipt.getId())
                .sourceDocNo(receipt.getNo())
                .sourceDocLineId(0L)
                .sourceRevision(1)
                .eventDate(eventDate)
                .accountingDate(eventDate)
                .amountWithoutTax(receiptPrice)
                .taxAmount(BigDecimal.ZERO)
                .amountWithTax(receiptPrice)
                .docTotalAmount(receiptPrice)
                .costAmount(BigDecimal.ZERO)
                .currency(null)
                .exchangeRate(BigDecimal.ONE)
                .reversalOfEventId(0L)
                .occurredAt(receipt.getReceiptTime() == null ? LocalDateTime.now() : receipt.getReceiptTime())
                .build();
    }

    private ErpAccountingEventReverseReqBO buildReceiptReversalRequest(ErpFinanceReceiptDO receipt) {
        LocalDate eventDate = receipt.getReceiptTime() == null
                ? LocalDate.now() : receipt.getReceiptTime().toLocalDate();
        return ErpAccountingEventReverseReqBO.builder()
                .eventType("RECEIPT_POSTED")
                .sourceDocType("FINANCE_RECEIPT")
                .sourceDocId(receipt.getId())
                .sourceDocLineId(0L)
                .sourceRevision(1)
                .eventDate(eventDate)
                // 冲销记在当前会计期，不回写原会计期
                .accountingDate(LocalDate.now())
                .occurredAt(LocalDateTime.now())
                .build();
    }

    private static BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private List<ErpFinanceReceiptItemDO> validateFinanceReceiptItems(
            Long customerId,
            List<ErpFinanceReceiptSaveReqVO.Item> list) {
        return convertList(list, o -> BeanUtils.toBean(o, ErpFinanceReceiptItemDO.class, item -> {
            if (ObjectUtil.equal(item.getBizType(), ErpBizTypeEnum.SALE_OUT.getType())) {
                ErpSaleOutDO saleOut = saleOutService.validateSaleOut(item.getBizId());
                Assert.equals(saleOut.getCustomerId(), customerId, "客户必须相同");
                item.setTotalPrice(saleOut.getTotalPrice()).setBizNo(saleOut.getNo());
            } else if (ObjectUtil.equal(item.getBizType(), ErpBizTypeEnum.SALE_RETURN.getType())) {
                ErpSaleReturnDO saleReturn = saleReturnService.validateSaleReturn(item.getBizId());
                Assert.equals(saleReturn.getCustomerId(), customerId, "客户必须相同");
                item.setTotalPrice(saleReturn.getTotalPrice().negate()).setBizNo(saleReturn.getNo());
            } else {
                throw new IllegalArgumentException("业务类型不正确：" + item.getBizType());
            }
        }));
    }

    private void updateFinanceReceiptItemList(Long id, List<ErpFinanceReceiptItemDO> newList) {
        // 第一步，对比新老数据，获得添加、修改、删除的列表
        List<ErpFinanceReceiptItemDO> oldList = financeReceiptItemMapper.selectListByReceiptId(id);
        List<List<ErpFinanceReceiptItemDO>> diffList = diffList(oldList, newList, // id 不同，就认为是不同的记录
                (oldVal, newVal) -> oldVal.getId().equals(newVal.getId()));

        // 第二步，批量添加、修改、删除
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            diffList.get(0).forEach(o -> o.setReceiptId(id));
            financeReceiptItemMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            financeReceiptItemMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            financeReceiptItemMapper.deleteByIds(convertList(diffList.get(2), ErpFinanceReceiptItemDO::getId));
        }

        // 第三步，更新销售出库、退货的收款金额情况
        updateSalePrice(CollectionUtils.newArrayList(diffList));
    }

    private void updateSalePrice(List<ErpFinanceReceiptItemDO> receiptItems) {
        receiptItems.forEach(receiptItem -> {
            BigDecimal totalReceiptPrice = financeReceiptItemMapper.selectReceiptPriceSumByBizIdAndBizType(
                    receiptItem.getBizId(), receiptItem.getBizType());
            if (ErpBizTypeEnum.SALE_OUT.getType().equals(receiptItem.getBizType())) {
                saleOutService.updateSaleInReceiptPrice(receiptItem.getBizId(), totalReceiptPrice);
            } else if (ErpBizTypeEnum.SALE_RETURN.getType().equals(receiptItem.getBizType())) {
                saleReturnService.updateSaleReturnRefundPrice(receiptItem.getBizId(), totalReceiptPrice.negate());
            } else {
                throw new IllegalArgumentException("业务类型不正确：" + receiptItem.getBizType());
            }
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFinanceReceipt(List<Long> ids) {
        // 1. 校验不处于已审批
        List<ErpFinanceReceiptDO> receipts = financeReceiptMapper.selectByIds(ids);
        if (CollUtil.isEmpty(receipts)) {
            return;
        }
        receipts.forEach(receipt -> {
            if (ErpAuditStatus.APPROVE.getStatus().equals(receipt.getStatus())) {
                throw exception(FINANCE_RECEIPT_DELETE_FAIL_APPROVE, receipt.getNo());
            }
        });

        // 2. 遍历删除，并记录操作日志
        receipts.forEach(receipt -> {
            // 2.1 删除收款单
            financeReceiptMapper.deleteById(receipt.getId());
            // 2.2 删除收款单项
            List<ErpFinanceReceiptItemDO> receiptItems = financeReceiptItemMapper.selectListByReceiptId(receipt.getId());
            financeReceiptItemMapper.deleteByIds(convertSet(receiptItems, ErpFinanceReceiptItemDO::getId));

            // 2.3 更新销售出库、退货的收款金额情况
            updateSalePrice(receiptItems);
        });
    }

    private ErpFinanceReceiptDO validateFinanceReceiptExists(Long id) {
        ErpFinanceReceiptDO receipt = financeReceiptMapper.selectById(id);
        if (receipt == null) {
            throw exception(FINANCE_RECEIPT_NOT_EXISTS);
        }
        return receipt;
    }

    @Override
    public ErpFinanceReceiptDO getFinanceReceipt(Long id) {
        return financeReceiptMapper.selectById(id);
    }

    @Override
    public PageResult<ErpFinanceReceiptDO> getFinanceReceiptPage(ErpFinanceReceiptPageReqVO pageReqVO) {
        return financeReceiptMapper.selectPage(pageReqVO);
    }

    // ==================== 收款单项 ====================

    @Override
    public List<ErpFinanceReceiptItemDO> getFinanceReceiptItemListByReceiptId(Long receiptId) {
        return financeReceiptItemMapper.selectListByReceiptId(receiptId);
    }

    @Override
    public List<ErpFinanceReceiptItemDO> getFinanceReceiptItemListByReceiptIds(Collection<Long> receiptIds) {
        if (CollUtil.isEmpty(receiptIds)) {
            return Collections.emptyList();
        }
        return financeReceiptItemMapper.selectListByReceiptIds(receiptIds);
    }

}
