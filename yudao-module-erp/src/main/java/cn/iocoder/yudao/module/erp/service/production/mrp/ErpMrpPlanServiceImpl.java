package cn.iocoder.yudao.module.erp.service.production.mrp;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.production.mrp.vo.ErpMrpPlanPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.production.mrp.vo.ErpMrpPlanSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.mps.ErpMpsPlanDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.mrp.ErpMrpPlanDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.mrp.ErpMrpResultDO;
import cn.iocoder.yudao.module.erp.dal.mysql.production.mrp.ErpMrpPlanMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.mrp.ErpMrpResultMapper;
import cn.iocoder.yudao.module.erp.service.finance.cost.bom.ErpBomComponent;
import cn.iocoder.yudao.module.erp.service.finance.cost.bom.ErpBomProvider;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.production.mps.ErpMpsPlanService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.MRP_PLAN_NOT_EXECUTABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.MRP_PLAN_NOT_CONFIRMABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.MRP_PLAN_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.MRP_PLAN_NO_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.MRP_PLAN_STATUS_INVALID;

/**
 * ERP 物料需求计划 Service 实现类
 *
 * MRP 计算逻辑：
 * 1. 读取关联 MPS 计划及其明细，作为独立需求
 * 2. 按产品展开 BOM（通过 {@code ErpBomProvider}），计算相关需求
 * 3. 减去当前库存（{@code ErpStockService}）
 * 4. 生成净需求和计划订单（采购 / 生产）
 * 5. 结果写入 {@code ErpMrpResultDO}
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class ErpMrpPlanServiceImpl implements ErpMrpPlanService {

    /**
     * MRP 计划状态：10 草稿
     */
    private static final int STATUS_DRAFT = 10;
    /**
     * MRP 计划状态：20 已计算
     */
    private static final int STATUS_CALCULATED = 20;
    /**
     * MRP 计划状态：30 已确认
     */
    private static final int STATUS_CONFIRMED = 30;
    /**
     * MRP 计划状态：40 已关闭
     */
    private static final int STATUS_CLOSED = 40;

    /**
     * 需求类型：10 独立需求
     */
    private static final int DEMAND_TYPE_INDEPENDENT = 10;
    /**
     * 需求类型：20 相关需求
     */
    private static final int DEMAND_TYPE_DEPENDENT = 20;

    /**
     * 计划订单类型：10 采购
     */
    private static final int ORDER_TYPE_PURCHASE = 10;
    /**
     * 计划订单类型：20 生产
     */
    private static final int ORDER_TYPE_PRODUCE = 20;

    @Resource
    private ErpMrpPlanMapper mrpPlanMapper;
    @Resource
    private ErpMrpResultMapper mrpResultMapper;
    @Resource
    private ErpMpsPlanService mpsPlanService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpBomProvider bomProvider;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createMrpPlan(ErpMrpPlanSaveReqVO createReqVO) {
        // 校验编号唯一
        validateNoUnique(null, createReqVO.getNo());
        // 插入
        ErpMrpPlanDO plan = BeanUtils.toBean(createReqVO, ErpMrpPlanDO.class);
        plan.setStatus(STATUS_DRAFT);
        mrpPlanMapper.insert(plan);
        return plan.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMrpPlan(ErpMrpPlanSaveReqVO updateReqVO) {
        ErpMrpPlanDO existPlan = validateMrpPlan(updateReqVO.getId());
        // 只有草稿状态才能修改
        if (!ObjUtil.equal(existPlan.getStatus(), STATUS_DRAFT)) {
            throw exception(MRP_PLAN_STATUS_INVALID);
        }
        validateNoUnique(updateReqVO.getId(), updateReqVO.getNo());
        ErpMrpPlanDO updateObj = BeanUtils.toBean(updateReqVO, ErpMrpPlanDO.class);
        mrpPlanMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMrpPlan(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        for (Long id : ids) {
            ErpMrpPlanDO plan = validateMrpPlan(id);
            // 只有草稿/已关闭状态才能删除
            if (!ObjUtil.equal(plan.getStatus(), STATUS_DRAFT)
                    && !ObjUtil.equal(plan.getStatus(), STATUS_CLOSED)) {
                throw exception(MRP_PLAN_STATUS_INVALID);
            }
            // 删除结果
            mrpResultMapper.deleteByPlanId(id);
            mrpPlanMapper.deleteById(id);
        }
    }

    @Override
    public ErpMrpPlanDO getMrpPlan(Long id) {
        return mrpPlanMapper.selectById(id);
    }

    @Override
    public PageResult<ErpMrpPlanDO> getMrpPlanPage(ErpMrpPlanPageReqVO pageReqVO) {
        return mrpPlanMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeMrp(Long id) {
        ErpMrpPlanDO plan = validateMrpPlan(id);
        // 只有草稿/已计算状态才能执行计算
        if (!ObjUtil.equal(plan.getStatus(), STATUS_DRAFT)
                && !ObjUtil.equal(plan.getStatus(), STATUS_CALCULATED)) {
            throw exception(MRP_PLAN_NOT_EXECUTABLE);
        }
        // 清理旧结果
        mrpResultMapper.deleteByPlanId(id);

        List<ErpMrpResultDO> results = new ArrayList<>();
        BigDecimal totalDemand = BigDecimal.ZERO;
        BigDecimal totalPurchase = BigDecimal.ZERO;
        BigDecimal totalProduce = BigDecimal.ZERO;

        // 1. 读取 MPS 计划，作为独立需求
        LocalDate deliveryDate = plan.getPlanDate();
        if (plan.getMpsPlanId() != null) {
            ErpMpsPlanDO mpsPlan = mpsPlanService.getMpsPlan(plan.getMpsPlanId());
            if (mpsPlan != null) {
                if (mpsPlan.getDemandDate() != null) {
                    deliveryDate = mpsPlan.getDemandDate();
                }
                // MPS 计划数量作为独立需求
                BigDecimal mpsQty = mpsPlan.getPlannedQuantity() == null
                        ? BigDecimal.ZERO : mpsPlan.getPlannedQuantity();
                if (mpsQty.compareTo(BigDecimal.ZERO) > 0) {
                    ErpMrpResultDO independent = buildIndependentResult(
                            plan.getId(), mpsPlan.getProductId(), mpsPlan.getProductName(),
                            mpsQty, deliveryDate);
                    results.add(independent);
                    totalDemand = totalDemand.add(mpsQty);
                    // 独立需求：根据是否有 BOM 判定采购/生产
                    if (isProduceProduct(mpsPlan.getProductId())) {
                        totalProduce = totalProduce.add(independent.getPlannedOrderQuantity());
                    } else {
                        totalPurchase = totalPurchase.add(independent.getPlannedOrderQuantity());
                    }
                    // 2. 展开 BOM 计算相关需求
                    List<ErpMrpResultDO> dependentResults = expandBom(
                            plan.getId(), mpsPlan.getProductId(), mpsQty, deliveryDate);
                    for (ErpMrpResultDO r : dependentResults) {
                        results.add(r);
                        totalDemand = totalDemand.add(r.getDemandQuantity());
                        if (ObjUtil.equal(r.getPlannedOrderType(), ORDER_TYPE_PURCHASE)) {
                            totalPurchase = totalPurchase.add(r.getPlannedOrderQuantity());
                        } else {
                            totalProduce = totalProduce.add(r.getPlannedOrderQuantity());
                        }
                    }
                }
            }
        } else {
            // 未关联 MPS，按 MPS 计划明细汇总独立需求
            log.warn("[executeMrp][planId({}) 未关联 MPS 计划，结果为空]", id);
        }

        // 3. 批量写入结果
        if (CollUtil.isNotEmpty(results)) {
            mrpResultMapper.insertBatch(results);
        }
        // 4. 更新计划状态为已计算
        ErpMrpPlanDO update = new ErpMrpPlanDO();
        update.setId(id);
        update.setStatus(STATUS_CALCULATED);
        update.setTotalDemandCount(totalDemand);
        update.setTotalPurchaseCount(totalPurchase);
        update.setTotalProduceCount(totalProduce);
        mrpPlanMapper.updateById(update);
        log.info("[executeMrp][planId({}) results({}) totalDemand({}) totalPurchase({}) totalProduce({})]",
                id, results.size(), totalDemand, totalPurchase, totalProduce);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmMrpPlan(Long id) {
        ErpMrpPlanDO plan = validateMrpPlan(id);
        // 只有已计算状态才能确认
        if (!ObjUtil.equal(plan.getStatus(), STATUS_CALCULATED)) {
            throw exception(MRP_PLAN_NOT_CONFIRMABLE);
        }
        mrpPlanMapper.updateById(new ErpMrpPlanDO().setId(id)
                .setStatus(STATUS_CONFIRMED));
    }

    @Override
    public ErpMrpPlanDO validateMrpPlan(Long id) {
        ErpMrpPlanDO plan = mrpPlanMapper.selectById(id);
        if (plan == null) {
            throw exception(MRP_PLAN_NOT_EXISTS);
        }
        return plan;
    }

    // ==================== 私有方法 ====================

    private void validateNoUnique(Long id, String no) {
        if (no == null) {
            return;
        }
        ErpMrpPlanDO plan = mrpPlanMapper.selectByNo(no);
        if (plan == null) {
            return;
        }
        if (id == null || !ObjUtil.equal(plan.getId(), id)) {
            throw exception(MRP_PLAN_NO_DUPLICATE, no);
        }
    }

    /**
     * 构造独立需求结果（减去库存得到净需求）
     */
    private ErpMrpResultDO buildIndependentResult(Long planId, Long productId, String productName,
                                                  BigDecimal demandQty, LocalDate deliveryDate) {
        BigDecimal stock = getAvailableStock(productId);
        BigDecimal netDemand = demandQty.subtract(stock);
        if (netDemand.compareTo(BigDecimal.ZERO) < 0) {
            netDemand = BigDecimal.ZERO;
        }
        int orderType = isProduceProduct(productId) ? ORDER_TYPE_PRODUCE : ORDER_TYPE_PURCHASE;
        return ErpMrpResultDO.builder()
                .planId(planId)
                .productId(productId)
                .productName(productName)
                .demandType(DEMAND_TYPE_INDEPENDENT)
                .demandQuantity(demandQty)
                .stockQuantity(stock)
                .netDemand(netDemand)
                .plannedOrderType(orderType)
                .plannedOrderQuantity(netDemand)
                .plannedDeliveryDate(deliveryDate)
                .build();
    }

    /**
     * 递归展开 BOM，计算相关需求
     *
     * @param planId MRP 计划编号
     * @param parentProductId 父件产品编号
     * @param parentQty 父件需求量
     * @param deliveryDate 交付日期
     * @return 相关需求结果列表（扁平）
     */
    private List<ErpMrpResultDO> expandBom(Long planId, Long parentProductId,
                                           BigDecimal parentQty, LocalDate deliveryDate) {
        List<ErpMrpResultDO> results = new ArrayList<>();
        // BFS 展开避免循环引用
        List<BomNode> queue = new ArrayList<>();
        queue.add(new BomNode(parentProductId, parentQty, parentProductId, parentQty));
        int depth = 0;
        int maxDepth = 10;
        while (!queue.isEmpty() && depth < maxDepth) {
            List<BomNode> nextLevel = new ArrayList<>();
            for (BomNode node : queue) {
                List<ErpBomComponent> components = bomProvider.getBomComponents(node.productId);
                if (CollUtil.isEmpty(components)) {
                    continue;
                }
                for (ErpBomComponent comp : components) {
                    if (comp.getProductId() == null || comp.getQuantity() == null) {
                        continue;
                    }
                    BigDecimal childQty = comp.getQuantity().multiply(node.quantity);
                    // 子件结果（基于父件）
                    ErpProductDO product = productService.getProduct(comp.getProductId());
                    String childName = product == null ? null : product.getName();
                    BigDecimal stock = getAvailableStock(comp.getProductId());
                    BigDecimal netDemand = childQty.subtract(stock);
                    if (netDemand.compareTo(BigDecimal.ZERO) < 0) {
                        netDemand = BigDecimal.ZERO;
                    }
                    int orderType = isProduceProduct(comp.getProductId())
                            ? ORDER_TYPE_PRODUCE : ORDER_TYPE_PURCHASE;
                    results.add(ErpMrpResultDO.builder()
                            .planId(planId)
                            .productId(comp.getProductId())
                            .productName(childName)
                            .demandType(DEMAND_TYPE_DEPENDENT)
                            .demandQuantity(childQty)
                            .stockQuantity(stock)
                            .netDemand(netDemand)
                            .plannedOrderType(orderType)
                            .plannedOrderQuantity(netDemand)
                            .plannedDeliveryDate(deliveryDate)
                            .sourceProductId(node.productId)
                            .sourceQuantity(node.quantity)
                            .build());
                    nextLevel.add(new BomNode(comp.getProductId(), childQty,
                            node.productId, node.quantity));
                }
            }
            queue = nextLevel;
            depth++;
        }
        return results;
    }

    /**
     * 判断产品是否为生产件（有 BOM 视为生产，否则采购）
     */
    private boolean isProduceProduct(Long productId) {
        if (productId == null) {
            return false;
        }
        List<ErpBomComponent> components = bomProvider.getBomComponents(productId);
        return CollUtil.isNotEmpty(components);
    }

    /**
     * 获取产品可用库存
     */
    private BigDecimal getAvailableStock(Long productId) {
        if (productId == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal stock = stockService.getStockCount(productId);
        return stock == null ? BigDecimal.ZERO : stock;
    }

    /**
     * BOM 展开节点（用于 BFS）
     */
    private static class BomNode {
        final Long productId;
        final BigDecimal quantity;
        final Long sourceProductId;
        final BigDecimal sourceQuantity;

        BomNode(Long productId, BigDecimal quantity, Long sourceProductId, BigDecimal sourceQuantity) {
            this.productId = productId;
            this.quantity = quantity;
            this.sourceProductId = sourceProductId;
            this.sourceQuantity = sourceQuantity;
        }
    }

}
