package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlAccountDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlInitialBalanceDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlVoucherDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlVoucherEntryDO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 账表单次查询的聚合上下文（账套口径，ADR 266）
 *
 * <p>纯数据载体 + 派生余额口径，不做 I/O：构建由 {@link ErpGlLedgerQueryEngine} 负责，
 * 行模型由 {@link ErpGlLedgerRowAssembler} 负责，服务门面 {@link ErpGlLedgerServiceImpl} 只做编排。
 *
 * <p>余额一律在「借正贷负净额」空间计算（ADR 266 §3 裁定 3）：
 * <ul>
 *   <li>锚点 {@code anchor = s(科目) × opening_amount}——期初按科目自然方向录入，故需方向系数折算
 *       （借向 10 / 双向 30 取 +1，贷向 20 取 -1，双向按借方口径归集，ADR 266 §8-4）；</li>
 *   <li>期初净额 = 锚点 + 期初之前的发生净额；期末净额 = 锚点 + 期末之前的发生净额；</li>
 *   <li>本年累计 = 凭证侧当年至今发生额 +（δ=1 时）期初行的本年累计发生额。</li>
 * </ul>
 *
 * @author 智云
 */
class ErpGlLedgerQueryContext {

    static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final BigDecimal MULTIPLIER_POSITIVE = BigDecimal.ONE;
    private static final BigDecimal MULTIPLIER_NEGATIVE = new BigDecimal("-1");
    /** 余额方向：贷方（ERP 值域） */
    private static final int DIRECTION_CREDIT = 20;

    /** 查询期间首日 */
    LocalDate startDate;
    /** 查询期间末日 */
    LocalDate endDate;
    /** 查询年度 1 月 1 日（本年累计窗口下界） */
    LocalDate yearStart;
    /** 查询期间起始月（yyyy-MM） */
    String startMonth;
    /** 查询期间结束月（yyyy-MM） */
    String endMonth;
    /** δ：本年累计是否叠加期初行的本年累计发生额（启用年证据口径，ADR 266 裁定 4） */
    boolean yearToDateWithInitial;

    /** 科目表（按科目编码升序，页面据此建树） */
    final Map<Long, ErpGlAccountDO> accountMap = new LinkedHashMap<>();
    /** 父科目编号 → 直接子科目编号 */
    final Map<Long, List<Long>> childrenMap = new HashMap<>();
    /** 科目编号 → 父链深度（level 缺失时的回落口径） */
    final Map<Long, Integer> depthMap = new HashMap<>();
    /** 科目级原始量 */
    final Map<Long, LedgerAccount> rawAgg = new HashMap<>();
    /** 科目级子树量（自身 + 全部后代） */
    final Map<Long, LedgerAccount> subtreeAgg = new HashMap<>();
    /** 账套期初余额（科目编号 → 行） */
    final Map<Long, ErpGlInitialBalanceDO> initialBalanceMap = new HashMap<>();
    /** 已审核凭证（编号 → 凭证） */
    final Map<Long, ErpGlVoucherDO> voucherMap = new LinkedHashMap<>();
    /** 期间相关分录（已挂凭证） */
    final List<LedgerEntry> entries = new ArrayList<>();

    LedgerAccount raw(Long accountId) {
        return rawAgg.computeIfAbsent(accountId, k -> new LedgerAccount());
    }

    LedgerAccount subtree(Long accountId) {
        return subtreeAgg.computeIfAbsent(accountId, k -> new LedgerAccount());
    }

    /**
     * 方向系数：贷向取 -1；借向(10)、双向(30) 与空值取 +1（双向按借方口径归集，ADR 266 §8-4）
     */
    BigDecimal multiplier(ErpGlAccountDO account) {
        if (account == null || account.getBalanceDirection() == null) {
            return MULTIPLIER_POSITIVE;
        }
        return account.getBalanceDirection() == DIRECTION_CREDIT
                ? MULTIPLIER_NEGATIVE : MULTIPLIER_POSITIVE;
    }

    /**
     * 期初余额净额 = 子树期初锚点净额（各自按方向折算后累加）+ 子树期初之前的发生净额
     *
     * <p>方向折算只在叶子发生一次，父级只做加法——父级方向不参与二次变换（ADR 266 §3 裁定 5）。
     */
    BigDecimal openingNet(ErpGlAccountDO account) {
        LedgerAccount subtree = subtree(account.getId());
        return subtree.openingNet.add(subtree.beforePeriodNet);
    }

    /**
     * 期末余额净额（含查询期间内的发生额）
     */
    BigDecimal endingNet(ErpGlAccountDO account) {
        LedgerAccount subtree = subtree(account.getId());
        return subtree.openingNet.add(subtree.totalNet);
    }

