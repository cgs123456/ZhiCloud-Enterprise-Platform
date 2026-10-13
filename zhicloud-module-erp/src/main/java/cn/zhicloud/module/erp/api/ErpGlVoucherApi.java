package cn.zhicloud.module.erp.api;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glvoucher.ErpGlVoucherPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glvoucher.ErpGlVoucherRespVO;

/**
 * ERP 会计凭证查询 API（跨模块契约，位于 erp.api 边界包）
 *
 * <p>V193（台账 §12.213）：FMS/ERP 财务融合后，FMS 账套详情页需要「本账套的凭证流水」。
 * 凭证只到 account_book_id，账簿再挂账套，故按账套查凭证由 ERP 侧解析账簿集合后查询
 * （单一来源是账簿，不在凭证落冗余列）。Modulith 规则禁止跨模块直接 import service/dal，
 * 故在此 api 包发布只读契约（与 {@link ErpAccountBookApi} 同范式）。
 *
 * <p>只读：不暴露任何写操作；凭证的创建/审核仍走 ERP 侧既有接口。
 *
 * @author 智云
 */
public interface ErpGlVoucherApi {

    /**
     * 按 FMS 账套分页查询凭证流水
     *
     * @param pageReqVO 查询条件（accountSetId 必填）
     * @return 凭证分页结果
     */
    PageResult<ErpGlVoucherRespVO> getGlVoucherPageByAccountSetId(ErpGlVoucherPageReqVO pageReqVO);

}
