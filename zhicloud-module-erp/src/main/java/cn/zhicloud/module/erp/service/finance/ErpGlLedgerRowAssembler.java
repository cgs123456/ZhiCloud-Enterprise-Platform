package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerColumnVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerDetailExcelVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerDetailRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerGeneralExcelVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerGeneralRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerMultiColumnExcelDataVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerMultiColumnRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerSubjectBalanceExcelVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerSubjectBalanceRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerSubjectRespVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlAccountDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlInitialBalanceDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlVoucherEntryDO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.zhicloud.module.erp.service.finance.ErpGlLedgerQueryContext.LedgerAccount;
import static cn.zhicloud.module.erp.service.finance.ErpGlLedgerQueryContext.LedgerEntry;
import static cn.zhicloud.module.erp.service.finance.ErpGlLedgerQueryContext.MonthAmount;
import static cn.zhicloud.module.erp.service.finance.ErpGlLedgerQueryContext.debitPart;
import static cn.zhicloud.module.erp.service.finance.ErpGlLedgerQueryContext.creditPart;
import static cn.zhicloud.module.erp.service.finance.ErpGlLedgerQueryContext.formatMonth;
import static cn.zhicloud.module.erp.service.finance.ErpGlLedgerQueryContext.nvl;

/**
 * 账表行装配器：四族行模型 + Excel 数据（ADR 266 §3 裁定 6、§4）
 *
 * <ul>
 *   <li>明细账：期初行(rowType=1) → 明细行(rowType=2，逐笔滚动余额) → 本期合计行(3) → 本年累计行(4)；</li>
 *   <li>总账：每科目 期初行(1) → 各月合计行(2，仅有发生额的月份) → 本年累计行(3)；</li>
 *   <li>科目余额表：每科目一行（nodeKey=row:科目编号, nodeType=1）+ children 树，仅含有数据科目及其祖先；</li>
 *   <li>多栏账：列为所选科目的直接子科目；行为期初行 + 明细行 + 本期合计行，专栏金额挂 {@code columnAmounts}。</li>
 * </ul>
 * 展示按符号拆借贷：净额 ≥ 0 计入借方列且方向「借」，&lt; 0 计入贷方列且方向「贷」。
 *
 * @author 智云
 */
@Component
class ErpGlLedgerRowAssembler {

    /** 摘要文案：期初余额 */
    static final String DIGEST_OPENING = "期初余额";
    /** 摘要文案：本月合计（总账月度行） */
    static final String DIGEST_MONTH_TOTAL = "本月合计";
    /** 摘要文案：本期合计 */
    static final String DIGEST_PERIOD_TOTAL = "本期合计";
    /** 摘要文案：本年累计 */
    static final String DIGEST_YEAR_TOTAL = "本年累计";

    /** 余额方向展示：借 */
    static final String DIRECTION_LABEL_DEBIT = "借";
    /** 余额方向展示：贷 */
    static final String DIRECTION_LABEL_CREDIT = "贷";

    /** 科目余额表节点类型：科目（金额式一期无辅助组合节点） */
    private static final int BALANCE_NODE_TYPE_SUBJECT = 1;

    private final ErpGlLedgerQueryEngine engine;

    ErpGlLedgerRowAssembler(ErpGlLedgerQueryEngine engine) {
        this.engine = engine;
    }

    // ==================== 明细账 ====================

