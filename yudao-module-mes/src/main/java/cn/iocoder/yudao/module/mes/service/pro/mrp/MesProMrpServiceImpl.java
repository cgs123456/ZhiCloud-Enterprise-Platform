package cn.iocoder.yudao.module.mes.service.pro.mrp;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.mrp.vo.MesProMrpPlanCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.mrp.vo.MesProMrpPlanPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.wm.materialstock.vo.MesWmMaterialStockListReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdProductBomDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.mrp.MesProMrpPlanDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.mrp.MesProMrpResultDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.materialstock.MesWmMaterialStockDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.mrp.MesProMrpPlanMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.mrp.MesProMrpResultMapper;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderPageReqVO;
import cn.iocoder.yudao.module.mes.enums.md.MesMrpLotSizeRuleEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProMrpPlanStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdProductBomService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import cn.iocoder.yudao.module.mes.service.wm.materialstock.MesWmMaterialStockService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_MRP_PLAN_NOT_DRAFT;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_MRP_PLAN_NOT_EXISTS;

/**
 * MES MRP 物料需求计划 Service 实现类
 *
 * MRP 展算流程：
 * 1. 读取已确认的生产工单作为生产计划
 * 2. 递归展开产品 BOM，累加各层级物料需求
 * 3. 查询当前库存（WMS 库存台账）
 * 4. 净需求 = 需求量 - 库存量（小于 0 则为 0）
 * 5. 生成计划采购订单建议
 *
 * @author 芋道源码
 */
@Service
@Validated
public class MesProMrpServiceImpl implements MesProMrpService {

    /**
     * 默认采购提前期（天）
     */
    private static final int DEFAULT_LEAD_TIME_DAYS = 7;

