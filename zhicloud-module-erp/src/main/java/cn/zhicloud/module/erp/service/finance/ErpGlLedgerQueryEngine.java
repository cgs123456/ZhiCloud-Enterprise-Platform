package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpAccountBookDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlAccountDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlInitialBalanceDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlVoucherDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlVoucherEntryDO;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpAccountBookMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlAccountMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlInitialBalanceMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlVoucherEntryMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlVoucherMapper;
import cn.zhicloud.module.erp.enums.finance.ErpGlVoucherStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static cn.zhicloud.module.erp.service.finance.ErpGlLedgerQueryContext.DAY_FORMATTER;
import static cn.zhicloud.module.erp.service.finance.ErpGlLedgerQueryContext.LedgerAccount;
import static cn.zhicloud.module.erp.service.finance.ErpGlLedgerQueryContext.LedgerEntry;
import static cn.zhicloud.module.erp.service.finance.ErpGlLedgerQueryContext.MONTH_FORMATTER;
import static cn.zhicloud.module.erp.service.finance.ErpGlLedgerQueryContext.MonthAmount;
import static cn.zhicloud.module.erp.service.finance.ErpGlLedgerQueryContext.formatMonth;
import static cn.zhicloud.module.erp.service.finance.ErpGlLedgerQueryContext.isZero;
import static cn.zhicloud.module.erp.service.finance.ErpGlLedgerQueryContext.nvl;

/**
 * 账表聚合引擎：上下文构建 + 原始量/子树量汇总 + 范围工具（ADR 266）
 *
 * <p>数据源固定为账套口径唯一真相链（ADR 266 §3 裁定 1）：
 * {@code accountSetId} → {@code erp_account_book} 账簿链 → 已审核（status=20）凭证 →
 * {@code erp_gl_voucher_entry} 聚合；期初取 {@code erp_gl_initial_balance}（C6③ 账套口径）；
 * 科目树取 {@code erp_gl_account}。不读 {@code fms_account_set}（跨模块读表破坏模块边界）。
 *
 * <p>账套无账簿 → 返回 {@code null} 上下文（空结果而非错误）。
 *
 * @author 智云
 */
@Component
class ErpGlLedgerQueryEngine {

    /** 科目级次缺省下限（ADR 266 裁定 7） */
    private static final int DEFAULT_MIN_LEVEL = 1;
    /** 科目级次缺省上限（ADR 266 裁定 7） */
    private static final int DEFAULT_MAX_LEVEL = 1;

    @Resource
    private ErpAccountBookMapper accountBookMapper;
    @Resource
    private ErpGlVoucherMapper voucherMapper;
    @Resource
    private ErpGlVoucherEntryMapper voucherEntryMapper;
    @Resource
    private ErpGlAccountMapper accountMapper;
    @Resource
    private ErpGlInitialBalanceMapper initialBalanceMapper;

    /**
     * 构建查询上下文
     *
     * @param reqVO 查询条件
     * @return 上下文；账套无账簿时为 {@code null}
     */
    ErpGlLedgerQueryContext buildContext(ErpGlLedgerReqVO reqVO) {
        List<Long> bookIds = new ArrayList<>();
        for (ErpAccountBookDO book : accountBookMapper.selectListByAccountSetId(reqVO.getAccountSetId())) {
            if (book.getId() != null) {
                bookIds.add(book.getId());
            }
        }
        if (bookIds.isEmpty()) {
            return null;
        }
        YearMonth startYm = YearMonth.parse(reqVO.getStartMonth(), MONTH_FORMATTER);
        YearMonth endYm = YearMonth.parse(reqVO.getEndMonth(), MONTH_FORMATTER);
        ErpGlLedgerQueryContext ctx = new ErpGlLedgerQueryContext();
        ctx.startDate = startYm.atDay(1);
        ctx.endDate = endYm.atEndOfMonth();
        ctx.yearStart = YearMonth.of(startYm.getYear(), 1).atDay(1);
        ctx.startMonth = reqVO.getStartMonth();
        ctx.endMonth = reqVO.getEndMonth();
        loadAccounts(ctx);
        loadVouchers(ctx, bookIds);
        loadEntries(ctx);
        loadInitialBalances(ctx, reqVO);
        aggregateRaw(ctx);
        aggregateSubtree(ctx);
        return ctx;
    }