    List<ErpGlLedgerDetailRespVO> detailRows(ErpGlLedgerQueryContext ctx, ErpGlAccountDO target) {
        List<ErpGlLedgerDetailRespVO> rows = new ArrayList<>();
        BigDecimal running = ctx.openingNet(target);
        rows.add(detailSummaryRow(target, ErpGlLedgerService.ROW_TYPE_OPENING, ctx.startMonth, DIGEST_OPENING,
                BigDecimal.ZERO, BigDecimal.ZERO, running));
        LedgerAccount subtree = ctx.subtree(target.getId());
        for (LedgerEntry item : engine.entriesInScope(ctx, engine.subtreeIds(ctx, target.getId()))) {
            ErpGlVoucherEntryDO entry = item.entry();
            BigDecimal debit = nvl(entry.getDebitAmount());
            BigDecimal credit = nvl(entry.getCreditAmount());
            running = running.add(debit).subtract(credit);
            ErpGlLedgerDetailRespVO row = new ErpGlLedgerDetailRespVO();
            row.setRowType(ErpGlLedgerService.ROW_TYPE_ENTRY);
            row.setEntryId(entry.getId());
            row.setSubjectId(entry.getAccountId());
            ErpGlAccountDO account = ctx.accountMap.get(entry.getAccountId());
            if (account != null) {
                row.setSubjectCode(account.getCode());
                row.setSubjectName(account.getName());
            } else {
                // 科目已删除：回落到分录冗余的编码与名称，保证账簿可读
                row.setSubjectCode(entry.getAccountCode());
                row.setSubjectName(entry.getAccountName());
            }
            row.setPeriod(formatMonth(item.date()));
            row.setAccountDate(item.date());
            row.setVoucherId(item.voucher().getId());
            row.setVoucherNumber(item.voucher().getVoucherNo());
            row.setDigest(entry.getSummary());
            row.setDebitAmount(debit);
            row.setCreditAmount(credit);
            fillBalance(row, running);
            rows.add(row);
        }
        rows.add(detailSummaryRow(target, ErpGlLedgerService.ROW_TYPE_PERIOD_TOTAL, ctx.endMonth,
                DIGEST_PERIOD_TOTAL, subtree.periodDebit, subtree.periodCredit, ctx.endingNet(target)));
        rows.add(detailSummaryRow(target, ErpGlLedgerService.ROW_TYPE_YEAR_TOTAL, ctx.endMonth,
                DIGEST_YEAR_TOTAL, ctx.yearDebit(target), ctx.yearCredit(target), ctx.endingNet(target)));
        return rows;
    }

    List<ErpGlLedgerDetailExcelVO> detailExcelList(ErpGlLedgerQueryContext ctx, ErpGlAccountDO target) {
        List<ErpGlLedgerDetailExcelVO> list = new ArrayList<>();
        for (ErpGlLedgerDetailRespVO row : detailRows(ctx, target)) {
            ErpGlLedgerDetailExcelVO vo = new ErpGlLedgerDetailExcelVO();
            vo.setAccountDate(row.getAccountDate() == null ? ""
                    : ErpGlLedgerQueryEngine.formatDay(row.getAccountDate()));
            vo.setVoucherNumber(row.getVoucherNumber() == null ? "" : row.getVoucherNumber());
            vo.setDigest(row.getDigest() == null ? "" : row.getDigest());
            vo.setDebitAmount(row.getDebitAmount());
            vo.setCreditAmount(row.getCreditAmount());
            vo.setBalanceDirection(row.getBalanceDirection());
            vo.setBalance(row.getBalance());
            list.add(vo);
        }
        return list;
    }

    // ==================== 总账 ====================

    List<ErpGlLedgerGeneralRespVO> generalRows(ErpGlLedgerQueryContext ctx, ErpGlLedgerReqVO reqVO) {
        List<ErpGlLedgerGeneralRespVO> rows = new ArrayList<>();
        for (ErpGlAccountDO account : engine.scopeAccounts(ctx, reqVO)) {
            if (!engine.hasData(ctx, account)) {
                continue;
            }
            BigDecimal running = ctx.openingNet(account);
            rows.add(generalRow(account, ErpGlLedgerService.ROW_TYPE_OPENING, ctx.startMonth, DIGEST_OPENING,
                    BigDecimal.ZERO, BigDecimal.ZERO, running));
            for (Map.Entry<String, MonthAmount> month : engine.subtreeMonths(ctx, account).entrySet()) {
                if (ErpGlLedgerQueryContext.isZero(month.getValue().debit)
                        && ErpGlLedgerQueryContext.isZero(month.getValue().credit)) {
                    continue;
                }
                running = running.add(month.getValue().debit).subtract(month.getValue().credit);
                rows.add(generalRow(account, ErpGlLedgerService.ROW_TYPE_ENTRY, month.getKey(),
                        DIGEST_MONTH_TOTAL, month.getValue().debit, month.getValue().credit, running));
            }
            rows.add(generalRow(account, ErpGlLedgerService.ROW_TYPE_YEAR_TOTAL, ctx.endMonth,
                    DIGEST_YEAR_TOTAL, ctx.yearDebit(account), ctx.yearCredit(account),
                    ctx.endingNet(account)));
        }
        return rows;
    }

