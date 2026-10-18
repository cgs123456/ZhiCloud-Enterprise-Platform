import request from '@/config/axios'

/**
 * FMS 语境「期初余额」API（C6③ / ADR 265）
 *
 * FMS/ERP 财务融合：期初余额的单一真相是 ERP 的 erp_gl_initial_balance（账套口径），
 * 端点在 /erp/gl-initial-balance。FMS 页面**直连 ERP 端点**，不建第二套表/代理层
 * （与会计科目 V194 先例一致）；权限串 erp:gl-initial-balance:{query,update,import,export}。
 *
 * 一期仅金额式：数量核算 / 辅助核算整链未建（台账登记），类型相应裁剪。
 */

/** FMS 语境期初余额行 VO（字段与 ERP ErpGlInitialBalanceRespVO 一致，独立类型名避免跨模块耦合） */
export interface FmsInitialBalanceVO {
  subjectId: number // 科目编号
  subjectCode: string // 科目编码
  subjectName: string // 科目名称
  parentId?: number // 上级科目编号
  type?: number // 科目类型（10 资产 / 20 负债 / 30 权益 / 40 收入 / 50 费用 / 60 共同）
  typeName?: string // 科目类型名称
  balanceDirection?: number // 余额方向（10 借 / 20 贷 / 30 双向）
  balanceDirectionName?: string // 余额方向名称
  isLeaf?: boolean // 是否末级科目
  openingAmount: number // 期初余额
  yearDebitAmount: number // 本年累计借方发生额
  yearCreditAmount: number // 本年累计贷方发生额
  yearOpeningAmount: number // 年初余额（派生只读）
  profitLossAmount: number // 实际损益发生额（损益类科目）
}

/** FMS 语境期初余额保存行 VO（不含 yearOpeningAmount：派生只读，不入库不提交） */
export interface FmsInitialBalanceUpdateVO {
  subjectId: number // 科目编号
  openingAmount: number // 期初余额
  yearDebitAmount: number // 本年累计借方发生额
  yearCreditAmount: number // 本年累计贷方发生额
  profitLossAmount: number // 实际损益发生额
}

/** FMS 试算平衡信息 */
export interface FmsTrialBalanceVO {
  openingDebitAmount: number // 期初借方金额
  openingCreditAmount: number // 期初贷方金额
  openingDifferenceAmount: number // 期初差额
  yearDebitAmount: number // 本年累计借方金额
  yearCreditAmount: number // 本年累计贷方金额
  yearDifferenceAmount: number // 本年累计差额
  balanced: boolean // 是否平衡
}

// FMS 语境期初余额 API（直连 ERP /erp/gl-initial-balance/*）
export const FmsInitialBalanceApi = {
  // 查询账套某科目类别的期初余额列表
  getInitialBalanceList: async (accountSetId: number, subjectType: number) => {
    return await request.get<FmsInitialBalanceVO[]>({
      url: '/erp/gl-initial-balance/list',
      params: { accountSetId, subjectType }
    })
  },

  // 保存账套期初余额（差分 upsert）
  saveInitialBalance: async (accountSetId: number, balances: FmsInitialBalanceUpdateVO[]) => {
    return await request.put({
      url: '/erp/gl-initial-balance/save',
      data: { accountSetId, balances }
    })
  },

  // 查询试算平衡结果
  getTrialBalance: async (accountSetId: number) => {
    return await request.get<FmsTrialBalanceVO>({
      url: '/erp/gl-initial-balance/trial-balance',
      params: { accountSetId }
    })
  },

  // 导出账套期初余额 Excel
  exportInitialBalance: async (accountSetId: number) => {
    return await request.download({
      url: '/erp/gl-initial-balance/export-excel',
      params: { accountSetId }
    })
  },

  // 下载账套期初余额导入模板
  getInitialBalanceImportTemplate: async (accountSetId: number) => {
    return await request.download({
      url: '/erp/gl-initial-balance/get-import-template',
      params: { accountSetId }
    })
  },

  // 导入账套期初余额（返回实际写入行数）
  importInitialBalance: async (accountSetId: number, file: File) => {
    const data = new FormData()
    data.append('accountSetId', String(accountSetId))
    data.append('file', file)
    return await request.post<number>({
      url: '/erp/gl-initial-balance/import',
      data,
      headersType: 'multipart/form-data'
    })
  }
}
