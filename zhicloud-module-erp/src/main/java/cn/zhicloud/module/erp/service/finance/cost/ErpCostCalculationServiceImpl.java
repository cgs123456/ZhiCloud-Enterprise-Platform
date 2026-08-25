package cn.zhicloud.module.erp.service.finance.cost;

import cn.zhicloud.module.erp.dal.dataobject.finance.cost.ErpActualCostDO;
import cn.zhicloud.module.erp.service.finance.cost.bom.ErpBomComponent;
import cn.zhicloud.module.erp.service.finance.cost.bom.ErpBomProvider;
import cn.zhicloud.module.erp.dal.dataobject.finance.cost.ErpCostItemDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.cost.ErpCostVarianceDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.cost.ErpStandardCostDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.cost.ErpWorkOrderCostDO;
import cn.zhicloud.module.erp.dal.mysql.finance.cost.ErpActualCostMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.cost.ErpCostItemMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.cost.ErpCostVarianceMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.cost.ErpStandardCostMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.cost.ErpWorkOrderCostMapper;
import cn.zhicloud.module.erp.enums.finance.cost.ErpCostItemTypeEnum;
import cn.zhicloud.module.erp.enums.finance.cost.ErpStandardCostStatusEnum;
import cn.zhicloud.module.erp.enums.finance.cost.ErpVarianceTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.COST_CALCULATION_NO_DATA;

/**
 * ERP 成本核算 Service 实现类
 *
 * <p>实现核心的 BOM 卷积算法、实际成本归集、差异分析。
 *
 * <p><b>卷积算法说明</b>：成品成本 = ∑(下层物料标准成本 × BOM 用量) + 本层加工成本。
 * 由于当前 zhicloud 项目未提供独立 BOM 模块，本实现以 erp_standard_cost 表中已维护的
 * 各产品成本项目记录为输入，按产品维度汇总其所有成本项目的标准成本。当后续接入 BOM
 * 模块时，可在 {@link #calculateStandardCostByConvolution} 中递归遍历 BOM 层级，
 * 将下层物料的标准成本按用量卷积到上层。
 *
 * @author 智云
 */
@Slf4j
@Service
@Validated
public class ErpCostCalculationServiceImpl implements ErpCostCalculationService {

