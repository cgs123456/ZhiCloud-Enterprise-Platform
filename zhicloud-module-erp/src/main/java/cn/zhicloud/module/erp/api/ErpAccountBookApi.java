package cn.zhicloud.module.erp.api;

import cn.zhicloud.module.erp.controller.admin.finance.vo.accountbook.ErpAccountBookRespVO;

import java.util.List;

/**
 * ERP 账簿查询 API（跨模块契约，位于 erp.api 边界包）
 *
 * <p>FMS/ERP 财务融合（V191，台账 §12.212）后「账套 → 账簿 → 凭证」层级打通：
 * erp_account_book 增加 account_set_id 列，账簿挂到 FMS 账套下。FMS 账套详情页
 * 需要「本账套挂了哪些账簿」的反向可见性，但 Modulith 规则禁止跨模块直接 import
 * service/dal，故在此 api 包发布只读契约（与 {@link ErpMrpExecutorGateway} 同范式）。
 *
 * <p>只读：不暴露任何写操作，挂账套仍由 ERP 侧的账簿维护页完成。
 *
 * @author 智云
 */
public interface ErpAccountBookApi {

    /**
     * 按 FMS 账套查账簿列表（仅启用的）
     *
     * @param accountSetId FMS 账套编号
     * @return 账簿列表，无则空列表
     */
    List<ErpAccountBookRespVO> getAccountBookListByAccountSetId(Long accountSetId);

}
