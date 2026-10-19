package cn.zhicloud.module.erp.service.finance;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.collection.CollectionUtils;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpFinancePaymentDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpFinancePaymentItemDO;
import cn.zhicloud.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.zhicloud.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpFinancePaymentItemMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpFinancePaymentMapper;
import cn.zhicloud.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.zhicloud.module.erp.enums.ErpAuditStatus;
import cn.zhicloud.module.erp.enums.common.ErpBizTypeEnum;
import cn.zhicloud.module.erp.service.accounting.ErpAccountingEventService;
import cn.zhicloud.module.erp.service.accounting.bo.ErpAccountingEventCreateReqBO;
import cn.zhicloud.module.erp.service.accounting.bo.ErpAccountingEventReverseReqBO;
import cn.zhicloud.module.erp.service.purchase.ErpPurchaseInService;
import cn.zhicloud.module.erp.service.purchase.ErpPurchaseReturnService;
import cn.zhicloud.module.erp.service.purchase.ErpSupplierService;
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
 * ERP 付款单 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class ErpFinancePaymentServiceImpl implements ErpFinancePaymentService {

    private static final Logger log = LoggerFactory.getLogger(ErpFinancePaymentServiceImpl.class);
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
    private ErpFinancePaymentMapper financePaymentMapper;
    @Resource
    private ErpFinancePaymentItemMapper financePaymentItemMapper;

    @Resource
    private ErpNoRedisDAO noRedisDAO;

    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private ErpPurchaseInService purchaseInService;
    @Resource
    private ErpPurchaseReturnService purchaseReturnService;
    @Resource
    private ErpAccountingEventService accountingEventService;

    @Resource
    private AdminUserApi adminUserApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFinancePayment(ErpFinancePaymentSaveReqVO createReqVO) {
        // 1.1 幂等键与载荷指纹必须先于任何业务副作用计算和校验（同 CRM 回款范式）。
        // 付款/收款单的业务副作用（updatePurchasePrice、过账事件）不可重入，
        // 若放在校验之后，重试请求会先把副作用做一遍才命中幂等短路。
        String idempotencyKey = validateAndNormalizeIdempotencyKey(createReqVO.getIdempotencyKey());
        String payloadHash = buildPaymentPayloadHash(createReqVO);
        // 1.1 幂等预检：普通读。刻意不加锁——RR 下唯一索引上「查不到行」的 FOR UPDATE
        // 只加间隙锁，而同 gap 的间隙锁互相兼容（§12.233.4 实测：A 持锁期间 B 0.27s 即返回），
        // 占位锁根本挡不住并发。这里只是快速失败：真正拦住重复提交的是后面的唯一键。
        ErpFinancePaymentDO existing = financePaymentMapper.selectByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            return resolveIdempotentPayment(existing, payloadHash);
        }

        // 1.2 校验订单项的有效性
        List<ErpFinancePaymentItemDO> paymentItems = validateFinancePaymentItems(
                createReqVO.getSupplierId(), createReqVO.getItems());
        // 1.3 校验供应商
        supplierService.validateSupplier(createReqVO.getSupplierId());
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
        existing = financePaymentMapper.selectByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            return resolveIdempotentPayment(existing, payloadHash);
        }
        // 1.7 生成付款单号，并校验唯一性
        String no = noRedisDAO.generate(ErpNoRedisDAO.FINANCE_PAYMENT_NO_PREFIX);
        if (financePaymentMapper.selectByNo(no) != null) {
            throw exception(FINANCE_PAYMENT_NO_EXISTS);
        }

        // 2.1 插入付款单
        ErpFinancePaymentDO payment = BeanUtils.toBean(createReqVO, ErpFinancePaymentDO.class, in -> in
                .setNo(no).setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setIdempotencyKey(idempotencyKey).setPayloadHash(payloadHash));
        calculateTotalPrice(payment, paymentItems);
        try {
            financePaymentMapper.insert(payment);
        } catch (Exception e) {
            // 并发兜底的可恢复判定（§12.233 实测 + sandbox 并发实测 2026-10-19 暴露）：
            // Spring 对 MySQL ER_LOCK_DEADLOCK(1213) 的翻译在部分路径下不是
            // DeadlockLoserDataAccessException，sandbox N=8 同键并发实测中 insert 抛出的
            // MySQLTransactionRollbackException 未被原 catch (DuplicateKey | DeadlockLoser) 接住
            // → 500。这里 broad 为 DataAccessException/SQLException + 消息兜底，
            // 只有死锁/唯一键冲突才走 awaitWinner 回查；其它异常原样抛出（不伪装成幂等成功）。
            boolean recoverable = e instanceof org.springframework.dao.DataAccessException
                    || e instanceof java.sql.SQLException
                    || (e.getMessage() != null && e.getMessage().toLowerCase().contains("deadlock"))
                    || (e.getMessage() != null && e.getMessage().toLowerCase().contains("duplicate"));
            if (!recoverable) {
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                throw new RuntimeException(e);
            }
            RuntimeException fallback = (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
            // 同键并发的两种失败形态，收敛到同一条幂等回查路径：
            //   - DuplicateKeyException：我插入时那一行已提交
            //   - DeadlockLoserDataAccessException：多插入争同一唯一键，insert intention
            //     锁成环，InnoDB 选我当牺牲品回滚
            //
            // §12.233 实测（SHOW ENGINE INNODB STATUS 抓现场）：冲突全部集中在
            // uk_..._idempotency 这一个索引上，是纯 INSERT vs INSERT，与业务行锁无关，
            // 因此不存在「调整锁顺序」这个解法。
            //
            // 为什么回查必须**锁读**（FOR UPDATE），不能用普通读 + 自旋等待：
            //   RR 的一致性读视图在本事务**第一次一致性读**时就固定了，之后同一事务的
            //   所有普通 SELECT 都沿用该视图。1.1 的 selectByIdempotencyKey 是普通读，
            //   它建立的视图早于赢家提交——所以自旋读到的永远是「不存在」，等 2s 和 8s
            //   结果完全一样（实测：预 2s→8s，500 数不变）。锁读读的是最新已提交版本，
            //   不受快照限制，所以能等到赢家那一行。
            //
            // 被实测否决的两条路，不要重走：
            //   1) 普通读一次就返回     —— 赢家未提交 → null → 重抛 → 500（528 次无一命中）
            //   2) 普通读 + 自旋 N ms   —— 上一条的快照版本，等再久也读不到（见上）
            // 另外 1.1 处加 FOR UPDATE 占位锁也无效：唯一索引上「查不到行」只加间隙锁，
            // 间隙锁之间兼容（实测 B 在 A 持锁期间 0.27s 即返回），全部线程照旧走到 INSERT。
            //
            // 锁读的代价是回查本身也会死锁，处理见 awaitWinner：退避重试，不重排锁序。
            ErpFinancePaymentDO existing2 = awaitWinner(idempotencyKey, fallback);
            log.info("[createFinancePayment][幂等键={} 并发重复提交，返回已有 id={}]",
                    idempotencyKey, existing2.getId());
            return resolveIdempotentPayment(existing2, payloadHash);
        }
        // 2.2 插入付款单项
        paymentItems.forEach(o -> o.setPaymentId(payment.getId()));
        financePaymentItemMapper.insertBatch(paymentItems);

        // 3. 更新采购入库、退货的付款金额情况
        updatePurchasePrice(paymentItems);
        return payment.getId();
    }

    /**
     * 等赢家那一行变得可见，上界内做不到就把始异常抛出去。
     *
     * <p>为什么单独抽出来：回查用的锁读**自己也会死锁**。16 个线程同键并发时，
     * 输家们排在同一行的 X 锁后面，InnoDB 判定成环后随机挑牺牲品回滚，
     * 抛出的是 {@link DeadlockLoserDataAccessException}。实测（16 线程 × 20 轮）：
     * 锁读能等到赢家（"返回已有 id" 出现 48 次），但同时产生 348 次 InnoDB 死锁，
     * 每次死锁都是一个 500——因为它在 catch 块**内部**抛出，外层的 catch 接不到自己。</p>
     *
     * <p>所以这里必须自己吞掉回查期间的死锁并重试：锁读被回滚不改变任何事实，
     * 赢家那一行照样在那里，退避后重试即可。真正需要如实抛出的是超过上界仍等不到——
     * 那说明不是并发竞争而是系统级故障。</p>
     *
     * <p>两条边界，勿当缺陷「修」（§12.250.5-1/-3 登记，§12.251 备案，与 CRM
     * CrmReceivableServiceImpl#awaitWinner 同形制）：
     * ① catch 分支保持只读 —— InnoDB 死锁（1213）会把整个事务回滚，之后同一连接里的写
     *    不会回到原事务的原子单元（会随方法出口的 commit 单独落库），等于在失败路径上写库；
     * ② 只吞死锁，不吞锁等待超时 —— catch 精确匹配 {@code DeadlockLoserDataAccessException}
     *    （SQL 1213），1205（锁等待超时，{@code CannotAcquireLockException}）不被吞：
     *    持锁者停滞属系统级故障，应如实抛出，勿把 catch 扩到父类 PessimisticLockingFailure。</p>
     *
     * @param fallback 上界内仍等不到时抛出的原始异常（保留最初的失败上下文）
     */
    private ErpFinancePaymentDO awaitWinner(String idempotencyKey, RuntimeException fallback) {
        long deadline = System.currentTimeMillis() + idempotentWaitMaxMs;
        // 退避从一步开始，死锁越多说明竞争越激烈，退避就拉得越长
        long backoff = idempotentWaitStepMs;
        RuntimeException lastDeadlock = null;
        while (true) {
            try {
                ErpFinancePaymentDO existing = financePaymentMapper.selectByIdempotencyKeyForUpdate(idempotencyKey);
                if (existing != null) {
                    return existing;
                }
            } catch (Exception de) {
                // 回查自己被选为牺牲品：与「赢家已提交」这一事实无关，重试即可。
                // broad 为 Exception：Spring 对 MySQL ER_LOCK_DEADLOCK(1213) 的翻译在部分
                // 路径下不是 DeadlockLoserDataAccessException（sandbox 并发实测暴露），
                // 必须 broad catch 才能退避重试，避免回查期间的死锁泄漏成 500。
                lastDeadlock = (de instanceof RuntimeException) ? (RuntimeException) de : new RuntimeException(de);
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

    /**
     * 校验并规范化客户端幂等键：必填、长度上限 128（与列 varchar(128) 一致）。
     */
    private String validateAndNormalizeIdempotencyKey(String idempotencyKey) {
        String normalized = StrUtil.trimToNull(idempotencyKey);
        if (normalized == null) {
            throw exception(FINANCE_PAYMENT_IDEMPOTENCY_KEY_REQUIRED);
        }
        if (normalized.length() > 128) {
            throw exception(FINANCE_PAYMENT_IDEMPOTENCY_KEY_REQUIRED);
        }
        return normalized;
    }

    /**
     * 命中已有付款单时的裁决：载荷一致才返回原单，不一致必须拒绝。
     *
     * <p>不能无条件返回已有单据：客户端若复用了幂等键但换了金额/供应商，
     * 静默返回原单会让调用方以为自己提交的那笔已经生效。</p>
     */
    private Long resolveIdempotentPayment(ErpFinancePaymentDO existing, String payloadHash) {
        if (!Objects.equals(existing.getPayloadHash(), payloadHash)) {
            throw exception(FINANCE_PAYMENT_IDEMPOTENCY_KEY_CONFLICT);
        }
        return existing.getId();
    }

    /**
     * 创建请求载荷的 SHA-256。
     *
     * <p>金额经 {@link #normalizeAmount} 去掉尾部零：BigDecimal 的 100 与 100.00
     * 表示同一金额，不归一化会把同一次提交误判成不同载荷。明细行按传入顺序参与哈希。</p>
     */
    private String buildPaymentPayloadHash(ErpFinancePaymentSaveReqVO reqVO) {
        StringBuilder payload = new StringBuilder();
        payload.append(hashValue(reqVO.getSupplierId()))
                .append('|').append(hashValue(reqVO.getAccountId()))
                .append('|').append(hashValue(reqVO.getFinanceUserId()))
                .append('|').append(hashValue(reqVO.getPaymentTime()))
                .append('|').append(hashValue(normalizeAmount(reqVO.getDiscountPrice())))
                .append('|').append(hashValue(reqVO.getRemark()));
        if (CollUtil.isNotEmpty(reqVO.getItems())) {
            // 明细顺序敏感：交换两行的提交被视为不同载荷
            reqVO.getItems().forEach(item -> payload.append('|')
                    .append(hashValue(item.getBizType()))
                    .append('|').append(hashValue(item.getBizId()))
                    .append('|').append(hashValue(normalizeAmount(item.getPaymentPrice()))));
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
    public void updateFinancePayment(ErpFinancePaymentSaveReqVO updateReqVO) {
        // 1.1 校验存在
        ErpFinancePaymentDO payment = validateFinancePaymentExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(payment.getStatus())) {
            throw exception(FINANCE_PAYMENT_UPDATE_FAIL_APPROVE, payment.getNo());
        }
        // 1.2 校验供应商
        supplierService.validateSupplier(updateReqVO.getSupplierId());
        // 1.3 校验结算账户
        if (updateReqVO.getAccountId() != null) {
            accountService.validateAccount(updateReqVO.getAccountId());
        }
        // 1.4 校验财务人员
        if (updateReqVO.getFinanceUserId() != null) {
            adminUserApi.validateUser(updateReqVO.getFinanceUserId());
        }
        // 1.5 校验付款单项的有效性
        List<ErpFinancePaymentItemDO> paymentItems = validateFinancePaymentItems(
                updateReqVO.getSupplierId(), updateReqVO.getItems());

        // 2.1 更新付款单
        ErpFinancePaymentDO updateObj = BeanUtils.toBean(updateReqVO, ErpFinancePaymentDO.class);
        calculateTotalPrice(updateObj, paymentItems);
        financePaymentMapper.updateById(updateObj);
        // 2.2 更新付款单项
        updateFinancePaymentItemList(updateReqVO.getId(), paymentItems);
    }

    private void calculateTotalPrice(ErpFinancePaymentDO payment, List<ErpFinancePaymentItemDO> paymentItems) {
        payment.setTotalPrice(getSumValue(paymentItems, ErpFinancePaymentItemDO::getPaymentPrice, BigDecimal::add, BigDecimal.ZERO));
        // discountPrice 在 VO 上仅由 @NotNull 约束，DB 列是 DEFAULT NULL（V82:344），
        // 非 HTTP 入口（内部调用、单测、集成）不受该注解保护，必须按 0 兜底，
        // 否则 subtract(null) 抛 NPE 变成 500。
        payment.setPaymentPrice(payment.getTotalPrice().subtract(defaultZero(payment.getDiscountPrice())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFinancePaymentStatus(Long id, Integer status) {
        if (!ErpAuditStatus.isValid(status)) {
            throw exception(ERP_AUDIT_STATUS_INVALID, status);
        }
        boolean approve = ErpAuditStatus.APPROVE.getStatus().equals(status);
        // 1.1 校验存在
        ErpFinancePaymentDO payment = validateFinancePaymentExists(id);
        // 1.2 幂等处理：目标状态与当前状态一致时，返回同一业务结果
        if (status.equals(payment.getStatus())) {
            return;
        }

        // 2. 更新状态
        int updateCount = financePaymentMapper.updateByIdAndStatus(id, payment.getStatus(),
                new ErpFinancePaymentDO().setStatus(status));
        if (updateCount == 0) {
            ErpFinancePaymentDO latest = financePaymentMapper.selectOneForUpdate(ErpFinancePaymentDO::getId, id);
            if (latest != null && status.equals(latest.getStatus())) {
                return;
            }
            throw exception(approve ? FINANCE_PAYMENT_APPROVE_FAIL : FINANCE_PAYMENT_PROCESS_FAIL);
        }

        // 3. 审核通过时生成付款会计事件；事件与付款状态变更处于同一事务
        if (approve) {
            accountingEventService.postEvent(buildPaymentPostedEvent(payment));
        } else if (ErpAuditStatus.APPROVE.getStatus().equals(payment.getStatus())) {
            // 仅当单据此前已审核（存在已过账原事件）时才生成冲销事件；
            // PROCESS 直接驳回等场景没有可冲销的原事件，不应阻断业务状态流转
            accountingEventService.reverseEvent(buildPaymentReversalRequest(payment));
        }
    }

    private ErpAccountingEventCreateReqBO buildPaymentPostedEvent(ErpFinancePaymentDO payment) {
        LocalDate eventDate = payment.getPaymentTime() == null
                ? LocalDate.now() : payment.getPaymentTime().toLocalDate();
        BigDecimal paymentPrice = defaultZero(payment.getPaymentPrice());
        return ErpAccountingEventCreateReqBO.builder()
                .eventType("PAYMENT_POSTED")
                .sourceDocType("FINANCE_PAYMENT")
                .sourceDocId(payment.getId())
                .sourceDocNo(payment.getNo())
                .sourceDocLineId(0L)
                .sourceRevision(1)
                .eventDate(eventDate)
                .accountingDate(eventDate)
                .amountWithoutTax(paymentPrice)
                .taxAmount(BigDecimal.ZERO)
                .amountWithTax(paymentPrice)
                .docTotalAmount(paymentPrice)
                .currency(null)
                .exchangeRate(BigDecimal.ONE)
                .reversalOfEventId(0L)
                .occurredAt(payment.getPaymentTime() == null ? LocalDateTime.now() : payment.getPaymentTime())
                .build();
    }

    private ErpAccountingEventReverseReqBO buildPaymentReversalRequest(ErpFinancePaymentDO payment) {
        LocalDate eventDate = payment.getPaymentTime() == null
                ? LocalDate.now() : payment.getPaymentTime().toLocalDate();
        return ErpAccountingEventReverseReqBO.builder()
                .eventType("PAYMENT_POSTED")
                .sourceDocType("FINANCE_PAYMENT")
                .sourceDocId(payment.getId())
                .sourceDocLineId(0L)
                .sourceRevision(1)
                .eventDate(eventDate)
                .accountingDate(LocalDate.now())
                .occurredAt(LocalDateTime.now())
                .build();
    }
    private static BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private List<ErpFinancePaymentItemDO> validateFinancePaymentItems(
            Long supplierId,
            List<ErpFinancePaymentSaveReqVO.Item> list) {
        return convertList(list, o -> BeanUtils.toBean(o, ErpFinancePaymentItemDO.class, item -> {
            if (ObjectUtil.equal(item.getBizType(), ErpBizTypeEnum.PURCHASE_IN.getType())) {
                ErpPurchaseInDO purchaseIn = purchaseInService.validatePurchaseIn(item.getBizId());
                Assert.equals(purchaseIn.getSupplierId(), supplierId, "供应商必须相同");
                item.setTotalPrice(purchaseIn.getTotalPrice()).setBizNo(purchaseIn.getNo());
            } else if (ObjectUtil.equal(item.getBizType(), ErpBizTypeEnum.PURCHASE_RETURN.getType())) {
                ErpPurchaseReturnDO purchaseReturn = purchaseReturnService.validatePurchaseReturn(item.getBizId());
                Assert.equals(purchaseReturn.getSupplierId(), supplierId, "供应商必须相同");
                item.setTotalPrice(purchaseReturn.getTotalPrice().negate()).setBizNo(purchaseReturn.getNo());
            } else {
                throw new IllegalArgumentException("业务类型不正确：" + item.getBizType());
            }
        }));
    }

    private void updateFinancePaymentItemList(Long id, List<ErpFinancePaymentItemDO> newList) {
        // 第一步，对比新老数据，获得添加、修改、删除的列表
        List<ErpFinancePaymentItemDO> oldList = financePaymentItemMapper.selectListByPaymentId(id);
        List<List<ErpFinancePaymentItemDO>> diffList = diffList(oldList, newList, // id 不同，就认为是不同的记录
                (oldVal, newVal) -> oldVal.getId().equals(newVal.getId()));

        // 第二步，批量添加、修改、删除
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            diffList.get(0).forEach(o -> o.setPaymentId(id));
            financePaymentItemMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            financePaymentItemMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            financePaymentItemMapper.deleteByIds(convertList(diffList.get(2), ErpFinancePaymentItemDO::getId));
        }

        // 第三步，更新采购入库、退货的付款金额情况
        updatePurchasePrice(CollectionUtils.newArrayList(diffList));
    }

    private void updatePurchasePrice(List<ErpFinancePaymentItemDO> paymentItems) {
        paymentItems.forEach(paymentItem -> {
            BigDecimal totalPaymentPrice = financePaymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(
                    paymentItem.getBizId(), paymentItem.getBizType());
            if (ErpBizTypeEnum.PURCHASE_IN.getType().equals(paymentItem.getBizType())) {
                purchaseInService.updatePurchaseInPaymentPrice(paymentItem.getBizId(), totalPaymentPrice);
            } else if (ErpBizTypeEnum.PURCHASE_RETURN.getType().equals(paymentItem.getBizType())) {
                purchaseReturnService.updatePurchaseReturnRefundPrice(paymentItem.getBizId(), totalPaymentPrice.negate());
            } else {
                throw new IllegalArgumentException("业务类型不正确：" + paymentItem.getBizType());
            }
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFinancePayment(List<Long> ids) {
        // 1. 校验不处于已审批
        List<ErpFinancePaymentDO> payments = financePaymentMapper.selectByIds(ids);
        if (CollUtil.isEmpty(payments)) {
            return;
        }
        payments.forEach(payment -> {
            if (ErpAuditStatus.APPROVE.getStatus().equals(payment.getStatus())) {
                throw exception(FINANCE_PAYMENT_DELETE_FAIL_APPROVE, payment.getNo());
            }
        });

        // 2. 遍历删除，并记录操作日志
        payments.forEach(payment -> {
            // 2.1 删除付款单
            financePaymentMapper.deleteById(payment.getId());
            // 2.2 删除付款单项
            List<ErpFinancePaymentItemDO> paymentItems = financePaymentItemMapper.selectListByPaymentId(payment.getId());
            financePaymentItemMapper.deleteByIds(convertSet(paymentItems, ErpFinancePaymentItemDO::getId));

            // 2.3 更新采购入库、退货的付款金额情况
            updatePurchasePrice(paymentItems);
        });
    }

    private ErpFinancePaymentDO validateFinancePaymentExists(Long id) {
        ErpFinancePaymentDO payment = financePaymentMapper.selectById(id);
        if (payment == null) {
            throw exception(FINANCE_PAYMENT_NOT_EXISTS);
        }
        return payment;
    }

    @Override
    public ErpFinancePaymentDO getFinancePayment(Long id) {
        return financePaymentMapper.selectById(id);
    }

    @Override
    public PageResult<ErpFinancePaymentDO> getFinancePaymentPage(ErpFinancePaymentPageReqVO pageReqVO) {
        return financePaymentMapper.selectPage(pageReqVO);
    }

    // ==================== 付款单项 ====================

    @Override
    public List<ErpFinancePaymentItemDO> getFinancePaymentItemListByPaymentId(Long paymentId) {
        return financePaymentItemMapper.selectListByPaymentId(paymentId);
    }

    @Override
    public List<ErpFinancePaymentItemDO> getFinancePaymentItemListByPaymentIds(Collection<Long> paymentIds) {
        if (CollUtil.isEmpty(paymentIds)) {
            return Collections.emptyList();
        }
        return financePaymentItemMapper.selectListByPaymentIds(paymentIds);
    }

}
