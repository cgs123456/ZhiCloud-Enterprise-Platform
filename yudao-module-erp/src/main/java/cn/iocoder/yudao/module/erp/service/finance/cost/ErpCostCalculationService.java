package cn.iocoder.yudao.module.erp.service.finance.cost;

import java.math.BigDecimal;
import java.util.List;

/**
 * ERP 成本核算 Service 接口
 *
 * <p>提供基于 BOM 卷积算法的产品成本核算能力，包括：
 * <ul>
 *   <li>标准成本卷积计算：按 BOM 层级递归计算成品的标准成本</li>
 *   <li>实际成本归集：将工单成本按产品+期间归集为实际成本</li>
 *   <li>差异分析：对比标准成本与实际成本，生成差异记录</li>
 * </ul>
 *
 * @author 芋道源码
 */
public interface ErpCostCalculationService {

    /**
     * 计算产品在某期间的标准成本（BOM 卷积算法）
     *
     * <p>卷积算法：从最底层的采购件开始，逐层向上计算半成品、成品的标准成本。
     * 每一层成本 = ∑(下层物料标准成本 × BOM 用量) + 本层加工成本（人工 + 制造费用 + 外协）
     *
     * @param productId 产品 ID
     * @param costPeriod 成本期间（yyyymm）
     * @return 该产品各成本项目的标准成本列表（已写入 erp_standard_cost）
     */
    List<BigDecimal> calculateStandardCostByConvolution(Long productId, String costPeriod);

    /**
     * 归集工单成本到实际成本
     *
     * <p>将同一产品 + 同一期间的所有工单成本按成本项目分类汇总：
     * <ul>
     *   <li>材料成本 → MATERIAL（10）</li>
     *   <li>人工成本 → LABOR（20）</li>
     *   <li>制造费用 → OVERHEAD（30）</li>
     *   <li>外协成本 → OUTSOURCING（40）</li>
     * </ul>
     *
     * @param productId 产品 ID
     * @param costPeriod 成本期间
     * @return 归集生成的实际成本记录数
     */
    int collectActualCostFromWorkOrders(Long productId, String costPeriod);

    /**
     * 执行差异分析
     *
     * <p>对指定产品 + 期间，逐项对比标准成本与实际成本：
     * <ul>
     *   <li>差异金额 = 实际成本 - 标准成本</li>
     *   <li>差异率 = 差异金额 / 标准成本 × 100%</li>
     *   <li>差异类型：&lt; 0 有利差异，&gt; 0 不利差异</li>
     * </ul>
     * 结果写入 erp_cost_variance 表。
     *
     * @param productId 产品 ID
     * @param costPeriod 成本期间
     * @return 生成的差异记录数
     */
    int analyzeVariance(Long productId, String costPeriod);

}