    List<ErpGlLedgerGeneralExcelVO> generalExcelList(ErpGlLedgerQueryContext ctx, ErpGlLedgerReqVO reqVO) {
        List<ErpGlLedgerGeneralExcelVO> list = new ArrayList<>();
        for (ErpGlLedgerGeneralRespVO row : generalRows(ctx, reqVO)) {
            ErpGlLedgerGeneralExcelVO vo = new ErpGlLedgerGeneralExcelVO();
            vo.setSubjectCode(row.getSubjectCode());
            vo.setSubjectName(row.getSubjectName());
            vo.setPeriod(row.getPeriod());
            vo.setDigest(row.getDigest());
            vo.setDebitAmount(row.getDebitAmount());
            vo.setCreditAmount(row.getCreditAmount());
            vo.setBalanceDirection(row.getBalanceDirection());
            vo.setBalance(row.getBalance());
            list.add(vo);
        }
        return list;
    }

    // ==================== 科目余额表 ====================

    List<ErpGlLedgerSubjectBalanceRespVO> subjectBalanceTree(ErpGlLedgerQueryContext ctx,
                                                             ErpGlLedgerReqVO reqVO) {
        Set<Long> dataIds = new HashSet<>();
        for (ErpGlAccountDO account : ctx.accountMap.values()) {
            if (engine.hasData(ctx, account)) {
                dataIds.add(account.getId());
            }
        }
        Set<Long> keep = engine.withAncestors(ctx, dataIds);
        int minLevel = reqVO.getMinLevel() == null ? 1 : reqVO.getMinLevel();
        int maxLevel = reqVO.getMaxLevel() == null ? 1 : reqVO.getMaxLevel();
        String startCode = engine.codeOf(ctx, reqVO.getStartSubjectId());
        String endCode = engine.codeOf(ctx, reqVO.getEndSubjectId());
        if (startCode != null && endCode != null && startCode.compareTo(endCode) > 0) {
            return Collections.emptyList(); // 区间两端颠倒 → 空区间
        }
        Map<Long, ErpGlLedgerSubjectBalanceRespVO> nodes = new LinkedHashMap<>();
        for (ErpGlAccountDO account : ctx.accountMap.values()) {
            if (!keep.contains(account.getId())) {
                continue;
            }
            Integer level = engine.levelOf(ctx, account);
            if (level == null || level < minLevel || level > maxLevel) {
                continue;
            }
            if (!engine.inCodeRange(account.getCode(), startCode, endCode)) {
                continue;
            }
            nodes.put(account.getId(), balanceNode(ctx, account, level));
        }
        Map<Long, List<Long>> childIds = new LinkedHashMap<>();
        List<Long> roots = new ArrayList<>();
        for (Long accountId : nodes.keySet()) {
            Long parentId = ctx.accountMap.get(accountId).getParentId();
            if (parentId != null && nodes.containsKey(parentId)) {
                childIds.computeIfAbsent(parentId, k -> new ArrayList<>()).add(accountId);
            } else {
                roots.add(accountId);
            }
        }
        for (Map.Entry<Long, List<Long>> item : childIds.entrySet()) {
            ErpGlLedgerSubjectBalanceRespVO parent = nodes.get(item.getKey());
            List<ErpGlLedgerSubjectBalanceRespVO> children = new ArrayList<>();
            for (Long childId : item.getValue()) {
                children.add(nodes.get(childId));
            }
            parent.setChildren(children);
        }
        List<ErpGlLedgerSubjectBalanceRespVO> result = new ArrayList<>();
        for (Long rootId : roots) {
            result.add(nodes.get(rootId));
        }
        return result;
    }

