package cn.zhicloud.module.mes.service.pro.route;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.mes.controller.admin.pro.route.vo.capacity.MesProRouteCapacitySaveReqVO;
import cn.zhicloud.module.mes.dal.dataobject.pro.route.MesProRouteCapacityDO;
import cn.zhicloud.module.mes.dal.mysql.pro.route.MesProRouteCapacityMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link MesProRouteCapacityServiceImpl} 的单元测试（mock mapper，无需 H2 表）
 *
 * 覆盖 D4 产能 CRUD：upsert（新增/更新）、已启用护栏、按路线查询、级联物理删（§12.270 D4/D5）。
 */
@Import(MesProRouteCapacityServiceImpl.class)
public class MesProRouteCapacityServiceImplTest extends BaseDbUnitTest {

    private static final Long ROUTE_ID = 300L;

    @MockitoBean
    private MesProRouteCapacityMapper routeCapacityMapper;
    @MockitoBean
    private MesProRouteService routeService;

    @Resource
    private MesProRouteCapacityService routeCapacityService;

    private MesProRouteCapacitySaveReqVO buildSaveReqVO(String capacity, String efficiency) {
        MesProRouteCapacitySaveReqVO reqVO = new MesProRouteCapacitySaveReqVO();
        reqVO.setRouteId(ROUTE_ID);
        reqVO.setCapacity(new BigDecimal(capacity));
        reqVO.setEfficiency(new BigDecimal(efficiency));
        reqVO.setRemark("备注");
        return reqVO;
    }

    @Test
    public void testSaveRouteCapacity_createWhenAbsent() {
        when(routeCapacityMapper.selectByRouteId(ROUTE_ID)).thenReturn(null);
        doAnswer(inv -> {
            MesProRouteCapacityDO capacity = inv.getArgument(0);
            capacity.setId(99L);
            return 1;
        }).when(routeCapacityMapper).insert(any(MesProRouteCapacityDO.class));

        Long id = routeCapacityService.saveRouteCapacity(buildSaveReqVO("12.50", "1.000"));

        assertEquals(99L, id);
        // 已启用护栏先于写入
        verify(routeService).validateRouteNotEnable(ROUTE_ID);
        ArgumentCaptor<MesProRouteCapacityDO> captor = ArgumentCaptor.forClass(MesProRouteCapacityDO.class);
        verify(routeCapacityMapper).insert(captor.capture());
        assertEquals(ROUTE_ID, captor.getValue().getRouteId());
        assertEquals(0, captor.getValue().getCapacity().compareTo(new BigDecimal("12.50")));
        assertEquals(0, captor.getValue().getEfficiency().compareTo(new BigDecimal("1.000")));
        verify(routeCapacityMapper, never()).updateById(any(MesProRouteCapacityDO.class));
    }

    @Test
    public void testSaveRouteCapacity_updateWhenPresent() {
        when(routeCapacityMapper.selectByRouteId(ROUTE_ID)).thenReturn(
                MesProRouteCapacityDO.builder().id(77L).routeId(ROUTE_ID)
                        .capacity(new BigDecimal("1")).efficiency(BigDecimal.ONE).build());

        Long id = routeCapacityService.saveRouteCapacity(buildSaveReqVO("5.00", "0.800"));

        assertEquals(77L, id);
        ArgumentCaptor<MesProRouteCapacityDO> captor = ArgumentCaptor.forClass(MesProRouteCapacityDO.class);
        verify(routeCapacityMapper).updateById(captor.capture());
        assertEquals(77L, captor.getValue().getId());
        assertEquals(ROUTE_ID, captor.getValue().getRouteId());
        assertEquals(0, captor.getValue().getCapacity().compareTo(new BigDecimal("5.00")));
        assertEquals(0, captor.getValue().getEfficiency().compareTo(new BigDecimal("0.800")));
        verify(routeCapacityMapper, never()).insert(any(MesProRouteCapacityDO.class));
    }

    @Test
    public void testGetRouteCapacityByRouteId() {
        MesProRouteCapacityDO capacity = MesProRouteCapacityDO.builder().id(1L).routeId(ROUTE_ID).build();
        when(routeCapacityMapper.selectByRouteId(ROUTE_ID)).thenReturn(capacity);

        assertEquals(capacity, routeCapacityService.getRouteCapacityByRouteId(ROUTE_ID));
        // 未配置 → null（调用方回退）
        assertNull(routeCapacityService.getRouteCapacityByRouteId(999L));
    }

    @Test
    public void testDeleteRouteCapacityByRouteId_physicalWash() {
        routeCapacityService.deleteRouteCapacityByRouteId(ROUTE_ID);

        // 级联冲洗走物理删（防逻辑删 tombstone 顶撞 uk(tenant_id, route_id)）
        verify(routeCapacityMapper).deleteByRouteIdPhysical(ROUTE_ID);
        verify(routeCapacityMapper, never()).deleteById(anyLong());
    }

}
