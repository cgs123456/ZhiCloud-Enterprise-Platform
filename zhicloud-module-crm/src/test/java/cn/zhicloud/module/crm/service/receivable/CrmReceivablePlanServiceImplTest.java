package cn.zhicloud.module.crm.service.receivable;

import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.module.crm.dal.dataobject.receivable.CrmReceivablePlanDO;
import cn.zhicloud.module.crm.dal.mysql.receivable.CrmReceivablePlanMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * {@link CrmReceivablePlanServiceImpl} 的回款计划抢占边界测试。
 */
@ExtendWith(MockitoExtension.class)
public class CrmReceivablePlanServiceImplTest {

    @Mock
    private CrmReceivablePlanMapper receivablePlanMapper;

    private CrmReceivablePlanServiceImpl receivablePlanService;

    @BeforeEach
    void setUp() {
        receivablePlanService = new CrmReceivablePlanServiceImpl();
        ReflectionTestUtils.setField(receivablePlanService, "receivablePlanMapper", receivablePlanMapper);
    }

    @Test
    void updateReceivablePlanReceivableId_shouldFailWhenPlanWasClaimedConcurrently() {
        CrmReceivablePlanDO plan = CrmReceivablePlanDO.builder()
                .id(1L)
                .contractId(10L)
                .price(new BigDecimal("10.00"))
                .version(0)
                .build();
        when(receivablePlanMapper.selectById(1L)).thenReturn(plan);
        when(receivablePlanMapper.claimReceivableId(1L, 100L)).thenReturn(0);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> receivablePlanService.updateReceivablePlanReceivableId(1L, 100L));
        assertEquals(1_020_006_001, exception.getCode());
    }

}