    /**
     * 本年累计借方发生额
     */
    BigDecimal yearDebit(ErpGlAccountDO account) {
        LedgerAccount subtree = subtree(account.getId());
        return yearToDateWithInitial
                ? subtree.yearDebitVoucher.add(subtree.yearDebitInitial) : subtree.yearDebitVoucher;
    }

    /**
     * 本年累计贷方发生额
     */
    BigDecimal yearCredit(ErpGlAccountDO account) {
        LedgerAccount subtree = subtree(account.getId());
        return yearToDateWithInitial
                ? subtree.yearCreditVoucher.add(subtree.yearCreditInitial) : subtree.yearCreditVoucher;
    }

    /**
     * 分录 + 所属凭证（避免行装配阶段逐行回查凭证）
     */
    static class LedgerEntry {

        private final ErpGlVoucherEntryDO entry;
        private final ErpGlVoucherDO voucher;

        LedgerEntry(ErpGlVoucherEntryDO entry, ErpGlVoucherDO voucher) {
            this.entry = entry;
            this.voucher = voucher;
        }

        ErpGlVoucherEntryDO entry() {
            return entry;
        }

        ErpGlVoucherDO voucher() {
            return voucher;
        }

        LocalDate date() {
            return voucher.getVoucherDate();
        }
    }

    /**
     * 月度发生额（借贷原始两列）
     */
    static class MonthAmount {

        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;

        void add(MonthAmount other) {
            debit = debit.add(other.debit);
            credit = credit.add(other.credit);
        }
    }

    /**
     * 科目量（原始量与子树量共用结构；只做累加，不做方向变换）
     */
    static class LedgerAccount {

        /** 期初锚点净额（期初按科目自然方向录入，此处已按方向折算到借正贷负空间） */
        BigDecimal openingNet = BigDecimal.ZERO;
        /** 期初行的本年累计借方发生额 */
        BigDecimal yearDebitInitial = BigDecimal.ZERO;
        /** 期初行的本年累计贷方发生额 */
        BigDecimal yearCreditInitial = BigDecimal.ZERO;
        /** 期初之前（凭证日期 &lt; 期初首日）发生净额 */
        BigDecimal beforePeriodNet = BigDecimal.ZERO;
        /** 期末之前（凭证日期 ≤ 期末末日）发生净额 */
        BigDecimal totalNet = BigDecimal.ZERO;
        /** 本期借方发生额（原始列） */
        BigDecimal periodDebit = BigDecimal.ZERO;
        /** 本期贷方发生额（原始列） */
        BigDecimal periodCredit = BigDecimal.ZERO;
        /** 凭证侧当年至今借方发生额 */
        BigDecimal yearDebitVoucher = BigDecimal.ZERO;
        /** 凭证侧当年至今贷方发生额 */
        BigDecimal yearCreditVoucher = BigDecimal.ZERO;
        /** 月度发生额（键 = yyyy-MM，仅落在查询期间内） */
        final Map<String, MonthAmount> months = new TreeMap<>();

        void add(LedgerAccount other) {
            if (other == null) {
                return;
            }
            openingNet = openingNet.add(other.openingNet);
            yearDebitInitial = yearDebitInitial.add(other.yearDebitInitial);
            yearCreditInitial = yearCreditInitial.add(other.yearCreditInitial);
            beforePeriodNet = beforePeriodNet.add(other.beforePeriodNet);
            totalNet = totalNet.add(other.totalNet);
            periodDebit = periodDebit.add(other.periodDebit);
            periodCredit = periodCredit.add(other.periodCredit);
            yearDebitVoucher = yearDebitVoucher.add(other.yearDebitVoucher);
            yearCreditVoucher = yearCreditVoucher.add(other.yearCreditVoucher);
            other.months.forEach((month, amount) ->
                    months.computeIfAbsent(month, k -> new MonthAmount()).add(amount));
        }

        BigDecimal periodNet() {
            return periodDebit.subtract(periodCredit);
        }
    }

    static BigDecimal nvl(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    static boolean isZero(BigDecimal amount) {
        return amount == null || amount.signum() == 0;
    }

    static String formatMonth(LocalDate date) {
        return MONTH_FORMATTER.format(date);
    }

    static BigDecimal debitPart(BigDecimal net) {
        return net.signum() < 0 ? BigDecimal.ZERO : net;
    }

    static BigDecimal creditPart(BigDecimal net) {
        return net.signum() < 0 ? net.negate() : BigDecimal.ZERO;
    }

}