    private void loadAccounts(ErpGlLedgerQueryContext ctx) {
        List<ErpGlAccountDO> accounts = new ArrayList<>(accountMapper.selectList(null));
        accounts.sort(Comparator.comparing(ErpGlAccountDO::getCode,
                Comparator.nullsLast(Comparator.naturalOrder())));
        for (ErpGlAccountDO account : accounts) {
            if (account.getId() == null) {
                continue;
            }
            ctx.accountMap.put(account.getId(), account);
            Long parentId = account.getParentId();
            if (parentId != null && parentId != 0L) {
                ctx.childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(account.getId());
            }
        }
    }

    /**
     * 已审核凭证：账簿链 + 记账日期 ≤ 期末
     *
     * <p>不设日期下界——期初余额口径要求把启用点之前的全部前置发生额算进期初，
     * 而启用月需读 {@code fms_account_set}（跨模块读表，ADR 266 §3 裁定 5 不允许）。
     * 大租户数据量列为后续优化项（ADR 266 §8-9）。
     */
    private void loadVouchers(ErpGlLedgerQueryContext ctx, List<Long> bookIds) {
        LambdaQueryWrapperX<ErpGlVoucherDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(ErpGlVoucherDO::getStatus, ErpGlVoucherStatusEnum.APPROVED.getStatus());
        wrapper.in(ErpGlVoucherDO::getAccountBookId, bookIds);
        wrapper.le(ErpGlVoucherDO::getVoucherDate, ctx.endDate);
        for (ErpGlVoucherDO voucher : voucherMapper.selectList(wrapper)) {
            if (voucher.getId() != null && voucher.getVoucherDate() != null) {
                ctx.voucherMap.put(voucher.getId(), voucher);
            }
        }
        ctx.yearToDateWithInitial = true;
        for (ErpGlVoucherDO voucher : ctx.voucherMap.values()) {
            if (voucher.getVoucherDate().isBefore(ctx.yearStart)) {
                ctx.yearToDateWithInitial = false;
                break;
            }
        }
    }

    private void loadEntries(ErpGlLedgerQueryContext ctx) {
        if (ctx.voucherMap.isEmpty()) {
            return;
        }
        List<Long> voucherIds = new ArrayList<>(ctx.voucherMap.keySet());
        for (ErpGlVoucherEntryDO entry : voucherEntryMapper.selectListByVoucherIds(voucherIds)) {
            ErpGlVoucherDO voucher = ctx.voucherMap.get(entry.getVoucherId());
            if (voucher != null) {
                ctx.entries.add(new LedgerEntry(entry, voucher));
            }
        }
    }

    private void loadInitialBalances(ErpGlLedgerQueryContext ctx, ErpGlLedgerReqVO reqVO) {
        for (ErpGlInitialBalanceDO row : initialBalanceMapper.selectListByAccountSetId(reqVO.getAccountSetId())) {
            if (row.getAccountId() != null) {
                ctx.initialBalanceMap.put(row.getAccountId(), row);
            }
        }
    }

    /**
     * 汇总科目级原始量：期初锚点 + 期初之前/本期/当年至今发生额 + 月度发生额
     */
    private void aggregateRaw(ErpGlLedgerQueryContext ctx) {
        for (ErpGlInitialBalanceDO row : ctx.initialBalanceMap.values()) {
            LedgerAccount account = ctx.raw(row.getAccountId());
            account.openingNet = ctx.multiplier(ctx.accountMap.get(row.getAccountId()))
                    .multiply(nvl(row.getOpeningAmount()));
            account.yearDebitInitial = nvl(row.getYearDebitAmount());
            account.yearCreditInitial = nvl(row.getYearCreditAmount());
        }
        for (LedgerEntry item : ctx.entries) {
            ErpGlVoucherEntryDO entry = item.entry();
            LocalDate date = item.date();
            BigDecimal debit = nvl(entry.getDebitAmount());
            BigDecimal credit = nvl(entry.getCreditAmount());
            LedgerAccount account = ctx.raw(entry.getAccountId());
            if (date.isBefore(ctx.startDate)) {
                account.beforePeriodNet = account.beforePeriodNet.add(debit).subtract(credit);
            }
            if (date.isAfter(ctx.endDate)) {
                continue;
            }
            account.totalNet = account.totalNet.add(debit).subtract(credit);
            if (!date.isBefore(ctx.startDate)) {
                account.periodDebit = account.periodDebit.add(debit);
                account.periodCredit = account.periodCredit.add(credit);
                MonthAmount month = account.months.computeIfAbsent(formatMonth(date), k -> new MonthAmount());
                month.debit = month.debit.add(debit);
                month.credit = month.credit.add(credit);
            }
            if (!date.isBefore(ctx.yearStart)) {
                account.yearDebitVoucher = account.yearDebitVoucher.add(debit);
                account.yearCreditVoucher = account.yearCreditVoucher.add(credit);
            }
        }
    }