    List<ErpGlLedgerSubjectBalanceExcelVO> subjectBalanceExcelList(List<ErpGlLedgerSubjectBalanceRespVO> nodes) {
        List<ErpGlLedgerSubjectBalanceExcelVO> list = new ArrayList<>();
        flattenBalance(nodes, list);
        return list;
    }

    private void flattenBalance(List<ErpGlLedgerSubjectBalanceRespVO> nodes,
                                List<ErpGlLedgerSubjectBalanceExcelVO> target) {
        for (ErpGlLedgerSubjectBalanceRespVO node : nodes) {
            ErpGlLedgerSubjectBalanceExcelVO vo = new ErpGlLedgerSubjectBalanceExcelVO();
            vo.setLevel(node.getLevel());
            vo.setSubjectCode(node.getSubjectCode());
            vo.setSubjectName(node.getSubjectName());
            vo.setOpeningDebitAmount(node.getOpeningDebitAmount());
            vo.setOpeningCreditAmount(node.getOpeningCreditAmount());
            vo.setPeriodDebitAmount(node.getPeriodDebitAmount());
            vo.setPeriodCreditAmount(node.getPeriodCreditAmount());
            vo.setYearDebitAmount(node.getYearDebitAmount());
            vo.setYearCreditAmount(node.getYearCreditAmount());
            vo.setEndingDebitAmount(node.getEndingDebitAmount());
            vo.setEndingCreditAmount(node.getEndingCreditAmount());
            target.add(vo);
            if (node.getChildren() != null) {
                flattenBalance(node.getChildren(), target);
            }
        }
    }

    // ==================== 多栏账 ====================

    ErpGlLedgerMultiColumnRespVO multiColumn(ErpGlLedgerQueryContext ctx, ErpGlAccountDO target) {
        ErpGlLedgerMultiColumnRespVO result = new ErpGlLedgerMultiColumnRespVO();
        for (ErpGlAccountDO child : engine.childAccounts(ctx, target)) {
            ErpGlLedgerColumnVO column = new ErpGlLedgerColumnVO();
            column.setSubjectId(child.getId());
            column.setSubjectCode(child.getCode());
            column.setSubjectName(child.getName());
            column.setBalanceDirection(child.getBalanceDirection());
            result.getColumns().add(column);
        }
        result.setRows(multiColumnRows(ctx, target));
        return result;
    }

    /**
     * 多栏账行 = 明细账行 + 专栏金额（期初行、明细行、本年累计行不带专栏）
     */
    private List<ErpGlLedgerDetailRespVO> multiColumnRows(ErpGlLedgerQueryContext ctx, ErpGlAccountDO target) {
        List<ErpGlLedgerDetailRespVO> rows = detailRows(ctx, target);
        List<ErpGlAccountDO> children = engine.childAccounts(ctx, target);
        if (children.isEmpty()) {
            return rows;
        }
        Map<Long, LedgerEntry> entryById = new HashMap<>();
        for (LedgerEntry item : engine.entriesInScope(ctx, engine.subtreeIds(ctx, target.getId()))) {
            if (item.entry().getId() != null) {
                entryById.put(item.entry().getId(), item);
            }
        }
        for (ErpGlLedgerDetailRespVO row : rows) {
            Map<Long, BigDecimal> columnAmounts = new HashMap<>();
            if (row.getRowType() == ErpGlLedgerService.ROW_TYPE_ENTRY && row.getEntryId() != null) {
                LedgerEntry item = entryById.get(row.getEntryId());
                if (item != null) {
                    for (ErpGlAccountDO child : children) {
                        if (child.getId().equals(item.entry().getAccountId())) {
                            columnAmounts.put(child.getId(), ctx.multiplier(child)
                                    .multiply(nvl(item.entry().getDebitAmount())
                                            .subtract(nvl(item.entry().getCreditAmount()))));
                        }
                    }
                }
            } else if (row.getRowType() == ErpGlLedgerService.ROW_TYPE_OPENING) {
                for (ErpGlAccountDO child : children) {
                    columnAmounts.put(child.getId(),
                            ctx.multiplier(child).multiply(ctx.openingNet(child)));
                }
            } else if (row.getRowType() == ErpGlLedgerService.ROW_TYPE_PERIOD_TOTAL) {
                for (ErpGlAccountDO child : children) {
                    columnAmounts.put(child.getId(),
                            ctx.multiplier(child).multiply(ctx.subtree(child.getId()).periodNet()));
                }
            }
            row.setColumnAmounts(columnAmounts.isEmpty() ? null : columnAmounts);
        }
        return rows;
    }

