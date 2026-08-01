package cn.iocoder.yudao.module.erp.service.purchase.evaluation;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.evaluation.vo.ErpSupplierEvaluationPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.evaluation.vo.ErpSupplierEvaluationSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.evaluation.ErpSupplierEvaluationDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.evaluation.ErpSupplierEvaluationItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.evaluation.ErpSupplierEvaluationItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.evaluation.ErpSupplierEvaluationMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseInService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseOrderService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseReturnService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_EVALUATION_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_EVALUATION_PERIOD_DUPLICATE;

/**
 * ERP 供应商评估 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class ErpSupplierEvaluationServiceImpl implements ErpSupplierEvaluationService {

    /**
     * 交期评分权重 30%
     */
    private static final BigDecimal WEIGHT_DELIVERY = new BigDecimal("30");
    /**
     * 质量评分权重 30%
     */
    private static final BigDecimal WEIGHT_QUALITY = new BigDecimal("30");
    /**
     * 价格评分权重 20%
     */
    private static final BigDecimal WEIGHT_PRICE = new BigDecimal("20");
    /**
     * 服务评分权重 20%
     */
    private static final BigDecimal WEIGHT_SERVICE = new BigDecimal("20");
    /**
     * 默认服务评分
     */
    private static final BigDecimal DEFAULT_SERVICE_SCORE = new BigDecimal("80");

    @Resource
    private ErpSupplierEvaluationMapper evaluationMapper;
    @Resource
    private ErpSupplierEvaluationItemMapper evaluationItemMapper;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpPurchaseInService purchaseInService;
    @Resource
    private ErpPurchaseReturnService purchaseReturnService;
    @Resource
    private ErpPurchaseOrderService purchaseOrderService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEvaluation(ErpSupplierEvaluationSaveReqVO createReqVO) {
        // 校验供应商存在
        supplierService.validateSupplier(createReqVO.getSupplierId());
        // 校验周期不重复
        validatePeriodDuplicate(null, createReqVO.getSupplierId(), createReqVO.getEvaluationPeriod());
        // 计算综合评分与等级
        BigDecimal totalScore = calcTotalScore(createReqVO);
        String grade = determineGrade(totalScore);
        // 插入主表
        ErpSupplierEvaluationDO evaluation = BeanUtils.toBean(createReqVO, ErpSupplierEvaluationDO.class);
        evaluation.setTotalScore(totalScore);
        evaluation.setGrade(grade);
        evaluationMapper.insert(evaluation);
        // 插入指标项
        saveItems(evaluation.getId(), createReqVO.getItems());
        return evaluation.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEvaluation(ErpSupplierEvaluationSaveReqVO updateReqVO) {
        ErpSupplierEvaluationDO exist = validateEvaluation(updateReqVO.getId());
        // 校验供应商存在
        supplierService.validateSupplier(updateReqVO.getSupplierId());
        // 校验周期不重复
        validatePeriodDuplicate(updateReqVO.getId(), updateReqVO.getSupplierId(), updateReqVO.getEvaluationPeriod());
        BigDecimal totalScore = calcTotalScore(updateReqVO);
        String grade = determineGrade(totalScore);
        ErpSupplierEvaluationDO updateObj = BeanUtils.toBean(updateReqVO, ErpSupplierEvaluationDO.class);
        updateObj.setTotalScore(totalScore);
        updateObj.setGrade(grade);
        evaluationMapper.updateById(updateObj);
        // 重建指标项
        evaluationItemMapper.deleteByEvaluationId(exist.getId());
        saveItems(exist.getId(), updateReqVO.getItems());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEvaluation(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        for (Long id : ids) {
            validateEvaluation(id);
            evaluationItemMapper.deleteByEvaluationId(id);
            evaluationMapper.deleteById(id);
        }
    }

    @Override
    public ErpSupplierEvaluationDO getEvaluation(Long id) {
        return evaluationMapper.selectById(id);
    }

    @Override
    public PageResult<ErpSupplierEvaluationDO> getEvaluationPage(ErpSupplierEvaluationPageReqVO pageReqVO) {
        return evaluationMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpSupplierEvaluationItemDO> getEvaluationItemListByEvaluationId(Long evaluationId) {
        return evaluationItemMapper.selectListByEvaluationId(evaluationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long calculateEvaluation(Long supplierId, String period) {
        // 校验供应商存在
        supplierService.validateSupplier(supplierId);
        // 校验周期不重复
        validatePeriodDuplicate(null, supplierId, period);
        // 周期起止时间
        YearMonth ym = parsePeriod(period);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();

        // 1. 查询周期内已审核的采购入库单
        List<ErpPurchaseInDO> purchaseIns = queryPurchaseIns(supplierId, start, end);
        // 2. 查询周期内已审核的采购退货单
        List<ErpPurchaseReturnDO> purchaseReturns = queryPurchaseReturns(supplierId, start, end);
        // 3. 查询周期内已审核的采购订单（用于价格波动）
        List<ErpPurchaseOrderDO> purchaseOrders = queryPurchaseOrders(supplierId, start, end);

        // 4. 计算各项评分
        // 4.1 交期评分 = 按时到货率 * 100（按时：入库时间 <= 订单交期，简化为按入库日期 <= 周期结束日）
        BigDecimal deliveryScore = calculateDeliveryScore(purchaseIns, end);
        // 4.2 质量评分 = (1 - 退货率) * 100
        BigDecimal qualityScore = calculateQualityScore(purchaseIns, purchaseReturns);
        // 4.3 价格评分 = 价格稳定度评分
        BigDecimal priceScore = calculatePriceScore(purchaseOrders);
        // 4.4 服务评分 = 默认 80
        BigDecimal serviceScore = DEFAULT_SERVICE_SCORE;

        // 5. 综合评分 = 加权平均
        BigDecimal totalScore = deliveryScore.multiply(WEIGHT_DELIVERY)
                .add(qualityScore.multiply(WEIGHT_QUALITY))
                .add(priceScore.multiply(WEIGHT_PRICE))
                .add(serviceScore.multiply(WEIGHT_SERVICE))
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        String grade = determineGrade(totalScore);

        // 6. 写入主表
        ErpSupplierEvaluationDO evaluation = ErpSupplierEvaluationDO.builder()
                .supplierId(supplierId)
                .evaluationPeriod(period)
                .qualityScore(qualityScore)
                .deliveryScore(deliveryScore)
                .priceScore(priceScore)
                .serviceScore(serviceScore)
                .totalScore(totalScore)
                .grade(grade)
                .evaluator("system")
                .remark("系统自动计算")
                .build();
        evaluationMapper.insert(evaluation);
        // 7. 写入指标项
        List<ErpSupplierEvaluationItemDO> items = buildItems(evaluation.getId(),
                deliveryScore, qualityScore, priceScore, serviceScore);
        if (CollUtil.isNotEmpty(items)) {
            evaluationItemMapper.insertBatch(items);
        }
        log.info("[calculateEvaluation][supplierId({}) period({}) delivery({}) quality({}) price({}) service({}) total({}) grade({})]",
                supplierId, period, deliveryScore, qualityScore, priceScore, serviceScore, totalScore, grade);
        return evaluation.getId();
    }

    @Override
    public ErpSupplierEvaluationDO validateEvaluation(Long id) {
        ErpSupplierEvaluationDO evaluation = evaluationMapper.selectById(id);
        if (evaluation == null) {
            throw exception(SUPPLIER_EVALUATION_NOT_EXISTS);
        }
        return evaluation;
    }

    // ==================== 私有方法 ====================

    private void validatePeriodDuplicate(Long id, Long supplierId, String period) {
        if (supplierId == null || period == null) {
            return;
        }
        ErpSupplierEvaluationDO evaluation = evaluationMapper.selectBySupplierAndPeriod(supplierId, period);
        if (evaluation == null) {
            return;
        }
        if (id == null || !ObjUtil.equal(evaluation.getId(), id)) {
            throw exception(SUPPLIER_EVALUATION_PERIOD_DUPLICATE, supplierId, period);
        }
    }

    private void saveItems(Long evaluationId, List<ErpSupplierEvaluationSaveReqVO.Item> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        List<ErpSupplierEvaluationItemDO> doList = new ArrayList<>();
        for (ErpSupplierEvaluationSaveReqVO.Item item : items) {
            ErpSupplierEvaluationItemDO itemDO = BeanUtils.toBean(item, ErpSupplierEvaluationItemDO.class);
            itemDO.setId(null);
            itemDO.setEvaluationId(evaluationId);
            // 计算加权得分
            if (itemDO.getScore() != null && itemDO.getWeight() != null) {
                itemDO.setWeightedScore(itemDO.getScore().multiply(itemDO.getWeight())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
            }
            doList.add(itemDO);
        }
        evaluationItemMapper.insertBatch(doList);
    }

    /**
     * 计算综合评分（综合 4 项评分加权）
     */
    private BigDecimal calcTotalScore(ErpSupplierEvaluationSaveReqVO reqVO) {
        if (reqVO.getTotalScore() != null) {
            return reqVO.getTotalScore();
        }
        BigDecimal delivery = nullToZero(reqVO.getDeliveryScore());
        BigDecimal quality = nullToZero(reqVO.getQualityScore());
        BigDecimal price = nullToZero(reqVO.getPriceScore());
        BigDecimal service = nullToZero(reqVO.getServiceScore());
        return delivery.multiply(WEIGHT_DELIVERY)
                .add(quality.multiply(WEIGHT_QUALITY))
                .add(price.multiply(WEIGHT_PRICE))
                .add(service.multiply(WEIGHT_SERVICE))
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    /**
     * 根据综合评分判定等级
     */
    private String determineGrade(BigDecimal totalScore) {
        if (totalScore == null) {
            return "D";
        }
        if (totalScore.compareTo(new BigDecimal("90")) >= 0) {
            return "A";
        }
        if (totalScore.compareTo(new BigDecimal("80")) >= 0) {
            return "B";
        }
        if (totalScore.compareTo(new BigDecimal("60")) >= 0) {
            return "C";
        }
        return "D";
    }

    private List<ErpPurchaseInDO> queryPurchaseIns(Long supplierId, LocalDateTime start, LocalDateTime end) {
        ErpPurchaseInPageReqVO pageReqVO = new ErpPurchaseInPageReqVO();
        pageReqVO.setSupplierId(supplierId);
        pageReqVO.setStatus(ErpAuditStatus.APPROVE.getStatus());
        pageReqVO.setInTime(new LocalDateTime[]{start, end});
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ErpPurchaseInDO> list = purchaseInService.getPurchaseInPage(pageReqVO).getList();
        return list == null ? new ArrayList<>() : list;
    }

    private List<ErpPurchaseReturnDO> queryPurchaseReturns(Long supplierId, LocalDateTime start, LocalDateTime end) {
        ErpPurchaseReturnPageReqVO pageReqVO = new ErpPurchaseReturnPageReqVO();
        pageReqVO.setSupplierId(supplierId);
        pageReqVO.setStatus(ErpAuditStatus.APPROVE.getStatus());
        pageReqVO.setReturnTime(new LocalDateTime[]{start, end});
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ErpPurchaseReturnDO> list = purchaseReturnService.getPurchaseReturnPage(pageReqVO).getList();
        return list == null ? new ArrayList<>() : list;
    }

    private List<ErpPurchaseOrderDO> queryPurchaseOrders(Long supplierId, LocalDateTime start, LocalDateTime end) {
        // 采购订单 Service 暴露的分页接口已支持订单时间过滤
        cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderPageReqVO pageReqVO =
                new cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderPageReqVO();
        pageReqVO.setSupplierId(supplierId);
        pageReqVO.setStatus(ErpAuditStatus.APPROVE.getStatus());
        pageReqVO.setOrderTime(new LocalDateTime[]{start, end});
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ErpPurchaseOrderDO> list = purchaseOrderService.getPurchaseOrderPage(pageReqVO).getList();
        if (CollUtil.isEmpty(list)) {
            return new ArrayList<>();
        }
        return list;
    }

    /**
     * 交期评分 = 按时到货率 * 100
     *
     * <p>按时判定：入库时间在周期结束日之前视为按时（简化逻辑，实际可对比订单交期）。
     */
    private BigDecimal calculateDeliveryScore(List<ErpPurchaseInDO> purchaseIns, LocalDateTime periodEnd) {
        if (CollUtil.isEmpty(purchaseIns)) {
            return new BigDecimal("80");
        }
        int onTimeCount = 0;
        for (ErpPurchaseInDO in : purchaseIns) {
            if (in.getInTime() != null && !in.getInTime().isAfter(periodEnd)) {
                onTimeCount++;
            }
        }
        BigDecimal ratio = new BigDecimal(onTimeCount)
                .divide(new BigDecimal(purchaseIns.size()), 4, RoundingMode.HALF_UP);
        return ratio.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 质量评分 = (1 - 退货率) * 100
     *
     * <p>退货率 = 退货数量 / 入库数量
     */
    private BigDecimal calculateQualityScore(List<ErpPurchaseInDO> purchaseIns,
                                             List<ErpPurchaseReturnDO> purchaseReturns) {
        BigDecimal inCount = sumCount(purchaseIns, ErpPurchaseInDO::getTotalCount);
        if (inCount.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal("80");
        }
        BigDecimal returnCount = sumCount(purchaseReturns, ErpPurchaseReturnDO::getTotalCount);
        BigDecimal returnRate = returnCount.divide(inCount, 4, RoundingMode.HALF_UP);
        if (returnRate.compareTo(BigDecimal.ONE) > 0) {
            returnRate = BigDecimal.ONE;
        }
        return BigDecimal.ONE.subtract(returnRate)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 价格评分 = 价格稳定度评分
     *
     * <p>价格波动率 = (max - min) / avg；
     * <p>波动率 <= 5% 满分 100；波动率 >= 30% 记 60；中间线性插值。
     */
    private BigDecimal calculatePriceScore(List<ErpPurchaseOrderDO> purchaseOrders) {
        if (CollUtil.isEmpty(purchaseOrders)) {
            return new BigDecimal("80");
        }
        List<BigDecimal> prices = purchaseOrders.stream()
                .map(ErpPurchaseOrderDO::getTotalPrice)
                .filter(java.util.Objects::nonNull)
                .filter(p -> p.compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
        if (prices.size() < 2) {
            return new BigDecimal("90");
        }
        BigDecimal max = prices.stream().reduce(BigDecimal::max).orElse(BigDecimal.ZERO);
        BigDecimal min = prices.stream().reduce(BigDecimal::min).orElse(BigDecimal.ZERO);
        BigDecimal sum = prices.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = sum.divide(new BigDecimal(prices.size()), 4, RoundingMode.HALF_UP);
        if (avg.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal("80");
        }
        BigDecimal volatility = max.subtract(min).divide(avg, 4, RoundingMode.HALF_UP);
        // 波动率 <= 5% 满分，>= 30% 记 60
        BigDecimal lowThreshold = new BigDecimal("0.05");
        BigDecimal highThreshold = new BigDecimal("0.30");
        if (volatility.compareTo(lowThreshold) <= 0) {
            return new BigDecimal("100.00");
        }
        if (volatility.compareTo(highThreshold) >= 0) {
            return new BigDecimal("60.00");
        }
        // 线性插值：5% -> 100, 30% -> 60
        BigDecimal score = new BigDecimal("100").subtract(
                volatility.subtract(lowThreshold)
                        .divide(highThreshold.subtract(lowThreshold), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("40")));
        return score.setScale(2, RoundingMode.HALF_UP);
    }

    private List<ErpSupplierEvaluationItemDO> buildItems(Long evaluationId, BigDecimal deliveryScore,
                                                         BigDecimal qualityScore, BigDecimal priceScore,
                                                         BigDecimal serviceScore) {
        List<ErpSupplierEvaluationItemDO> items = new ArrayList<>();
        items.add(buildItem(evaluationId, "交期评分（按时到货率）", deliveryScore, WEIGHT_DELIVERY));
        items.add(buildItem(evaluationId, "质量评分（退货率倒推）", qualityScore, WEIGHT_QUALITY));
        items.add(buildItem(evaluationId, "价格评分（价格波动率）", priceScore, WEIGHT_PRICE));
        items.add(buildItem(evaluationId, "服务评分（默认）", serviceScore, WEIGHT_SERVICE));
        return items;
    }

    private ErpSupplierEvaluationItemDO buildItem(Long evaluationId, String indicator,
                                                  BigDecimal score, BigDecimal weight) {
        BigDecimal weighted = score.multiply(weight)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        return ErpSupplierEvaluationItemDO.builder()
                .evaluationId(evaluationId)
                .indicator(indicator)
                .score(score)
                .weight(weight)
                .weightedScore(weighted)
                .build();
    }

    private <T> BigDecimal sumCount(List<T> list, java.util.function.Function<T, BigDecimal> getter) {
        if (CollUtil.isEmpty(list)) {
            return BigDecimal.ZERO;
        }
        return list.stream()
                .map(getter)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private YearMonth parsePeriod(String period) {
        int year = Integer.parseInt(period.substring(0, 4));
        int month = Integer.parseInt(period.substring(4, 6));
        return YearMonth.of(year, month);
    }

    private static BigDecimal nullToZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

}
