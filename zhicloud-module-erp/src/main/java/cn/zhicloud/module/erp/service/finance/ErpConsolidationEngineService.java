package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.module.erp.dal.dataobject.finance.ErpConsolidationWorksheetDO;

import java.util.List;
import java.util.Map;

/**
 * ERP 合并报表自动抵消引擎 Service 接口（P1）
 *
 * <p>基于合并范围（{@link cn.zhicloud.module.erp.dal.dataobject.finance.ErpConsolidationScopeDO}）
 * 与已有合并抵消分录（ErpConsolidationEntry）数据，自动生成各类抵消分录记录到
 * {@link ErpConsolidationWorksheetDO}（合并工作底稿）。
 *
 * <p>说明：本引擎不直接生成 GL 凭证，仅生成抵消分录记录，便于人工审核后入账。
 *
 * @author 智云
 */
public interface ErpConsolidationEngineService {

    /**
     * 生成投资权益抵消分录
     *
     * <p>抵消逻辑：
     * <ul>
     *   <li>借：子公司所有者权益（按持股比例）</li>
     *   <li>贷：长期股权投资（母公司对子公司投资）</li>
     *   <li>差额：少数股东权益</li>
     * </ul>
     *
     * @param parentId     母公司编号
     * @param subId        子公司编号
     * @param period       合并周期（yyyyMM）
     * @return 生成的工作底稿列表
     */
    List<ErpConsolidationWorksheetDO> generateInvestmentEquityElimination(Long parentId, Long subId, String period);

    /**
     * 生成内部应收应付抵消分录
     *
     * <p>抵消逻辑：
     * <ul>
     *   <li>借：内部应付（子公司对母公司）</li>
     *   <li>贷：内部应收（母公司对子公司）</li>
     * </ul>
     *
     * @param parentId     母公司编号
     * @param subId        子公司编号
     * @param period       合并周期（yyyyMM）
     * @return 生成的工作底稿列表
     */
    List<ErpConsolidationWorksheetDO> generateIntercompanyArApElimination(Long parentId, Long subId, String period);

    /**
     * 生成内部销售成本抵消分录
     *
     * <p>抵消逻辑：
     * <ul>
     *   <li>借：销售收入（母公司对子公司销售）</li>
     *   <li>贷：销售成本（按毛利率计算未实现利润部分）</li>
     * </ul>
     *
     * @param parentId     母公司编号
     * @param subId        子公司编号
     * @param period       合并周期（yyyyMM）
     * @return 生成的工作底稿列表
     */
    List<ErpConsolidationWorksheetDO> generateIntercompanySaleCogsElimination(Long parentId, Long subId, String period);

    /**
     * 生成内部固定资产交易抵消分录
     *
     * <p>抵消逻辑：
     * <ul>
     *   <li>借：固定资产原价（未实现利润部分）</li>
     *   <li>贷：累计折旧</li>
     * </ul>
     *
     * @param parentId     母公司编号
     * @param subId        子公司编号
     * @param period       合并周期（yyyyMM）
     * @return 生成的工作底稿列表
     */
    List<ErpConsolidationWorksheetDO> generateIntercompanyFaElimination(Long parentId, Long subId, String period);

    /**
     * 批量生成所有合并范围的抵消分录
     *
     * <p>遍历所有启用的合并范围，依次生成 4 类抵消分录。
     *
     * @param period 合并周期（yyyyMM）
     * @return 生成的工作底稿列表
     */
    List<ErpConsolidationWorksheetDO> generateAllEliminations(String period);

    /**
     * 生成合并资产负债表
     *
     * <p>基于母公司 + 各子公司报表数据，扣除工作底稿中已审核的抵消分录金额，
     * 输出按科目分类的合并资产负债表数据。
     *
     * @param period 合并周期（yyyyMM）
     * @return 按科目编码分组的合并金额（key=科目编码，value=合并后余额）
     */
    Map<String, java.math.BigDecimal> generateConsolidatedBalanceSheet(String period);

    /**
     * 生成合并利润表
     *
     * <p>基于母公司 + 各子公司利润表数据，扣除工作底稿中已审核的抵消分录金额，
     * 输出按科目分类的合并利润表数据。
     *
     * @param period 合并周期（yyyyMM）
     * @return 按科目编码分组的合并金额（key=科目编码，value=合并后余额）
     */
    Map<String, java.math.BigDecimal> generateConsolidatedIncomeStatement(String period);

}
