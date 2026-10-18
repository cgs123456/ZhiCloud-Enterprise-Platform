package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlInitialBalanceDO;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlInitialBalanceMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * C6③（ADR 265）：ERP 期初余额账套口径 H2 真表对齐测试。
 *
 * <p>钉住三件事：
 * ① MyBatis-Plus 按 DO 生成的全列 SELECT/INSERT/UPDATE 在 H2 上可执行（V209 DDL 与 DO 列对齐）；
 * ② V209 唯一键 {@code uk_erp_gl_initial_balance_tenant_set_account} 真的生效
 * （mock 永远证明不了唯一键存在，而它是 upsert 幂等 / 并发防重的基础）；
 * ③ {@code selectListByAccountSetId} 走真 SQL 且按账套隔离。
 *
 * <p>不覆盖什么：H2 无 MySQL 的 {@code ON UPDATE CURRENT_TIMESTAMP} 语义；
 * 金额以 compareTo 断言（H2 decimal(20,4) 读取标度可能与写入标度不同）。
 *
 * @author 智云
 */
public class ErpGlInitialBalanceH2TableTest extends BaseDbUnitTest {

    @Resource
    private ErpGlInitialBalanceMapper initialBalanceMapper;

    private ErpGlInitialBalanceDO balance(Long accountSetId, Long accountId, String opening) {
        return ErpGlInitialBalanceDO.builder()
                .accountSetId(accountSetId)
                .accountId(accountId)
                .openingAmount(new BigDecimal(opening))
                .yearDebitAmount(BigDecimal.ZERO)
                .yearCreditAmount(BigDecimal.ZERO)
                .profitLossAmount(BigDecimal.ZERO)
                .build();
    }

    @Test
    public void test_insertAndFullColumnRoundTrip() {
        ErpGlInitialBalanceDO inserted = balance(1L, 1001L, "1000.0000");
        initialBalanceMapper.insert(inserted);
        assertNotNull(inserted.getId(), "IDENTITY 主键必须回填");

        ErpGlInitialBalanceDO found = initialBalanceMapper.selectById(inserted.getId());
        assertNotNull(found);
        assertEquals(1L, found.getAccountSetId());
        assertEquals(1001L, found.getAccountId());
        assertEquals(0, new BigDecimal("1000.0000").compareTo(found.getOpeningAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(found.getYearDebitAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(found.getYearCreditAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(found.getProfitLossAmount()));
    }

    @Test
    public void test_selectListByAccountSetId_isolatesAccountSets() {
        initialBalanceMapper.insert(balance(1L, 1001L, "100.0000"));
        initialBalanceMapper.insert(balance(1L, 1002L, "200.0000"));
        initialBalanceMapper.insert(balance(2L, 1001L, "999.0000"));

        List<ErpGlInitialBalanceDO> rows = initialBalanceMapper.selectListByAccountSetId(1L);
        assertEquals(2, rows.size(), "账套过滤必须只返回本账套行");
        assertTrue(rows.stream().allMatch(r -> r.getAccountSetId() == 1L));
    }

    @Test
    public void test_uniqueKey_tenantSetAccount_isEnforced() {
        initialBalanceMapper.insert(balance(1L, 1001L, "100.0000"));
        // 同租户同账套同科目二次插入必须被唯一键挡下——V209 核心约束
        assertThrows(DuplicateKeyException.class,
                () -> initialBalanceMapper.insert(balance(1L, 1001L, "500.0000")));
        // 不同账套同科目、同账套不同科目都必须放行
        initialBalanceMapper.insert(balance(2L, 1001L, "100.0000"));
        initialBalanceMapper.insert(balance(1L, 1002L, "100.0000"));
    }

    @Test
    public void test_updateById_writesAmounts() {
        ErpGlInitialBalanceDO inserted = balance(1L, 1001L, "100.0000");
        initialBalanceMapper.insert(inserted);

        inserted.setOpeningAmount(new BigDecimal("250.5000"));
        inserted.setYearCreditAmount(new BigDecimal("50.5000"));
        assertEquals(1, initialBalanceMapper.updateById(inserted));

        ErpGlInitialBalanceDO after = initialBalanceMapper.selectById(inserted.getId());
        assertEquals(0, new BigDecimal("250.5000").compareTo(after.getOpeningAmount()));
        assertEquals(0, new BigDecimal("50.5000").compareTo(after.getYearCreditAmount()));
    }

}
