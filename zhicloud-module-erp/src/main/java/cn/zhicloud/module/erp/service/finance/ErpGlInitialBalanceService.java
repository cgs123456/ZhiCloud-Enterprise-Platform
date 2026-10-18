package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.module.erp.controller.admin.finance.vo.glinitialbalance.ErpGlInitialBalanceExcelVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glinitialbalance.ErpGlInitialBalanceRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glinitialbalance.ErpGlInitialBalanceSaveReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glinitialbalance.ErpTrialBalanceRespVO;

import java.util.List;

/**
 * ERP 期初余额 Service 接口（账套口径，C6③ / ADR 265）
 *
 * <p>一期仅金额式：数量核算 / 辅助核算整链未建（登记）；期初余额未被三大报表 /
 * C6② 账表消费（后续接力）。
 *
 * @author 智云
 */
public interface ErpGlInitialBalanceService {

    /**
     * 获得账套某科目类别的期初余额列表（含该类别的全科目树与派生年初余额）
     *
     * @param accountSetId 账套编号（必填，账套口径）
     * @param subjectType 科目类型（10 资产 / 20 负债 / 30 权益 / 40 收入 / 50 费用 / 60 共同）
     * @return 科目行列表（未录行金额为 0）
     */
    List<ErpGlInitialBalanceRespVO> getInitialBalanceList(Long accountSetId, Integer subjectType);

    /**
     * 保存账套期初余额（差分 upsert）
     *
     * <p>护栏：账套经账簿链存在已审核凭证时拒绝（ADR 265 §3 裁定 6）；
     * 校验科目重复 / 存在 / 末级 / 损益类年初余额为 0。
     *
     * @param saveReqVO 保存请求
     */
    void saveInitialBalances(ErpGlInitialBalanceSaveReqVO saveReqVO);

    /**
     * 试算平衡（期初差与累计差绝对值均 < 0.005 视为平衡）
     *
     * @param accountSetId 账套编号
     * @return 试算平衡结果
     */
    ErpTrialBalanceRespVO getTrialBalance(Long accountSetId);

    /**
     * 导出行：账套所属租户的全部末级科目 × 四金额列（无行时金额为 0）
     *
     * @param accountSetId 账套编号
     * @return Excel 行列表
     */
    List<ErpGlInitialBalanceExcelVO> getInitialBalanceExcelList(Long accountSetId);

    /**
     * 导入模板行：全部末级科目（金额留空）
     *
     * @param accountSetId 账套编号（对齐端点契约保留；科目为租户级口径）
     * @return Excel 行列表
     */
    List<ErpGlInitialBalanceExcelVO> getImportTemplateList(Long accountSetId);

    /**
     * 导入账套期初余额
     *
     * <p>行口径：四金额全空的行跳过（模板未动过）；任一金额非空的行按 null=0 参与
     * upsert（显式填 0 = 清零）。整表校验失败整批回滚。
     *
     * @param accountSetId 账套编号
     * @param rows Excel 行列表
     * @return 实际 upsert 行数
     */
    Integer importInitialBalances(Long accountSetId, List<ErpGlInitialBalanceExcelVO> rows);

}