    @Resource
    private ErpStandardCostMapper standardCostMapper;
    @Resource
    private ErpActualCostMapper actualCostMapper;
    @Resource
    private ErpCostVarianceMapper costVarianceMapper;
    @Resource
    private ErpWorkOrderCostMapper workOrderCostMapper;
    @Resource
    private ErpCostItemMapper costItemMapper;
    @Resource
    private ErpBomProvider bomProvider;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<BigDecimal> calculateStandardCostByConvolution(Long productId, String costPeriod) {
        // 1. 获取产品当期生效的标准成本记录（卷积算法的输入）
        LocalDate periodDate = parsePeriodLastDay(costPeriod);
        List<ErpStandardCostDO> standardCosts = standardCostMapper.selectEffectiveListByProduct(productId, periodDate);
        if (standardCosts == null || standardCosts.isEmpty()) {
            throw exception(COST_CALCULATION_NO_DATA);
        }
        // 2. BOM 递归卷积计算：本层标准成本 + ∑(下层物料标准成本 × BOM 用量)
        //    通过 ErpBomProvider SPI 网关获取 BOM 数据（可由 MES 模块实现注入）
        List<BigDecimal> result = new ArrayList<>();
        for (ErpStandardCostDO sc : standardCosts) {
            // 卷积公式：本层标准成本 + ∑(下层物料标准成本 × BOM 用量)
            // 递归遍历 BOM 层级，累加各层子件的标准成本
            BigDecimal convolutedCost = calculateConvolutedCostRecursive(
                    productId, sc.getCostItemId(), periodDate,
                    new HashSet<>(), 0);
            // 如果递归结果为 0（无 BOM 或无子件成本），回退使用本层标准成本
            if (convolutedCost.compareTo(BigDecimal.ZERO) == 0) {
                convolutedCost = sc.getStandardCost() == null ? BigDecimal.ZERO : sc.getStandardCost();
            }
            result.add(convolutedCost);
            // 将计算结果回写为已生效状态
            sc.setStatus(ErpStandardCostStatusEnum.EFFECTIVE.getStatus());
            standardCostMapper.updateById(sc);
        }
        log.info("[calculateStandardCostByConvolution][productId({}) costPeriod({}) 计算出 {} 项标准成本]",
                productId, costPeriod, result.size());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int collectActualCostFromWorkOrders(Long productId, String costPeriod) {
        // 1. 查询产品在该期间的所有工单成本归集记录
        List<ErpWorkOrderCostDO> workOrderCosts = workOrderCostMapper.selectListByProductAndPeriod(productId, costPeriod);
        if (workOrderCosts == null || workOrderCosts.isEmpty()) {
            throw exception(COST_CALCULATION_NO_DATA);
        }
        // 2. 按成本项目分类汇总工单成本（卷积：将工单维度卷积到产品维度）
        BigDecimal totalMaterial = BigDecimal.ZERO;
        BigDecimal totalLabor = BigDecimal.ZERO;
        BigDecimal totalOverhead = BigDecimal.ZERO;
        BigDecimal totalOutsourcing = BigDecimal.ZERO;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        for (ErpWorkOrderCostDO wo : workOrderCosts) {
            totalMaterial = totalMaterial.add(nullToZero(wo.getMaterialCost()));
            totalLabor = totalLabor.add(nullToZero(wo.getLaborCost()));
            totalOverhead = totalOverhead.add(nullToZero(wo.getOverheadCost()));
            totalOutsourcing = totalOutsourcing.add(nullToZero(wo.getOutsourcingCost()));
            totalQuantity = totalQuantity.add(nullToZero(wo.getQuantity()));
        }
        // 3. 获取启用的成本项目列表
        List<ErpCostItemDO> costItems = costItemMapper.selectListByStatus(0);
        int count = 0;
        for (ErpCostItemDO costItem : costItems) {
            BigDecimal actualCost;
            ErpCostItemTypeEnum typeEnum = ErpCostItemTypeEnum.MATERIAL; // default
            for (ErpCostItemTypeEnum e : ErpCostItemTypeEnum.values()) {
                if (e.getType().equals(costItem.getType())) {
                    typeEnum = e;
                    break;
                }
            }
            switch (typeEnum) {
                case MATERIAL:
                    actualCost = totalMaterial;
                    break;
                case LABOR:
                    actualCost = totalLabor;
                    break;
                case OVERHEAD:
                    actualCost = totalOverhead;
                    break;
                case OUTSOURCING:
                    actualCost = totalOutsourcing;
                    break;
                default:
                    continue; // 其他类型不归集
            }
            // 跳过零金额项目
            if (actualCost.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            // 4. 写入或更新实际成本记录（同产品+期间+项目唯一）
            ErpActualCostDO existing = actualCostMapper.selectByProductAndPeriod(productId, costPeriod, costItem.getId());
            ErpActualCostDO actualCostDO;
            if (existing != null) {
                actualCostDO = existing;
            } else {
                actualCostDO = ErpActualCostDO.builder()
                        .productId(productId)
                        .costPeriod(costPeriod)
                        .costItemId(costItem.getId())
                        .build();
            }
            actualCostDO.setActualCost(actualCost);
            actualCostDO.setActualQuantity(totalQuantity);
            if (totalQuantity.compareTo(BigDecimal.ZERO) != 0) {
                actualCostDO.setUnitCost(actualCost.divide(totalQuantity, 4, RoundingMode.HALF_UP));
            }
            if (existing != null) {
                actualCostMapper.updateById(actualCostDO);
            } else {
                actualCostMapper.insert(actualCostDO);
            }
            count++;
        }
        log.info("[collectActualCostFromWorkOrders][productId({}) costPeriod({}) 归集生成 {} 条实际成本记录]",
                productId, costPeriod, count);
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int analyzeVariance(Long productId, String costPeriod) {
        // 1. 获取产品在该期间的实际成本记录
        List<ErpActualCostDO> actualCosts = actualCostMapper.selectListByProductAndPeriod(productId, costPeriod);
        if (actualCosts == null || actualCosts.isEmpty()) {
            throw exception(COST_CALCULATION_NO_DATA);
        }
        // 2. 获取产品在该期间生效的标准成本记录
        LocalDate periodDate = parsePeriodLastDay(costPeriod);
        List<ErpStandardCostDO> standardCosts = standardCostMapper.selectEffectiveListByProduct(productId, periodDate);
        // 3. 逐项对比生成差异记录
        int count = 0;
        for (ErpActualCostDO actual : actualCosts) {
            // 查找对应成本项目的标准成本
            BigDecimal standard = BigDecimal.ZERO;
            if (standardCosts != null) {
                for (ErpStandardCostDO sc : standardCosts) {
                    if (sc.getCostItemId() != null && sc.getCostItemId().equals(actual.getCostItemId())) {
                        standard = sc.getStandardCost() == null ? BigDecimal.ZERO : sc.getStandardCost();
                        break;
                    }
                }
            }
            BigDecimal actualCost = actual.getActualCost() == null ? BigDecimal.ZERO : actual.getActualCost();
            BigDecimal varianceAmount = actualCost.subtract(standard);
            BigDecimal varianceRate = BigDecimal.ZERO;
            if (standard.compareTo(BigDecimal.ZERO) != 0) {
                varianceRate = varianceAmount.divide(standard, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
            }
            Integer varianceType;
            if (varianceAmount.compareTo(BigDecimal.ZERO) < 0) {
                varianceType = ErpVarianceTypeEnum.FAVORABLE.getType();
            } else if (varianceAmount.compareTo(BigDecimal.ZERO) > 0) {
                varianceType = ErpVarianceTypeEnum.UNFAVORABLE.getType();
            } else {
                continue; // 无差异，跳过
            }
            // 4. 写入差异记录
            ErpCostVarianceDO varianceDO = ErpCostVarianceDO.builder()
                    .productId(productId)
                    .costPeriod(costPeriod)
                    .costItemId(actual.getCostItemId())
                    .standardCost(standard)
                    .actualCost(actualCost)
                    .varianceAmount(varianceAmount)
                    .varianceRate(varianceRate)
                    .varianceType(varianceType)
                    .build();
            costVarianceMapper.insert(varianceDO);
            count++;
        }
        log.info("[analyzeVariance][productId({}) costPeriod({}) 生成 {} 条差异记录]",
                productId, costPeriod, count);
        return count;
    }

    /**
     * BOM 递归卷积计算
     *
     * <p>卷积公式：本层标准成本 + ∑(下层物料标准成本 × BOM 用量)
     *
     * <p>防环策略：
     * <ul>
     *   <li>使用 visited Set 记录已访问的产品编号，遇到环则跳过</li>
     *   <li>递归深度上限 MAX_BOM_DEPTH（10 层），超过则停止递归</li>
     * </ul>
     *
     * @param productId   当前产品编号
     * @param costItemId  成本项目编号
     * @param periodDate  生效日期
     * @param visited     已访问的产品编号集合（防环）
     * @param depth       当前递归深度
     * @return 卷积后的标准成本
     */
    private static final int MAX_BOM_DEPTH = 10;

    private BigDecimal calculateConvolutedCostRecursive(Long productId, Long costItemId,
                                                         LocalDate periodDate,
                                                         Set<Long> visited, int depth) {
        // 1. 防环：已访问的产品不再重复计算
        if (visited.contains(productId)) {
            log.warn("[calculateConvolutedCostRecursive][产品 {} 在 BOM 链路中形成环，跳过（visited={}）]",
                    productId, visited);
            return BigDecimal.ZERO;
        }
        // 2. 深度上限保护
        if (depth >= MAX_BOM_DEPTH) {
            log.warn("[calculateConvolutedCostRecursive][产品 {} BOM 递归深度超过 {} 层，停止递归]",
                    productId, MAX_BOM_DEPTH);
            return BigDecimal.ZERO;
        }
        // 3. 获取本层标准成本
        ErpStandardCostDO baseCost = standardCostMapper.selectByProductAndCostItem(productId, costItemId, periodDate);
        BigDecimal base = (baseCost != null && baseCost.getStandardCost() != null)
                ? baseCost.getStandardCost() : BigDecimal.ZERO;
        // 4. 获取 BOM 子件列表
        List<ErpBomComponent> components = bomProvider.getBomComponents(productId);
        if (components == null || components.isEmpty()) {
            // 无子件，直接返回本层成本
            return base;
        }
        // 5. 标记当前产品已访问，递归累加子件成本
        visited.add(productId);
        BigDecimal childrenCost = BigDecimal.ZERO;
        for (ErpBomComponent component : components) {
            if (component.getProductId() == null || component.getQuantity() == null
                    || component.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            // 递归计算子件的卷积成本
            BigDecimal childCost = calculateConvolutedCostRecursive(
                    component.getProductId(), costItemId, periodDate, visited, depth + 1);
            // 子件成本 × BOM 用量
            childrenCost = childrenCost.add(childCost.multiply(component.getQuantity()));
        }
        // 6. 移除当前产品标记（允许兄弟节点再次访问）
        visited.remove(productId);
        // 卷积结果 = 本层成本 + 子件卷积成本合计
        return base.add(childrenCost);
    }

    /**
     * 将成本期间（yyyymm）解析为该月最后一天
     */
    private LocalDate parsePeriodLastDay(String costPeriod) {
        YearMonth ym = YearMonth.parse(costPeriod, DateTimeFormatter.ofPattern("yyyyMM"));
        return ym.atEndOfMonth();
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

}