    /**
     * 多栏账导出数据（动态表头：固定 7 列 + 各子科目列）
     */
    ErpGlLedgerMultiColumnExcelDataVO multiColumnExcelData(ErpGlLedgerMultiColumnRespVO ledger) {
        ErpGlLedgerMultiColumnExcelDataVO data = new ErpGlLedgerMultiColumnExcelDataVO();
        List<List<String>> head = new ArrayList<>();
        for (String title : new String[]{"日期", "凭证字号", "摘要", "借方", "贷方", "方向", "余额"}) {
            head.add(new ArrayList<>(Collections.singletonList(title)));
        }
        for (ErpGlLedgerColumnVO column : ledger.getColumns()) {
            head.add(new ArrayList<>(Collections.singletonList(
                    column.getSubjectCode() + "/" + column.getSubjectName())));
        }
        data.setHead(head);
        for (ErpGlLedgerDetailRespVO row : ledger.getRows()) {
            List<Object> line = new ArrayList<>();
            line.add(row.getAccountDate() == null ? "" : ErpGlLedgerQueryEngine.formatDay(row.getAccountDate()));
            line.add(row.getVoucherNumber() == null ? "" : row.getVoucherNumber());
            line.add(row.getDigest() == null ? "" : row.getDigest());
            line.add(row.getDebitAmount());
            line.add(row.getCreditAmount());
            line.add(row.getBalanceDirection());
            line.add(row.getBalance());
            for (ErpGlLedgerColumnVO column : ledger.getColumns()) {
                BigDecimal amount = row.getColumnAmounts() == null ? null
                        : row.getColumnAmounts().get(column.getSubjectId());
                line.add(amount == null ? "" : amount);
            }
            data.getRows().add(line);
        }
        return data;
    }

    // ==================== 明细账科目树 ====================

    /**
     * 期间内有发生额或期初非零的科目及其祖先（平铺，页面 handleTree 建树）
     */
    List<ErpGlLedgerSubjectRespVO> detailSubjects(ErpGlLedgerQueryContext ctx) {
        Set<Long> active = new HashSet<>();
        for (LedgerEntry item : ctx.entries) {
            LocalDate date = item.date();
            if (item.entry().getAccountId() != null
                    && !date.isBefore(ctx.startDate) && !date.isAfter(ctx.endDate)) {
                active.add(item.entry().getAccountId());
            }
        }
        for (ErpGlInitialBalanceDO row : ctx.initialBalanceMap.values()) {
            if (row.getAccountId() != null && engine.hasInitialValue(row)) {
                active.add(row.getAccountId());
            }
        }
        Set<Long> keep = engine.withAncestors(ctx, active);
        List<ErpGlLedgerSubjectRespVO> list = new ArrayList<>();
        // ctx.accountMap 已按科目编码升序（页面据此建树，编码序即树序）
        for (ErpGlAccountDO account : ctx.accountMap.values()) {
            if (!keep.contains(account.getId())) {
                continue;
            }
            ErpGlLedgerSubjectRespVO vo = new ErpGlLedgerSubjectRespVO();
            vo.setId(account.getId());
            vo.setParentId(account.getParentId());
            vo.setCode(account.getCode());
            vo.setName(account.getName());
            vo.setLevel(engine.levelOf(ctx, account));
            vo.setBalanceDirection(account.getBalanceDirection());
            vo.setStatus(account.getStatus());
            vo.setIsLeaf(account.getIsLeaf());
            vo.setSort(account.getSort());
            list.add(vo);
        }
        return list;
    }