    /**
     * 自底向上汇总子树量：父级 = 自身原始量 + 全部子级子树量（不做方向二次变换，ADR 266 §3 裁定 5）
     */
    private void aggregateSubtree(ErpGlLedgerQueryContext ctx) {
        List<ErpGlAccountDO> ordered = new ArrayList<>(ctx.accountMap.values());
        ordered.sort(Comparator.comparingInt((ErpGlAccountDO account) -> depthOf(ctx, account.getId())).reversed());
        for (ErpGlAccountDO account : ordered) {
            LedgerAccount subtree = ctx.subtree(account.getId());
            subtree.add(ctx.rawAgg.get(account.getId()));
            for (Long childId : ctx.childrenMap.getOrDefault(account.getId(), Collections.emptyList())) {
                subtree.add(ctx.subtreeAgg.get(childId));
            }
        }
    }

    /**
     * 科目级次：优先科目自带 level，缺失时回落到父链深度
     */
    Integer levelOf(ErpGlLedgerQueryContext ctx, ErpGlAccountDO account) {
        Integer level = account.getLevel();
        if (level != null && level > 0) {
            return level;
        }
        return depthOf(ctx, account.getId());
    }

    /**
     * 父链深度（环保护；不依赖 level 列的连续性）
     */
    int depthOf(ErpGlLedgerQueryContext ctx, Long accountId) {
        Integer cached = ctx.depthMap.get(accountId);
        if (cached != null) {
            return cached;
        }
        int depth = 1;
        Set<Long> visited = new HashSet<>();
        ErpGlAccountDO current = ctx.accountMap.get(accountId);
        while (current != null && current.getParentId() != null && current.getParentId() != 0L
                && visited.add(current.getId())) {
            ErpGlAccountDO parent = ctx.accountMap.get(current.getParentId());
            if (parent == null) {
                break;
            }
            depth++;
            current = parent;
        }
        ctx.depthMap.put(accountId, depth);
        return depth;
    }

    /**
     * 科目（含自身）子树编号集合
     */
    Set<Long> subtreeIds(ErpGlLedgerQueryContext ctx, Long accountId) {
        Set<Long> result = new java.util.LinkedHashSet<>();
        Deque<Long> stack = new ArrayDeque<>();
        stack.push(accountId);
        while (!stack.isEmpty()) {
            Long current = stack.pop();
            if (!result.add(current)) {
                continue; // 环保护：异常数据下避免死循环
            }
            for (Long childId : ctx.childrenMap.getOrDefault(current, Collections.emptyList())) {
                stack.push(childId);
            }
        }
        return result;
    }

    /**
     * 直接子科目（按科目编码升序）
     */
    List<ErpGlAccountDO> childAccounts(ErpGlLedgerQueryContext ctx, ErpGlAccountDO account) {
        List<ErpGlAccountDO> children = new ArrayList<>();
        for (Long childId : ctx.childrenMap.getOrDefault(account.getId(), Collections.emptyList())) {
            ErpGlAccountDO child = ctx.accountMap.get(childId);
            if (child != null) {
                children.add(child);
            }
        }
        return children;
    }

    /**
     * 展开祖先链（保证树连通；父级环由 visited 保护）
     */
    Set<Long> withAncestors(ErpGlLedgerQueryContext ctx, Set<Long> ids) {
        Set<Long> result = new java.util.LinkedHashSet<>();
        for (Long id : ids) {
            Long current = id;
            while (current != null && result.add(current)) {
                ErpGlAccountDO account = ctx.accountMap.get(current);
                Long parentId = account == null ? null : account.getParentId();
                current = parentId == null || parentId == 0L ? null : parentId;
            }
        }
        return result;
    }

