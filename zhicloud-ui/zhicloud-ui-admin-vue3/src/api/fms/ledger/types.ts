/**
 * FMS 语境「账表」类型定义（C6② / ADR 266）
 *
 * 字段与 ERP 端点 VO 一致（独立类型名避免跨模块耦合）：账表是纯查询域，一期金额式四族
 * （明细账 / 总账 / 科目余额表 / 多栏账）；数量与辅助核算四族因分录层无该两类事实而整族
 * 不建（旧契约的 debitQuantity / creditQuantity / 辅助组合节点类型 2 均不恢复）。
 */

/** 账表查询入参（四族共用；明细账与多栏账用 subjectId，总账与余额表用级次与科目编码区间） */
export interface FmsLedgerListReqVO {
  accountSetId: number // FMS 账套编号
  startMonth: string // 会计期间起始月（yyyy-MM）
  endMonth: string // 会计期间结束月（yyyy-MM）
  subjectId?: number // 科目编号（明细账 / 多栏账，按科目子树聚合）
  startSubjectId?: number // 起始科目编号（按科目编码闭区间）
  endSubjectId?: number // 结束科目编号（按科目编码闭区间）
  minLevel?: number // 科目级次下限（含）
  maxLevel?: number // 科目级次上限（含）
}

/** 明细账 / 多栏账行（行类型：1 期初 / 2 明细 / 3 本期合计 / 4 本年累计） */
export interface FmsLedgerDetailVO {
  rowType?: number // 行类型
  entryId?: number // 分录编号（仅明细行）
  subjectId?: number // 科目编号
  subjectCode?: string // 科目编码
  subjectName?: string // 科目名称
  period?: string // 会计期间（yyyy-MM）
  accountDate?: string // 记账日期（yyyy-MM-dd，仅明细行）
  voucherId?: number // 凭证编号（仅明细行）
  voucherNumber?: string // 凭证字号（仅明细行）
  digest?: string // 摘要（汇总行为口径文案）
  debitAmount?: number // 借方金额
  creditAmount?: number // 贷方金额
  balanceDirection?: string // 余额方向：借 / 贷
  balance?: number // 余额绝对值
  columnAmounts?: Record<string, number> // 多栏账专栏金额（键=直接子科目编号，自然方向净额）
}

/** 总账行（行类型：1 期初 / 2 月度合计 / 3 本年累计） */
export interface FmsLedgerGeneralVO {
  rowType?: number // 行类型
  subjectId?: number // 科目编号
  subjectCode?: string // 科目编码
  subjectName?: string // 科目名称
  period?: string // 会计期间（yyyy-MM）
  digest?: string // 摘要（期初余额 / 本月合计 / 本年累计）
  debitAmount?: number // 借方发生额
  creditAmount?: number // 贷方发生额
  balanceDirection?: string // 余额方向：借 / 贷
  balance?: number // 余额绝对值
}

/** 科目余额行（树；nodeType 恒为 1 科目——金额式一期无辅助组合节点） */
export interface FmsSubjectBalanceVO {
  nodeKey?: string // 树节点键（row:科目编号）
  nodeType?: number // 节点类型：1 科目
  subjectId?: number // 科目编号
  subjectCode?: string // 科目编码
  subjectName?: string // 科目名称
  level?: number // 科目级次
  openingDebitAmount?: number // 期初借方余额
  openingCreditAmount?: number // 期初贷方余额
  periodDebitAmount?: number // 本期借方发生额
  periodCreditAmount?: number // 本期贷方发生额
  yearDebitAmount?: number // 本年累计借方发生额
  yearCreditAmount?: number // 本年累计贷方发生额
  endingDebitAmount?: number // 期末借方余额
  endingCreditAmount?: number // 期末贷方余额
  children?: FmsSubjectBalanceVO[] // 子科目余额行
}

/** 多栏账专栏列（所选科目的直接子科目；balanceDirection 为 ERP 值域 10 借 / 20 贷 / 30 双向） */
export interface FmsLedgerColumnVO {
  subjectId?: number // 子科目编号
  subjectCode?: string // 子科目编码
  subjectName?: string // 子科目名称
  balanceDirection?: number // 余额方向（10 借方 / 20 贷方 / 30 双向）
}

/** 多栏账结果（列 + 行） */
export interface FmsMultiColumnVO {
  columns: FmsLedgerColumnVO[] // 专栏列
  rows: FmsLedgerDetailVO[] // 账簿行
}

/** 明细账科目树节点（平铺，前端 handleTree 建树） */
export interface FmsLedgerSubjectVO {
  id?: number // 科目编号
  parentId?: number // 父级科目编号
  code?: string // 科目编码
  name?: string // 科目名称
  level?: number // 科目级次
  balanceDirection?: number // 余额方向（10 借方 / 20 贷方 / 30 双向）
  status?: number // 状态（0 启用 / 1 停用）
  isLeaf?: boolean // 是否末级科目
  sort?: number // 排序
}
