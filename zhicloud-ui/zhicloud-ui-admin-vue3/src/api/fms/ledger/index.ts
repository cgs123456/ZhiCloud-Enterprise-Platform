import request from '@/config/axios'
import type {
  FmsLedgerDetailVO,
  FmsLedgerGeneralVO,
  FmsLedgerListReqVO,
  FmsLedgerSubjectVO,
  FmsMultiColumnVO,
  FmsSubjectBalanceVO
} from './types'

/**
 * FMS 语境「账表」API（C6② / ADR 266）
 *
 * FMS/ERP 财务融合：账表的单一真相是 ERP 财务域（账套 → erp_account_book 账簿链 → 已审核凭证
 * → erp_gl_voucher_entry 聚合 + erp_gl_initial_balance 期初），端点在 /erp/gl-ledger。
 * FMS 页面**直连 ERP 端点**，不建第二套表 / 代理层（与会计科目 V194、期初余额 C6③ 先例一致）；
 * 权限串 erp:gl-ledger:{query,export}，打印为纯前端权限 erp:gl-ledger:print（无后端端点）。
 *
 * 一期仅金额式：数量核算 / 辅助核算整链未建（分录层无该两类事实，台账登记），接口相应裁剪。
 */
export const FmsLedgerApi = {
  // 查询明细账行
  getDetailList: async (params: FmsLedgerListReqVO) => {
    return await request.get<FmsLedgerDetailVO[]>({ url: '/erp/gl-ledger/detail/list', params })
  },
  // 导出明细账 Excel
  exportDetail: async (params: FmsLedgerListReqVO) => {
    return await request.download({ url: '/erp/gl-ledger/detail/export-excel', params })
  },

  // 查询总账行
  getGeneralList: async (params: FmsLedgerListReqVO) => {
    return await request.get<FmsLedgerGeneralVO[]>({ url: '/erp/gl-ledger/general/list', params })
  },
  // 导出总账 Excel
  exportGeneral: async (params: FmsLedgerListReqVO) => {
    return await request.download({ url: '/erp/gl-ledger/general/export-excel', params })
  },

  // 查询科目余额表
  getSubjectBalanceList: async (params: FmsLedgerListReqVO) => {
    return await request.get<FmsSubjectBalanceVO[]>({
      url: '/erp/gl-ledger/subject-balance/list',
      params
    })
  },
  // 导出科目余额表 Excel
  exportSubjectBalance: async (params: FmsLedgerListReqVO) => {
    return await request.download({ url: '/erp/gl-ledger/subject-balance/export-excel', params })
  },

  // 查询多栏账（专栏列 + 账簿行）
  getMultiColumn: async (params: FmsLedgerListReqVO) => {
    return await request.get<FmsMultiColumnVO>({ url: '/erp/gl-ledger/multi-column/list', params })
  },
  // 导出多栏账 Excel（动态表头：固定列 + 各子科目列）
  exportMultiColumn: async (params: FmsLedgerListReqVO) => {
    return await request.download({ url: '/erp/gl-ledger/multi-column/export-excel', params })
  },

  // 查询明细账科目树节点（期间内有发生额或期初非零的科目及其祖先，平铺）
  getDetailSubjectList: async (params: FmsLedgerListReqVO) => {
    return await request.get<FmsLedgerSubjectVO[]>({
      url: '/erp/gl-ledger/detail-subjects',
      params
    })
  }
}