    /**
     * 期间内、按（记账日期, 凭证编号, 分录排序, 分录编号）升序的分录
     */
    List<LedgerEntry> entriesInScope(ErpGlLedgerQueryContext ctx, Set<Long> scope) {
        List<LedgerEntry> result = new ArrayList<>();
        for (LedgerEntry item : ctx.entries) {
            if (item.date().isBefore(ctx.startDate) || item.date().isAfter(ctx.endDate)) {
                continue;
            }
            if (!scope.contains(item.entry().getAccountId())) {
                continue;
            }
            result.add(item);
        }
        result.sort(Comparator.comparing(LedgerEntry::date)
                .thenComparing(item -> item.entry().getVoucherId() == null ? 0L : item.entry().getVoucherId())
                .thenComparing(item -> item.entry().getSort() == null ? 0 : item.entry().getSort())
                .thenComparing(item -> item.entry().getId() == null ? 0L : item.entry().getId()));
        return result;
    }

    /**
     * 子树月度发生额（键升序）
     */
    Map<String, MonthAmount> subtreeMonths(ErpGlLedgerQueryContext ctx, ErpGlAccountDO account) {
        Map<String, MonthAmount> result = new TreeMap<>();
        LedgerAccount raw = ctx.rawAgg.get(account.getId());
        if (raw != null) {
            raw.months.forEach((month, amount) -> mergeMonth(result, month, amount));
        }
        for (ErpGlAccountDO child : childAccounts(ctx, account)) {
            subtreeMonths(ctx, child).forEach((month, amount) -> mergeMonth(result, month, amount));
        }
        return result;
    }

    private void mergeMonth(Map<String, MonthAmount> target, String month, MonthAmount amount) {
        target.computeIfAbsent(month, k -> new MonthAmount()).add(amount);
    }

    /**
     * 总账 / 科目余额表的科目域：级次区间 + 科目编码闭区间
     */
    List<ErpGlAccountDO> scopeAccounts(ErpGlLedgerQueryContext ctx, ErpGlLedgerReqVO reqVO) {
        int minLevel = reqVO.getMinLevel() == null ? DEFAULT_MIN_LEVEL : reqVO.getMinLevel();
        int maxLevel = reqVO.getMaxLevel() == null ? DEFAULT_MAX_LEVEL : reqVO.getMaxLevel();
        String startCode = codeOf(ctx, reqVO.getStartSubjectId());
        String endCode = codeOf(ctx, reqVO.getEndSubjectId());
        if (startCode != null && endCode != null && startCode.compareTo(endCode) > 0) {
            return Collections.emptyList(); // 区间两端颠倒 → 空区间（ADR 266 裁定 7）
        }
        List<ErpGlAccountDO> result = new ArrayList<>();
        for (ErpGlAccountDO account : ctx.accountMap.values()) {
            Integer level = levelOf(ctx, account);
            if (level == null || level < minLevel || level > maxLevel) {
                continue;
            }
            if (!inCodeRange(account.getCode(), startCode, endCode)) {
                continue;
            }
            result.add(account);
        }
        return result;
    }

    /**
     * 科目是否存在数据（期初 / 期末 / 本期 / 本年累计任一非零）
     */
    boolean hasData(ErpGlLedgerQueryContext ctx, ErpGlAccountDO account) {
        LedgerAccount subtree = ctx.subtree(account.getId());
        return !isZero(ctx.openingNet(account))
                || !isZero(ctx.endingNet(account))
                || !isZero(subtree.periodDebit) || !isZero(subtree.periodCredit)
                || !isZero(ctx.yearDebit(account)) || !isZero(ctx.yearCredit(account));
    }

    /**
     * 期初行是否带值（三金额任一非零）
     */
    boolean hasInitialValue(ErpGlInitialBalanceDO row) {
        return !isZero(nvl(row.getOpeningAmount()))
                || !isZero(nvl(row.getYearDebitAmount()))
                || !isZero(nvl(row.getYearCreditAmount()));
    }

    /**
     * 边界科目的编码（科目不存在或无编码 → 该端不限）
     */
    String codeOf(ErpGlLedgerQueryContext ctx, Long accountId) {
        if (accountId == null) {
            return null;
        }
        ErpGlAccountDO account = ctx.accountMap.get(accountId);
        return account == null ? null : account.getCode();
    }

    /**
     * 科目编码是否落在闭区间内
     */
    boolean inCodeRange(String code, String startCode, String endCode) {
        if (startCode == null && endCode == null) {
            return true;
        }
        if (code == null) {
            return false;
        }
        if (startCode != null && code.compareTo(startCode) < 0) {
            return false;
        }
        return endCode == null || code.compareTo(endCode) <= 0;
    }

    static String formatDay(LocalDate date) {
        return DAY_FORMATTER.format(date);
    }

}