    // ==================== 行构造 ====================

    private ErpGlLedgerDetailRespVO detailSummaryRow(ErpGlAccountDO account, int rowType, String period,
                                                     String digest, BigDecimal debit, BigDecimal credit,
                                                     BigDecimal net) {
        ErpGlLedgerDetailRespVO row = new ErpGlLedgerDetailRespVO();
        row.setRowType(rowType);
        row.setSubjectId(account.getId());
        row.setSubjectCode(account.getCode());
        row.setSubjectName(account.getName());
        row.setPeriod(period);
        row.setDigest(digest);
        row.setDebitAmount(debit);
        row.setCreditAmount(credit);
        fillBalance(row, net);
        return row;
    }

    private ErpGlLedgerGeneralRespVO generalRow(ErpGlAccountDO account, int rowType, String period,
                                                String digest, BigDecimal debit, BigDecimal credit,
                                                BigDecimal net) {
        ErpGlLedgerGeneralRespVO row = new ErpGlLedgerGeneralRespVO();
        row.setRowType(rowType);
        row.setSubjectId(account.getId());
        row.setSubjectCode(account.getCode());
        row.setSubjectName(account.getName());
        row.setPeriod(period);
        row.setDigest(digest);
        row.setDebitAmount(debit);
        row.setCreditAmount(credit);
        fillBalance(row, net);
        return row;
    }

    private ErpGlLedgerSubjectBalanceRespVO balanceNode(ErpGlLedgerQueryContext ctx, ErpGlAccountDO account,
                                                        Integer level) {
        BigDecimal openingNet = ctx.openingNet(account);
        BigDecimal endingNet = ctx.endingNet(account);
        LedgerAccount subtree = ctx.subtree(account.getId());
        ErpGlLedgerSubjectBalanceRespVO node = new ErpGlLedgerSubjectBalanceRespVO();
        node.setNodeKey("row:" + account.getId());
        node.setNodeType(BALANCE_NODE_TYPE_SUBJECT);
        node.setSubjectId(account.getId());
        node.setSubjectCode(account.getCode());
        node.setSubjectName(account.getName());
        node.setLevel(level);
        node.setOpeningDebitAmount(debitPart(openingNet));
        node.setOpeningCreditAmount(creditPart(openingNet));
        node.setPeriodDebitAmount(subtree.periodDebit);
        node.setPeriodCreditAmount(subtree.periodCredit);
        node.setYearDebitAmount(ctx.yearDebit(account));
        node.setYearCreditAmount(ctx.yearCredit(account));
        node.setEndingDebitAmount(debitPart(endingNet));
        node.setEndingCreditAmount(creditPart(endingNet));
        return node;
    }

    private void fillBalance(ErpGlLedgerDetailRespVO row, BigDecimal net) {
        if (net.signum() < 0) {
            row.setBalanceDirection(DIRECTION_LABEL_CREDIT);
            row.setBalance(net.negate());
            return;
        }
        row.setBalanceDirection(DIRECTION_LABEL_DEBIT);
        row.setBalance(net);
    }

    private void fillBalance(ErpGlLedgerGeneralRespVO row, BigDecimal net) {
        if (net.signum() < 0) {
            row.setBalanceDirection(DIRECTION_LABEL_CREDIT);
            row.setBalance(net.negate());
            return;
        }
        row.setBalanceDirection(DIRECTION_LABEL_DEBIT);
        row.setBalance(net);
    }

}