    @Resource
    private MesProMrpPlanMapper mrpPlanMapper;
    @Resource
    private MesProMrpResultMapper mrpResultMapper;
    @Resource
    private MesProWorkOrderService workOrderService;
    @Resource
    private MesMdProductBomService productBomService;
    @Resource
    private MesWmMaterialStockService materialStockService;
    @Resource
    private MesMdItemService itemService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createMrpPlan(MesProMrpPlanCreateReqVO createReqVO) {
        // 校验编号唯一
        validateMrpPlanNoUnique(null, createReqVO.getPlanNo());
        // 插入
        MesProMrpPlanDO plan = BeanUtils.toBean(createReqVO, MesProMrpPlanDO.class);
        plan.setPlanDate(LocalDateTime.now());
        plan.setStatus(MesProMrpPlanStatusEnum.DRAFT.getStatus());
        mrpPlanMapper.insert(plan);
        return plan.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<MesProMrpResultDO> calculateMrp(Long planId) {
        // 1. 校验存在 + 只有草稿状态才能计算
        MesProMrpPlanDO plan = validateMrpPlanExists(planId);
        if (ObjUtil.notEqual(plan.getStatus(), MesProMrpPlanStatusEnum.DRAFT.getStatus())) {
            throw exception(PRO_MRP_PLAN_NOT_DRAFT);
        }

        // 2. 读取已确认的生产工单作为生产计划
        List<MesProWorkOrderDO> workOrders = getConfirmedWorkOrders();
        if (CollUtil.isEmpty(workOrders)) {
            // 没有生产计划，直接更新状态并返回空结果
            mrpPlanMapper.updateById(new MesProMrpPlanDO().setId(planId)
                    .setStatus(MesProMrpPlanStatusEnum.CALCULATED.getStatus()));
            return new ArrayList<>();
        }

        // 3. 递归展开 BOM，累加各物料需求
        Map<Long, BigDecimal> requirementMap = new HashMap<>();
        for (MesProWorkOrderDO workOrder : workOrders) {
            if (workOrder.getProductId() == null || workOrder.getQuantity() == null) {
                continue;
            }
            expandBomRequirement(workOrder.getProductId(), workOrder.getQuantity(), requirementMap, new HashSet<>());
        }

        // 4. 批量查询物料主数据（用于安全库存/批量规则/提前期）
        Map<Long, MesMdItemDO> itemMap = itemService.getItemMap(requirementMap.keySet());

        // 5. P0-11：按 lowLevelCode 升序处理（保证下层先算，便于层级追溯）
        List<Map.Entry<Long, BigDecimal>> sortedEntries = new ArrayList<>(requirementMap.entrySet());
        sortedEntries.sort(Comparator.comparingInt(e -> {
            MesMdItemDO item = itemMap.get(e.getKey());
            return item == null || item.getLowLevelCode() == null ? Integer.MAX_VALUE : item.getLowLevelCode();
        }));

        // 6. 批量查询所有物料库存（避免N+1）
        List<Long> allItemIds = sortedEntries.stream().map(Map.Entry::getKey).collect(Collectors.toList());
        Map<Long, BigDecimal> stockQtyMap = materialStockService.batchGetStockQuantity(allItemIds);
        LocalDateTime planBaseDate = plan.getPlanDate() != null ? plan.getPlanDate() : LocalDateTime.now();
        List<MesProMrpResultDO> results = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : sortedEntries) {
            Long itemId = entry.getKey();
            BigDecimal requirementQty = entry.getValue();
            MesMdItemDO item = itemMap.get(itemId);
            // 从缓存Map获取库存（避免循环内查询）
            BigDecimal stockQty = stockQtyMap.getOrDefault(itemId, BigDecimal.ZERO);
            // P0-11：净需求 = 需求量 - (库存 - 安全库存)
            BigDecimal safetyStock = (item != null && item.getSafetyStock() != null)
                    ? item.getSafetyStock() : BigDecimal.ZERO;
            BigDecimal netRequirement = requirementQty.subtract(stockQty).add(safetyStock);
            if (netRequirement.compareTo(BigDecimal.ZERO) < 0) {
                netRequirement = BigDecimal.ZERO;
            }
            // P0-11：按批量规则调整计划订单量
            String lotSizeRule = (item != null && item.getLotSizeRule() != null)
                    ? item.getLotSizeRule() : MesMrpLotSizeRuleEnum.LFL.getCode();
            BigDecimal fixedLotSize = (item != null) ? item.getFixedLotSize() : null;
            BigDecimal lotSizeMultiple = (item != null) ? item.getLotSizeMultiple() : null;
            BigDecimal scrapRate = (item != null && item.getScrapRate() != null)
                    ? item.getScrapRate() : BigDecimal.ZERO;
            BigDecimal plannedOrderQty = applyLotSizeRule(netRequirement, lotSizeRule, fixedLotSize, lotSizeMultiple);
            // P0-11：按损耗率放大
            if (scrapRate.compareTo(BigDecimal.ZERO) > 0 && plannedOrderQty.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal factor = BigDecimal.ONE.add(scrapRate.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
                plannedOrderQty = plannedOrderQty.multiply(factor).setScale(4, RoundingMode.HALF_UP);
            }
            // P0-11：按物料提前期倒推计划订单日期
            int leadTimeDays = (item != null && item.getLeadTimeDays() != null && item.getLeadTimeDays() > 0)
                    ? item.getLeadTimeDays() : DEFAULT_LEAD_TIME_DAYS;
            LocalDateTime plannedOrderDate = planBaseDate.minusDays(leadTimeDays);
            MesProMrpResultDO result = MesProMrpResultDO.builder()
                    .planId(planId)
                    .productId(itemId)
                    .requirementQty(requirementQty)
                    .stockQty(stockQty)
                    .netRequirement(netRequirement)
                    .safetyStock(safetyStock)
                    .lotSizeRule(lotSizeRule)
                    .plannedOrderQty(plannedOrderQty)
                    .plannedOrderDate(plannedOrderDate)
                    .build();
            results.add(result);
        }

        // 7. 删除旧结果，插入新结果
        mrpResultMapper.deleteByPlanId(planId);
        if (CollUtil.isNotEmpty(results)) {
            mrpResultMapper.insertBatch(results);
        }

        // 8. 更新计划状态为已计算
        mrpPlanMapper.updateById(new MesProMrpPlanDO().setId(planId)
                .setStatus(MesProMrpPlanStatusEnum.CALCULATED.getStatus()));
        return results;
    }

    /**
     * P0-11：按批量规则计算计划订单量
     *
     * @param netRequirement   净需求
     * @param lotSizeRule      批量规则
     * @param fixedLotSize     固定批量（FOQ 用）
     * @param lotSizeMultiple  批量倍数（MULTIPLES 用）
     * @return 调整后的计划订单量
     */
    private BigDecimal applyLotSizeRule(BigDecimal netRequirement, String lotSizeRule,
                                        BigDecimal fixedLotSize, BigDecimal lotSizeMultiple) {
        if (netRequirement == null || netRequirement.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (MesMrpLotSizeRuleEnum.FOQ.getCode().equals(lotSizeRule)
                && fixedLotSize != null && fixedLotSize.compareTo(BigDecimal.ZERO) > 0) {
            // 固定批量：向上取整到固定批量的整数倍
            BigDecimal multiples = netRequirement.divide(fixedLotSize, 0, RoundingMode.UP);
            return fixedLotSize.multiply(multiples);
        }
        if (MesMrpLotSizeRuleEnum.MULTIPLES.getCode().equals(lotSizeRule)
                && lotSizeMultiple != null && lotSizeMultiple.compareTo(BigDecimal.ZERO) > 0) {
            // 倍数批量：向上取整到倍数的整数倍
            BigDecimal multiples = netRequirement.divide(lotSizeMultiple, 0, RoundingMode.UP);
            return lotSizeMultiple.multiply(multiples);
        }
        // LFL / POQ / 默认：计划订单 = 净需求
        return netRequirement;
    }

    @Override
    public List<MesProMrpResultDO> getMrpResult(Long planId) {
        return mrpResultMapper.selectListByPlanId(planId);
    }

    @Override
    public MesProMrpPlanDO getMrpPlan(Long id) {
        return mrpPlanMapper.selectById(id);
    }

    @Override
    public PageResult<MesProMrpPlanDO> getMrpPlanPage(MesProMrpPlanPageReqVO pageReqVO) {
        return mrpPlanMapper.selectPage(pageReqVO);
    }

    // ==================== 校验方法 ====================

    private MesProMrpPlanDO validateMrpPlanExists(Long id) {
        MesProMrpPlanDO plan = mrpPlanMapper.selectById(id);
        if (plan == null) {
            throw exception(PRO_MRP_PLAN_NOT_EXISTS);
        }
        return plan;
    }

    private void validateMrpPlanNoUnique(Long id, String planNo) {
        if (planNo == null) {
            return;
        }
        MesProMrpPlanDO plan = mrpPlanMapper.selectByPlanNo(planNo);
        if (plan == null) {
            return;
        }
        if (ObjUtil.notEqual(plan.getId(), id)) {
            throw exception(PRO_MRP_PLAN_NOT_EXISTS);
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 读取已确认状态的生产工单
     */
    private List<MesProWorkOrderDO> getConfirmedWorkOrders() {
        MesProWorkOrderPageReqVO pageReqVO = new MesProWorkOrderPageReqVO();
        pageReqVO.setStatus(MesProWorkOrderStatusEnum.CONFIRMED.getStatus());
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        return workOrderService.getWorkOrderPage(pageReqVO).getList();
    }

    /**
     * 递归展开 BOM，累加各物料需求量
     *
     * @param productId 父物料编号
     * @param quantity 父物料需求量
     * @param requirementMap 需求累加 Map
     * @param visited 已访问物料集合（防止 BOM 闭环导致无限递归）
     */
    private void expandBomRequirement(Long productId, BigDecimal quantity,
                                      Map<Long, BigDecimal> requirementMap, Set<Long> visited) {
        if (visited.contains(productId)) {
            return;
        }
        visited.add(productId);
        // 累加当前物料需求
        requirementMap.merge(productId, quantity, BigDecimal::add);
        // 递归展开子物料
        List<MesMdProductBomDO> bomList = productBomService.getProductBomListByItemId(productId);
        if (CollUtil.isEmpty(bomList)) {
            return;
        }
        for (MesMdProductBomDO bom : bomList) {
            if (bom.getBomItemId() == null || bom.getQuantity() == null) {
                continue;
            }
            // 子物料需求量 = 父物料需求量 * BOM 用量比例
            BigDecimal childQty = quantity.multiply(bom.getQuantity());
            expandBomRequirement(bom.getBomItemId(), childQty, requirementMap, new HashSet<>(visited));
        }
    }

    /**
     * 查询物料库存总量
     */
    private BigDecimal getStockQuantity(Long itemId) {
        MesWmMaterialStockListReqVO reqVO = new MesWmMaterialStockListReqVO();
        reqVO.setItemId(itemId);
        List<MesWmMaterialStockDO> stockList = materialStockService.getMaterialStockList(reqVO);
        if (CollUtil.isEmpty(stockList)) {
            return BigDecimal.ZERO;
        }
        return stockList.stream()
                .map(MesWmMaterialStockDO::getQuantity)
                .filter(qty -> qty != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
