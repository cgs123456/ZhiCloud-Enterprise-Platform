package cn.iocoder.yudao.module.erp.service.production.mps;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.production.mps.vo.ErpMpsPlanGenerateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.production.mps.vo.ErpMpsPlanPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.production.mps.vo.ErpMpsPlanSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.mps.ErpMpsPlanDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.mps.ErpMpsPlanDetailDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.production.mps.ErpMpsPlanDetailMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.mps.ErpMpsPlanMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.production.mps.ErpMpsPlanSourceEnum;
import cn.iocoder.yudao.module.erp.enums.production.mps.ErpMpsPlanStatusEnum;
import cn.iocoder.yudao.module.erp.enums.production.mps.ErpMpsPlanTypeEnum;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleOrderService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.api.ErpMrpExecutorGateway;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.MPS_PLAN_NO_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.MPS_PLAN_NOT_CONFIRMABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.MPS_PLAN_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.MPS_PLAN_NOT_RELEASABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.MPS_PLAN_PERIOD_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.MPS_PLAN_STATUS_INVALID;

/**
 * ERP 主生产计划 Service 实现类
 *
 * MPS 核心算法：
 * 1. 毛需求 = max(销售订单, 预测)（取大值策略，保守）
 * 2. 计划接收 = 已下达订单的预计入库
 * 3. 预计可用库存 = 期初库存 + 计划接收 - 毛需求
 * 4. 当预计可用库存 < 安全库存时，生成计划订单（netRequirement = safetyStock - projectedAvailable）
 * 5. 计划订单下达时间 = 计划订单接收 - 提前期
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class ErpMpsPlanServiceImpl implements ErpMpsPlanService {

    /**
     * 默认提前期（天）
     */
    private static final int DEFAULT_LEAD_TIME_DAYS = 7;
    /**
     * 默认安全库存
     */
    private static final BigDecimal DEFAULT_SAFETY_STOCK = BigDecimal.ZERO;
    /**
     * 默认预测数量
     */
    private static final BigDecimal DEFAULT_FORECAST_QUANTITY = BigDecimal.ZERO;

    @Resource
    private ErpMpsPlanMapper mpsPlanMapper;
    @Resource
    private ErpMpsPlanDetailMapper mpsPlanDetailMapper;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpSaleOrderService saleOrderService;

    @Autowired(required = false)
    private ErpMrpExecutorGateway mrpExecutorGateway;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createMpsPlan(ErpMpsPlanSaveReqVO createReqVO) {
        // 校验编号唯一
        validatePlanNoUnique(null, createReqVO.getPlanNo());
        // 校验产品存在 + 冗余产品信息
        ErpProductDO product = productService.getProduct(createReqVO.getProductId());
        // 校验产品+周期不重复
        validatePeriodNotDuplicate(null, createReqVO.getProductId(), createReqVO.getPlanPeriod());
        // 插入
        ErpMpsPlanDO plan = BeanUtils.toBean(createReqVO, ErpMpsPlanDO.class);
        if (product != null) {
            plan.setProductCode(product.getBarCode());
            plan.setProductName(product.getName());
        }
        if (plan.getPlanType() == null) {
            plan.setPlanType(ErpMpsPlanTypeEnum.MONTH.getType());
        }
        if (plan.getSource() == null) {
            plan.setSource(ErpMpsPlanSourceEnum.SALE_ORDER.getSource());
        }
        plan.setStatus(ErpMpsPlanStatusEnum.DRAFT.getStatus());
        mpsPlanMapper.insert(plan);
        return plan.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMpsPlan(ErpMpsPlanSaveReqVO updateReqVO) {
        ErpMpsPlanDO existPlan = validateMpsPlan(updateReqVO.getId());
        // 只有草稿状态才能修改
        if (!ObjUtil.equal(existPlan.getStatus(), ErpMpsPlanStatusEnum.DRAFT.getStatus())) {
            throw exception(MPS_PLAN_STATUS_INVALID);
        }
        // 校验编号唯一
        validatePlanNoUnique(updateReqVO.getId(), updateReqVO.getPlanNo());
        // 校验产品+周期不重复
        validatePeriodNotDuplicate(updateReqVO.getId(), updateReqVO.getProductId(), updateReqVO.getPlanPeriod());
        // 更新
        ErpMpsPlanDO updateObj = BeanUtils.toBean(updateReqVO, ErpMpsPlanDO.class);
        ErpProductDO product = productService.getProduct(updateReqVO.getProductId());
        if (product != null) {
            updateObj.setProductCode(product.getBarCode());
            updateObj.setProductName(product.getName());
        }
        mpsPlanMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMpsPlan(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        for (Long id : ids) {
            ErpMpsPlanDO plan = validateMpsPlan(id);
            // 只有草稿/已关闭状态才能删除
            if (!ObjUtil.equal(plan.getStatus(), ErpMpsPlanStatusEnum.DRAFT.getStatus())
                    && !ObjUtil.equal(plan.getStatus(), ErpMpsPlanStatusEnum.CLOSED.getStatus())) {
                throw exception(MPS_PLAN_STATUS_INVALID);
            }
            // 删除明细
            mpsPlanDetailMapper.deleteByPlanId(id);
            mpsPlanMapper.deleteById(id);
        }
    }

    @Override
    public ErpMpsPlanDO getMpsPlan(Long id) {
        return mpsPlanMapper.selectById(id);
    }

    @Override
    public PageResult<ErpMpsPlanDO> getMpsPlanPage(ErpMpsPlanPageReqVO pageReqVO) {
        return mpsPlanMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateMpsPlan(ErpMpsPlanGenerateReqVO reqVO) {
        // 1. 校验产品存在
        ErpProductDO product = productService.getProduct(reqVO.getProductId());
        if (product == null) {
            throw exception(MPS_PLAN_NOT_EXISTS);
        }
        // 2. 校验产品+周期不重复
        validatePeriodNotDuplicate(null, reqVO.getProductId(), reqVO.getPlanPeriod());
        // 3. 参数处理（安全库存 / 提前期 / 预测）
        BigDecimal safetyStock = reqVO.getSafetyStock() != null ? reqVO.getSafetyStock() : DEFAULT_SAFETY_STOCK;
        int leadTimeDays = (reqVO.getLeadTimeDays() != null && reqVO.getLeadTimeDays() > 0)
                ? reqVO.getLeadTimeDays() : DEFAULT_LEAD_TIME_DAYS;
        BigDecimal forecastQty = reqVO.getForecastQuantity() != null ? reqVO.getForecastQuantity() : DEFAULT_FORECAST_QUANTITY;
        // 4. 获取期初库存
        BigDecimal beginningStock = stockService.getStockCount(reqVO.getProductId());
        if (beginningStock == null) {
            beginningStock = BigDecimal.ZERO;
        }
        // 5. 获取销售订单需求（已审核的销售订单中该产品的未出货数量）
        BigDecimal saleOrderQty = getSaleOrderDemand(reqVO.getProductId());
        // 6. 毛需求 = max(销售订单, 预测)（取大值策略，保守）
        BigDecimal grossRequirement = saleOrderQty.max(forecastQty);
        // 7. 计划接收 = 已下达订单的预计入库（新计划默认 0）
        BigDecimal scheduledReceipt = BigDecimal.ZERO;
        // 8. 预计可用库存 = 期初库存 + 计划接收 - 毛需求
        BigDecimal projectedAvailable = beginningStock.add(scheduledReceipt).subtract(grossRequirement);
        // 9. 当预计可用库存 < 安全库存时，生成计划订单
        BigDecimal plannedOrderReceipt = BigDecimal.ZERO;
        if (projectedAvailable.compareTo(safetyStock) < 0) {
            // netRequirement = safetyStock - projectedAvailable
            plannedOrderReceipt = safetyStock.subtract(projectedAvailable);
            if (plannedOrderReceipt.compareTo(BigDecimal.ZERO) < 0) {
                plannedOrderReceipt = BigDecimal.ZERO;
            }
        }
        // 10. 创建主计划
        LocalDate demandDate = parsePeriodEndDate(reqVO.getPlanPeriod());
        LocalDate releaseDate = demandDate.minusDays(leadTimeDays);
        String planNo = generatePlanNo(reqVO.getPlanPeriod());
        ErpMpsPlanDO plan = ErpMpsPlanDO.builder()
                .planNo(planNo)
                .productId(reqVO.getProductId())
                .productCode(product.getBarCode())
                .productName(product.getName())
                .planPeriod(reqVO.getPlanPeriod())
                .planType(ErpMpsPlanTypeEnum.MONTH.getType())
                .demandDate(demandDate)
                .plannedQuantity(plannedOrderReceipt)
                .plannedFinishDate(demandDate)
                .source(ErpMpsPlanSourceEnum.SALE_ORDER.getSource())
                .status(ErpMpsPlanStatusEnum.DRAFT.getStatus())
                .sort(0)
                .build();
        mpsPlanMapper.insert(plan);
        // 11. 创建明细（时段时间为需求日期所在周期）
        ErpMpsPlanDetailDO detail = ErpMpsPlanDetailDO.builder()
                .planId(plan.getId())
                .periodStart(parsePeriodStartDate(reqVO.getPlanPeriod()))
                .periodEnd(demandDate)
                .grossRequirement(grossRequirement)
                .scheduledReceipt(scheduledReceipt)
                .projectedAvailableBalance(projectedAvailable)
                .plannedOrderReceipt(plannedOrderReceipt)
                .plannedOrderRelease(plannedOrderReceipt)
                .sort(1)
                .build();
        mpsPlanDetailMapper.insert(detail);
        log.info("[generateMpsPlan][planNo({}) product({}) period({}) gross({}) stock({}) projected({}) plannedOrder({}) releaseDate({})]",
                planNo, reqVO.getProductId(), reqVO.getPlanPeriod(), grossRequirement, beginningStock,
                projectedAvailable, plannedOrderReceipt, releaseDate);
        return plan.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmPlan(Long id) {
        ErpMpsPlanDO plan = validateMpsPlan(id);
        // 只有草稿状态才能确认
        if (!ObjUtil.equal(plan.getStatus(), ErpMpsPlanStatusEnum.DRAFT.getStatus())) {
            throw exception(MPS_PLAN_NOT_CONFIRMABLE);
        }
        mpsPlanMapper.updateById(new ErpMpsPlanDO().setId(id)
                .setStatus(ErpMpsPlanStatusEnum.CONFIRMED.getStatus()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseToMrp(Long id) {
        ErpMpsPlanDO plan = validateMpsPlan(id);
        // 只有已确认状态才能下发 MRP
        if (!ObjUtil.equal(plan.getStatus(), ErpMpsPlanStatusEnum.CONFIRMED.getStatus())) {
            throw exception(MPS_PLAN_NOT_RELEASABLE);
        }
        // 通过 SPI 网关触发 MRP 运算（mes 模块未启用时降级为 WARN 日志，状态照常更新）
        if (mrpExecutorGateway != null) {
            mrpExecutorGateway.executeMrp(plan);
        } else {
            log.warn("[releaseToMrp][mes 模块未启用，跳过 MRP 运算，planId({}) planNo({})]",
                    id, plan.getPlanNo());
        }
        mpsPlanMapper.updateById(new ErpMpsPlanDO().setId(id)
                .setStatus(ErpMpsPlanStatusEnum.RELEASED_MRP.getStatus()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closePlan(Long id) {
        ErpMpsPlanDO plan = validateMpsPlan(id);
        // 只有已下发 MRP 状态才能关闭
        if (!ObjUtil.equal(plan.getStatus(), ErpMpsPlanStatusEnum.RELEASED_MRP.getStatus())) {
            throw exception(MPS_PLAN_STATUS_INVALID);
        }
        mpsPlanMapper.updateById(new ErpMpsPlanDO().setId(id)
                .setStatus(ErpMpsPlanStatusEnum.CLOSED.getStatus()));
    }

    @Override
    public ErpMpsPlanDO validateMpsPlan(Long id) {
        ErpMpsPlanDO plan = mpsPlanMapper.selectById(id);
        if (plan == null) {
            throw exception(MPS_PLAN_NOT_EXISTS);
        }
        return plan;
    }

    @Override
    public List<ErpMpsPlanDetailDO> getMpsPlanDetailListByPlanId(Long planId) {
        return mpsPlanDetailMapper.selectListByPlanId(planId);
    }

    // ==================== 校验方法 ====================

    private void validatePlanNoUnique(Long id, String planNo) {
        if (planNo == null) {
            return;
        }
        ErpMpsPlanDO plan = mpsPlanMapper.selectByPlanNo(planNo);
        if (plan == null) {
            return;
        }
        if (id == null || !ObjUtil.equal(plan.getId(), id)) {
            throw exception(MPS_PLAN_NO_DUPLICATE, planNo);
        }
    }

    private void validatePeriodNotDuplicate(Long id, Long productId, String planPeriod) {
        if (productId == null || planPeriod == null) {
            return;
        }
        ErpMpsPlanDO plan = mpsPlanMapper.selectByProductAndPeriod(productId, planPeriod);
        if (plan == null) {
            return;
        }
        if (id == null || !ObjUtil.equal(plan.getId(), id)) {
            throw exception(MPS_PLAN_PERIOD_DUPLICATE, productId, planPeriod);
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 查询已审核销售订单中指定产品的需求量（未出货部分）
     *
     * @param productId 产品编号
     * @return 销售订单需求量
     */
    private BigDecimal getSaleOrderDemand(Long productId) {
        // 1. 查询已审核状态的销售订单
        ErpSaleOrderPageReqVO pageReqVO = new ErpSaleOrderPageReqVO();
        pageReqVO.setStatus(ErpAuditStatus.APPROVE.getStatus());
        pageReqVO.setProductId(productId);
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ErpSaleOrderDO> saleOrders = saleOrderService.getSaleOrderPage(pageReqVO).getList();
        if (CollUtil.isEmpty(saleOrders)) {
            return BigDecimal.ZERO;
        }
        // 2. 获取订单项
        Set<Long> orderIds = saleOrders.stream().map(ErpSaleOrderDO::getId).collect(Collectors.toSet());
        List<ErpSaleOrderItemDO> items = saleOrderService.getSaleOrderItemListByOrderIds(orderIds);
        if (CollUtil.isEmpty(items)) {
            return BigDecimal.ZERO;
        }
        // 3. 累加该产品的未出货数量（count - outCount）
        return items.stream()
                .filter(item -> ObjUtil.equal(item.getProductId(), productId))
                .map(item -> {
                    BigDecimal count = item.getCount() != null ? item.getCount() : BigDecimal.ZERO;
                    BigDecimal outCount = item.getOutCount() != null ? item.getOutCount() : BigDecimal.ZERO;
                    BigDecimal remain = count.subtract(outCount);
                    return remain.compareTo(BigDecimal.ZERO) > 0 ? remain : BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 根据计划周期生成计划编号
     *
     * @param planPeriod 计划周期（yyyyMM）
     * @return 计划编号
     */
    private String generatePlanNo(String planPeriod) {
        return "MPS-" + planPeriod + "-" + System.currentTimeMillis() % 100000;
    }

    /**
     * 解析计划周期为周期开始日期
     *
     * @param planPeriod 计划周期（yyyyMM）
     * @return 周期开始日期
     */
    private LocalDate parsePeriodStartDate(String planPeriod) {
        int year = Integer.parseInt(planPeriod.substring(0, 4));
        int month = Integer.parseInt(planPeriod.substring(4, 6));
        return LocalDate.of(year, month, 1);
    }

    /**
     * 解析计划周期为周期结束日期（需求日期）
     *
     * @param planPeriod 计划周期（yyyyMM）
     * @return 周期结束日期
     */
    private LocalDate parsePeriodEndDate(String planPeriod) {
        LocalDate start = parsePeriodStartDate(planPeriod);
        return start.plusMonths(1).minusDays(1);
    }

}