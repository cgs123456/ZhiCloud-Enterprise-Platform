package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.module.erp.dal.dataobject.finance.ErpPeriodDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpPeriodCloseDO;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlVoucherMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpPeriodCloseMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpPeriodMapper;
import cn.zhicloud.module.erp.enums.finance.ErpPeriodCloseTypeEnum;
import cn.zhicloud.module.erp.enums.finance.ErpPeriodStatusEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.PERIOD_CLOSE_FAIL_EXISTS_UNPOSTED_VOUCHER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ErpPeriodServiceImpl} 关账守卫的单元测试（§12.145）
 *
 * <p>只测本次补全的那一段：月末检查/损益结转/业务未审核单据都通过后，
 * 期间内若仍有未过账（草稿/已反审核）GL 凭证，关账必须被拒；凭证全部已审核则放行。
 * 未过账 = 期间凭证总数 − APPROVED 数。
 *
 * @author 智云
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ErpPeriodServiceImplCloseGuardTest {

    @Mock
    private ErpPeriodMapper periodMapper;
    @Mock
    private ErpPeriodCloseMapper periodCloseMapper;
    @Mock
    private ErpGlVoucherMapper glVoucherMapper;

    @InjectMocks
    private ErpPeriodServiceImpl periodService;

    private static final Long PERIOD_ID = 1L;

    private void stubReachedVoucherGuard() {
        when(periodMapper.selectById(PERIOD_ID)).thenReturn(
                ErpPeriodDO.builder().id(PERIOD_ID).code("202610")
                        .status(ErpPeriodStatusEnum.OPEN.getStatus()).build());
        when(periodCloseMapper.existsSuccess(eq(PERIOD_ID), eq(ErpPeriodCloseTypeEnum.MONTH_CHECK))).thenReturn(true);
        when(periodCloseMapper.existsSuccess(eq(PERIOD_ID), eq(ErpPeriodCloseTypeEnum.PROFIT_LOSS_TRANSFER))).thenReturn(true);
        // 月末检查记录为 null → 业务未审核数守卫按"无数据"跳过，直达凭证守卫
        when(periodCloseMapper.selectByPeriodAndType(eq(PERIOD_ID),
                eq(ErpPeriodCloseTypeEnum.MONTH_CHECK.getType()))).thenReturn((ErpPeriodCloseDO) null);
    }

    @Test
    public void testCloseBlockedByUnpostedVoucher() {
        stubReachedVoucherGuard();
        // 期间内 2 张凭证，仅 1 张 APPROVED → 1 张草稿未过账
        when(glVoucherMapper.selectCountByPeriodId(PERIOD_ID)).thenReturn(2L);
        when(glVoucherMapper.selectCountByPeriodIdAndStatus(eq(PERIOD_ID), any())).thenReturn(1L);

        assertServiceException(() -> periodService.closePeriod(PERIOD_ID),
                PERIOD_CLOSE_FAIL_EXISTS_UNPOSTED_VOUCHER, "202610", 1);
        verify(periodMapper, never()).updateById(any(ErpPeriodDO.class));
    }

    @Test
    public void testCloseAllowedWhenAllVouchersApproved() {
        stubReachedVoucherGuard();
        when(glVoucherMapper.selectCountByPeriodId(PERIOD_ID)).thenReturn(2L);
        when(glVoucherMapper.selectCountByPeriodIdAndStatus(eq(PERIOD_ID), any())).thenReturn(2L);

        periodService.closePeriod(PERIOD_ID);

        verify(periodMapper).updateById(any(ErpPeriodDO.class));
    }

    @Test
    public void testCloseAllowedWhenNoVouchers() {
        stubReachedVoucherGuard();
        when(glVoucherMapper.selectCountByPeriodId(PERIOD_ID)).thenReturn(0L);
        when(glVoucherMapper.selectCountByPeriodIdAndStatus(eq(PERIOD_ID), any())).thenReturn(0L);

        periodService.closePeriod(PERIOD_ID);

        verify(periodMapper).updateById(any(ErpPeriodDO.class));
    }

}
