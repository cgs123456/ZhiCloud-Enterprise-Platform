package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.finance.ErpPeriodCloseStatusEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpPeriodCloseTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpPeriodStatusEnum;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.period.ErpPeriodCloseResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.period.ErpPeriodPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.period.ErpPeriodSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpPeriodCloseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpPeriodDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.*;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockOutMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 会计期间 Service 实现类（P0-6）
 *
 * <p>实现期末处理三大功能：月末检查、调汇、损益结转。
 * 简化版不涉及凭证生成，仅基于已有业务单据进行汇总。
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class ErpPeriodServiceImpl implements ErpPeriodService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Resource
    private ErpPeriodMapper periodMapper;
    @Resource
    private ErpPeriodCloseMapper periodCloseMapper;

    // 业务单据 Mapper（用于月末检查和损益结转的数据汇总）
    @Resource
    private ErpPurchaseInMapper purchaseInMapper;
    @Resource
    private ErpSaleOutMapper saleOutMapper;
    @Resource
    private ErpStockInMapper stockInMapper;
    @Resource
    private ErpStockOutMapper stockOutMapper;
    @Resource
    private ErpFinancePaymentMapper financePaymentMapper;
    @Resource
    private ErpFinanceReceiptMapper financeReceiptMapper;

    // ==================== 会计期间 CRUD ====================

    @Override
    public Long createPeriod(ErpPeriodSaveReqVO createReqVO) {
        // 校验编码唯一
        if (periodMapper.selectByCode(createReqVO.getCode()) != null) {
            throw exception(PERIOD_CODE_DUPLICATE, createReqVO.getCode());
        }
        ErpPeriodDO period = BeanUtils.toBean(createReqVO, ErpPeriodDO.class);
        if (period.getStatus() == null) {
            period.setStatus(ErpPeriodStatusEnum.OPEN.getStatus());
        }
        periodMapper.insert(period);
        return period.getId();
    }

    @Override
    public void updatePeriod(ErpPeriodSaveReqVO updateReqVO) {
        ErpPeriodDO existing = validatePeriodExists(updateReqVO.getId());
        // 已关账期间不可修改
        if (ErpPeriodStatusEnum.CLOSED.getStatus().equals(existing.getStatus())) {
            throw exception(PERIOD_ALREADY_CLOSED, existing.getCode());
        }
        ErpPeriodDO updateObj = BeanUtils.toBean(updateReqVO, ErpPeriodDO.class);
        periodMapper.updateById(updateObj);
    }

    @Override
    public void deletePeriod(Long id) {
        ErpPeriodDO existing = validatePeriodExists(id);
        if (!ErpPeriodStatusEnum.OPEN.getStatus().equals(existing.getStatus())) {
            throw exception(PERIOD_ALREADY_CLOSED, existing.getCode());
        }
        periodMapper.deleteById(id);
    }

    @Override
    public ErpPeriodDO getPeriod(Long id) {
        return periodMapper.selectById(id);
    }

    @Override
    public ErpPeriodDO getPeriodByCode(String code) {
        return periodMapper.selectByCode(code);
    }

    @Override
    public PageResult<ErpPeriodDO> getPeriodPage(ErpPeriodPageReqVO pageReqVO) {
        return periodMapper.selectPage(pageReqVO,
                new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<ErpPeriodDO>()
                        .eqIfPresent(ErpPeriodDO::getYear, pageReqVO.getYear())
                        .likeIfPresent(ErpPeriodDO::getCode, pageReqVO.getCode())
                        .eqIfPresent(ErpPeriodDO::getStatus, pageReqVO.getStatus())
                        .orderByDesc(ErpPeriodDO::getCode));
    }

    // ==================== 期末处理链 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpPeriodCloseResultRespVO executeMonthCheck(Long periodId) {
        // 1. 校验存在
        ErpPeriodDO period = validatePeriodExists(periodId);
        // 2. 校验状态：必须 OPEN
        if (!ErpPeriodStatusEnum.OPEN.getStatus().equals(period.getStatus())) {
            throw exception(PERIOD_NOT_OPEN, period.getCode());
        }

        // 3. 统计该期间内未审核单据
        LocalDateTime[] timeRange = buildTimeRange(period);
        Integer unapprovedPurchaseIn = countUnapproved(purchaseInMapper, timeRange);
        Integer unapprovedSaleOut = countUnapproved(saleOutMapper, timeRange);
        Integer unapprovedStockIn = countUnapproved(stockInMapper, timeRange);
        Integer unapprovedStockOut = countUnapproved(stockOutMapper, timeRange);
        Integer unapprovedPayment = countUnapproved(financePaymentMapper, timeRange);
        Integer unapprovedReceipt = countUnapproved(financeReceiptMapper, timeRange);

        // 4. 构造结果
        ErpPeriodCloseResultRespVO result = new ErpPeriodCloseResultRespVO();
        result.setPeriodId(periodId);
        result.setPeriodCode(period.getCode());
        result.setType(ErpPeriodCloseTypeEnum.MONTH_CHECK.getType());
        result.setProcessStatus(ErpPeriodCloseStatusEnum.SUCCESS.getStatus());
        result.setExecutedBy(getCurrentUser());
        result.setExecutedTime(LocalDateTime.now());
        result.setUnapprovedPurchaseInCount(unapprovedPurchaseIn);
        result.setUnapprovedSaleOutCount(unapprovedSaleOut);
        result.setUnapprovedStockInCount(unapprovedStockIn);
        result.setUnapprovedStockOutCount(unapprovedStockOut);
        result.setUnapprovedPaymentCount(unapprovedPayment);
        result.setUnapprovedReceiptCount(unapprovedReceipt);

        // 5. 构造摘要 JSON
        Map<String, Object> summaryMap = new HashMap<>();
        summaryMap.put("unapprovedPurchaseInCount", unapprovedPurchaseIn);
        summaryMap.put("unapprovedSaleOutCount", unapprovedSaleOut);
        summaryMap.put("unapprovedStockInCount", unapprovedStockIn);
        summaryMap.put("unapprovedStockOutCount", unapprovedStockOut);
        summaryMap.put("unapprovedPaymentCount", unapprovedPayment);
        summaryMap.put("unapprovedReceiptCount", unapprovedReceipt);
        result.setSummary(toJson(summaryMap));

        // 6. 写入期末处理记录（覆盖式：先删除已有 MONTH_CHECK 记录，再插入）
        upsertPeriodCloseRecord(periodId, period.getCode(),
                ErpPeriodCloseTypeEnum.MONTH_CHECK, result.getSummary(), null, result.getExecutedBy());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpPeriodCloseResultRespVO executeRevaluation(Long periodId, BigDecimal periodExchangeRate) {
        // 1. 校验存在 + 状态
        ErpPeriodDO period = validatePeriodExists(periodId);
        if (!ErpPeriodStatusEnum.OPEN.getStatus().equals(period.getStatus())) {
            throw exception(PERIOD_NOT_OPEN, period.getCode());
        }
        // 2. 校验前置条件：必须已执行月末检查
        ensureMonthCheckExecuted(periodId, period.getCode());

        // 3. 简化版：当前 ErpAccountDO 没有币种字段，所以无外币账户需要调整时返回跳过
        // 实际生产时，应在此处查询启用外币的账户，根据记账汇率与期末汇率计算调整额
        ErpPeriodCloseResultRespVO result = new ErpPeriodCloseResultRespVO();
        result.setPeriodId(periodId);
        result.setPeriodCode(period.getCode());
        result.setType(ErpPeriodCloseTypeEnum.REVALUATION.getType());
        result.setProcessStatus(ErpPeriodCloseStatusEnum.SKIPPED.getStatus());
        result.setExecutedBy(getCurrentUser());
        result.setExecutedTime(LocalDateTime.now());
        result.setAdjustedAccountCount(0);
        result.setAdjustmentAmount(BigDecimal.ZERO);

        Map<String, Object> summaryMap = new HashMap<>();
        summaryMap.put("adjustedAccountCount", 0);
        summaryMap.put("adjustmentAmount", BigDecimal.ZERO);
        summaryMap.put("periodExchangeRate", periodExchangeRate);
        summaryMap.put("reason", "当前账户体系未启用外币，跳过调汇");
        result.setSummary(toJson(summaryMap));

        upsertPeriodCloseRecord(periodId, period.getCode(),
                ErpPeriodCloseTypeEnum.REVALUATION, result.getSummary(),
                BigDecimal.ZERO, result.getExecutedBy());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpPeriodCloseResultRespVO executeProfitLossTransfer(Long periodId) {
        // 1. 校验存在 + 状态
        ErpPeriodDO period = validatePeriodExists(periodId);
        if (!ErpPeriodStatusEnum.OPEN.getStatus().equals(period.getStatus())) {
            throw exception(PERIOD_NOT_OPEN, period.getCode());
        }
        // 2. 校验前置条件：必须已执行月末检查
        ensureMonthCheckExecuted(periodId, period.getCode());

        // 3. 汇总本期间已审核的销售出库单（收入）
        LocalDateTime[] timeRange = buildTimeRange(period);
        BigDecimal totalRevenue = sumApprovedTotalPrice(saleOutMapper, timeRange);
        // 4. 汇总本期间已审核的采购入库单（成本）
        BigDecimal totalExpense = sumApprovedTotalPrice(purchaseInMapper, timeRange);
        // 5. 计算本期净利润
        BigDecimal netProfit = totalRevenue.subtract(totalExpense);

        // 6. 构造结果
        ErpPeriodCloseResultRespVO result = new ErpPeriodCloseResultRespVO();
        result.setPeriodId(periodId);
        result.setPeriodCode(period.getCode());
        result.setType(ErpPeriodCloseTypeEnum.PROFIT_LOSS_TRANSFER.getType());
        result.setProcessStatus(ErpPeriodCloseStatusEnum.SUCCESS.getStatus());
        result.setExecutedBy(getCurrentUser());
        result.setExecutedTime(LocalDateTime.now());
        result.setTotalRevenue(totalRevenue);
        result.setTotalExpense(totalExpense);
        result.setNetProfit(netProfit);

        Map<String, Object> summaryMap = new HashMap<>();
        summaryMap.put("totalRevenue", totalRevenue);
        summaryMap.put("totalExpense", totalExpense);
        summaryMap.put("netProfit", netProfit);
        result.setSummary(toJson(summaryMap));

        // 7. 写入期末处理记录
        upsertPeriodCloseRecord(periodId, period.getCode(),
                ErpPeriodCloseTypeEnum.PROFIT_LOSS_TRANSFER, result.getSummary(),
                netProfit, result.getExecutedBy());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closePeriod(Long periodId) {
        // 1. 校验存在
        ErpPeriodDO period = validatePeriodExists(periodId);
        // 2. 校验状态
        if (ErpPeriodStatusEnum.CLOSED.getStatus().equals(period.getStatus())) {
            throw exception(PERIOD_ALREADY_CLOSED, period.getCode());
        }
        // 3. 校验前置条件：已执行月末检查 + 已执行损益结转
        ensureMonthCheckExecuted(periodId, period.getCode());
        if (!periodCloseMapper.existsSuccess(periodId, ErpPeriodCloseTypeEnum.PROFIT_LOSS_TRANSFER)) {
            throw exception(PERIOD_CLOSE_FAIL_NOT_DO_TRANSFER, period.getCode());
        }
        // 4. 校验无未审核单据（基于最近一次月末检查的 summary）
        ErpPeriodCloseDO monthCheckRecord = periodCloseMapper.selectByPeriodAndType(periodId,
                ErpPeriodCloseTypeEnum.MONTH_CHECK.getType());
        if (monthCheckRecord != null && StrUtil.isNotBlank(monthCheckRecord.getSummary())) {
            Map<String, Object> summary = parseJson(monthCheckRecord.getSummary());
            int totalUnapproved = sumUnapproved(summary);
            if (totalUnapproved > 0) {
                throw exception(PERIOD_CLOSE_FAIL_EXISTS_UNAPPROVED, period.getCode(), totalUnapproved);
            }
        }
        // 5. 更新期间状态为 CLOSED
        ErpPeriodDO updateObj = new ErpPeriodDO();
        updateObj.setId(periodId);
        updateObj.setStatus(ErpPeriodStatusEnum.CLOSED.getStatus());
        updateObj.setClosedBy(getCurrentUser());
        updateObj.setClosedTime(LocalDateTime.now());
        periodMapper.updateById(updateObj);
    }

    // ==================== 辅助方法 ====================

    private ErpPeriodDO validatePeriodExists(Long id) {
        ErpPeriodDO period = periodMapper.selectById(id);
        if (period == null) {
            throw exception(PERIOD_NOT_EXISTS);
        }
        return period;
    }

    private void ensureMonthCheckExecuted(Long periodId, String periodCode) {
        if (!periodCloseMapper.existsSuccess(periodId, ErpPeriodCloseTypeEnum.MONTH_CHECK)) {
            throw exception(PERIOD_CLOSE_FAIL_NOT_DO_MONTH_CHECK, periodCode);
        }
    }

    private LocalDateTime[] buildTimeRange(ErpPeriodDO period) {
        LocalDate start = period.getStartDate();
        LocalDate end = period.getEndDate();
        return new LocalDateTime[]{
                start.atStartOfDay(),
                end.atTime(LocalTime.MAX)
        };
    }

    /**
     * 统计指定时间区间内未审核（status=PROCESS）的单据数
     *
     * <p>使用 MyBatis-Plus 的 selectCount + LambdaQueryWrapper。
     * 单据的时间字段名各不相同，这里通过反射统一调用 getTimeField。
     * 为简化实现，采用直接写每个 Mapper 的查询。
     * <p><b>容错设计</b>：当对应业务子模块的表未导入数据库时（BadSqlGrammarException），
     * 视为该模块无未审核单据，返回 0，不影响期末处理链整体流程。
     * 这样可让会计期间模块独立部署，不强依赖所有 ERP 业务子模块的表结构。
     */
    private Integer countUnapproved(ErpPurchaseInMapper mapper, LocalDateTime[] timeRange) {
        return safeCount("erp_purchase_in", () -> mapper.selectCount(
                new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<ErpPurchaseInDO>()
                        .eq(ErpPurchaseInDO::getStatus, ErpAuditStatus.PROCESS.getStatus())
                        .between(ErpPurchaseInDO::getInTime, timeRange[0], timeRange[1])));
    }

    private Integer countUnapproved(ErpSaleOutMapper mapper, LocalDateTime[] timeRange) {
        return safeCount("erp_sale_out", () -> mapper.selectCount(
                new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<ErpSaleOutDO>()
                        .eq(ErpSaleOutDO::getStatus, ErpAuditStatus.PROCESS.getStatus())
                        .between(ErpSaleOutDO::getOutTime, timeRange[0], timeRange[1])));
    }

    private Integer countUnapproved(ErpStockInMapper mapper, LocalDateTime[] timeRange) {
        return safeCount("erp_stock_in", () -> mapper.selectCount(
                new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<ErpStockInDO>()
                        .eq(ErpStockInDO::getStatus, ErpAuditStatus.PROCESS.getStatus())
                        .between(ErpStockInDO::getInTime, timeRange[0], timeRange[1])));
    }

    private Integer countUnapproved(ErpStockOutMapper mapper, LocalDateTime[] timeRange) {
        return safeCount("erp_stock_out", () -> mapper.selectCount(
                new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<ErpStockOutDO>()
                        .eq(ErpStockOutDO::getStatus, ErpAuditStatus.PROCESS.getStatus())
                        .between(ErpStockOutDO::getOutTime, timeRange[0], timeRange[1])));
    }

    private Integer countUnapproved(ErpFinancePaymentMapper mapper, LocalDateTime[] timeRange) {
        return safeCount("erp_finance_payment", () -> mapper.selectCount(
                new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<ErpFinancePaymentDO>()
                        .eq(ErpFinancePaymentDO::getStatus, ErpAuditStatus.PROCESS.getStatus())
                        .between(ErpFinancePaymentDO::getPaymentTime, timeRange[0], timeRange[1])));
    }

    private Integer countUnapproved(ErpFinanceReceiptMapper mapper, LocalDateTime[] timeRange) {
        return safeCount("erp_finance_receipt", () -> mapper.selectCount(
                new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<ErpFinanceReceiptDO>()
                        .eq(ErpFinanceReceiptDO::getStatus, ErpAuditStatus.PROCESS.getStatus())
                        .between(ErpFinanceReceiptDO::getReceiptTime, timeRange[0], timeRange[1])));
    }

    /**
     * 汇总已审核单据的 totalPrice
     *
     * <p>同样采用容错设计：表未导入时返回 BigDecimal.ZERO，不影响损益结转流程。
     */
    private BigDecimal sumApprovedTotalPrice(ErpSaleOutMapper mapper, LocalDateTime[] timeRange) {
        return safeSum("erp_sale_out", () -> {
            java.util.List<ErpSaleOutDO> list = mapper.selectList(
                    new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<ErpSaleOutDO>()
                            .eq(ErpSaleOutDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                            .between(ErpSaleOutDO::getOutTime, timeRange[0], timeRange[1]));
            return list.stream().map(ErpSaleOutDO::getTotalPrice)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        });
    }

    private BigDecimal sumApprovedTotalPrice(ErpPurchaseInMapper mapper, LocalDateTime[] timeRange) {
        return safeSum("erp_purchase_in", () -> {
            java.util.List<ErpPurchaseInDO> list = mapper.selectList(
                    new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<ErpPurchaseInDO>()
                            .eq(ErpPurchaseInDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                            .between(ErpPurchaseInDO::getInTime, timeRange[0], timeRange[1]));
            return list.stream().map(ErpPurchaseInDO::getTotalPrice)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        });
    }

    /**
     * 安全计数包装：捕获业务子模块表未导入导致的 SQLException / BadSqlGrammarException，
     * 返回 0 而非抛出异常，保证期末处理链在缺少部分业务子模块时仍可执行。
     */
    private Integer safeCount(String module, java.util.function.Supplier<Long> counter) {
        try {
            Long count = counter.get();
            return count == null ? 0 : count.intValue();
        } catch (Exception e) {
            log.warn("[safeCount][{} 表未导入或查询失败，本次统计按 0 处理]", module, e);
            return 0;
        }
    }

    /**
     * 安全汇总包装：捕获业务子模块表未导入异常，返回 BigDecimal.ZERO。
     */
    private BigDecimal safeSum(String module, java.util.function.Supplier<BigDecimal> summer) {
        try {
            return summer.get();
        } catch (Exception e) {
            log.warn("[safeSum][{} 表未导入或查询失败，本次汇总按 0 处理]", module, e);
            return BigDecimal.ZERO;
        }
    }

    private void upsertPeriodCloseRecord(Long periodId, String periodCode,
                                         ErpPeriodCloseTypeEnum type, String summary,
                                         BigDecimal adjustmentAmount, String executedBy) {
        // 先删除已有记录（保证幂等）
        ErpPeriodCloseDO existing = periodCloseMapper.selectByPeriodAndType(periodId, type.getType());
        if (existing != null) {
            periodCloseMapper.deleteById(existing.getId());
        }
        ErpPeriodCloseDO record = ErpPeriodCloseDO.builder()
                .periodId(periodId)
                .periodCode(periodCode)
                .type(type.getType())
                .executedBy(executedBy)
                .executedTime(LocalDateTime.now())
                .processStatus(ErpPeriodCloseStatusEnum.SUCCESS.getStatus())
                .summary(summary)
                .adjustmentAmount(adjustmentAmount)
                .build();
        periodCloseMapper.insert(record);
    }

    private int sumUnapproved(Map<String, Object> summary) {
        int total = 0;
        String[] keys = {"unapprovedPurchaseInCount", "unapprovedSaleOutCount",
                "unapprovedStockInCount", "unapprovedStockOutCount",
                "unapprovedPaymentCount", "unapprovedReceiptCount"};
        for (String key : keys) {
            Object val = summary.get(key);
            if (val instanceof Number) {
                total += ((Number) val).intValue();
            }
        }
        return total;
    }

    private String getCurrentUser() {
        // 简化版：从 SecurityContextHolder 获取，这里先返回 "system"
        // 实际生产中可通过 SecurityFrameworkUtils.getLoginUserNickname()
        try {
            return cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserNickname();
        } catch (Exception e) {
            return "system";
        }
    }

    private String toJson(Map<String, Object> map) {
        try {
            return OBJECT_MAPPER.writeValueAsString(map);
        } catch (Exception e) {
            log.error("序列化期末处理摘要失败", e);
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, Map.class);
        } catch (Exception e) {
            log.error("反序列化期末处理摘要失败", e);
            return new HashMap<>();
        }
    }

}
