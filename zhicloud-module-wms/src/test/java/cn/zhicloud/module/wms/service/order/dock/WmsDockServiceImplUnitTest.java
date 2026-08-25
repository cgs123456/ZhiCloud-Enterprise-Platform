package cn.zhicloud.module.wms.service.order.dock;

import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.wms.controller.admin.order.dock.vo.WmsDockPageReqVO;
import cn.zhicloud.module.wms.controller.admin.order.dock.vo.WmsDockSaveReqVO;
import cn.zhicloud.module.wms.dal.dataobject.order.dock.WmsDockDO;
import cn.zhicloud.module.wms.dal.mysql.order.dock.WmsDockMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.DOCK_CODE_DUPLICATE;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.DOCK_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link WmsDockServiceImpl} 的纯 Mockito 单元测试（Item C 覆盖率补测）
 *
 * @author ZhiCloud 平台加固
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class WmsDockServiceImplUnitTest {

    @Mock
    private WmsDockMapper dockMapper;

    @InjectMocks
    private WmsDockServiceImpl dockService;

    private static WmsDockSaveReqVO buildSaveReqVO(Long id, Integer status) {
        WmsDockSaveReqVO reqVO = new WmsDockSaveReqVO();
        reqVO.setId(id);
        reqVO.setWarehouseId(1L);
        reqVO.setDockCode("D001");
        reqVO.setDockName("1号月台");
        reqVO.setDockType(1);
        reqVO.setStatus(status);
        return reqVO;
    }

    @Test
    public void testCreateDock_defaultStatusIdle() {
        when(dockMapper.selectByDockCode("D001")).thenReturn(null);
        doAnswer(invocation -> {
            WmsDockDO arg = invocation.getArgument(0);
            arg.setId(400L);
            return 1;
        }).when(dockMapper).insert(any(WmsDockDO.class));

        Long id = dockService.createDock(buildSaveReqVO(null, null));

        assertEquals(400L, id);
        ArgumentCaptor<WmsDockDO> captor = ArgumentCaptor.forClass(WmsDockDO.class);
        verify(dockMapper).insert(captor.capture());
        // status 为空时兜底为「空闲」10
        assertEquals(10, captor.getValue().getStatus());
    }

    @Test
    public void testCreateDock_keepGivenStatus() {
        when(dockMapper.selectByDockCode("D001")).thenReturn(null);

        dockService.createDock(buildSaveReqVO(null, 20));

        ArgumentCaptor<WmsDockDO> captor = ArgumentCaptor.forClass(WmsDockDO.class);
        verify(dockMapper).insert(captor.capture());
        assertEquals(20, captor.getValue().getStatus());
    }

    @Test
    public void testCreateDock_codeDuplicate() {
        when(dockMapper.selectByDockCode("D001")).thenReturn(WmsDockDO.builder().id(9L).build());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> dockService.createDock(buildSaveReqVO(null, null)));
        assertEquals(DOCK_CODE_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    public void testUpdateDock_success() {
        when(dockMapper.selectById(5L)).thenReturn(WmsDockDO.builder().id(5L).build());
        when(dockMapper.selectByDockCode("D001")).thenReturn(WmsDockDO.builder().id(5L).build());

        dockService.updateDock(buildSaveReqVO(5L, 10));

        verify(dockMapper).updateById(any(WmsDockDO.class));
    }

    @Test
    public void testUpdateDock_codeDuplicateWithOther() {
        when(dockMapper.selectById(5L)).thenReturn(WmsDockDO.builder().id(5L).build());
        when(dockMapper.selectByDockCode("D001")).thenReturn(WmsDockDO.builder().id(6L).build());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> dockService.updateDock(buildSaveReqVO(5L, 10)));
        assertEquals(DOCK_CODE_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    public void testDeleteDock_success() {
        when(dockMapper.selectById(5L)).thenReturn(WmsDockDO.builder().id(5L).build());

        dockService.deleteDock(5L);

        verify(dockMapper).deleteById(5L);
    }

    @Test
    public void testValidateDockExists() {
        // id 为空直接返回 null
        assertNull(dockService.validateDockExists(null));

        when(dockMapper.selectById(404L)).thenReturn(null);
        ServiceException ex = assertThrows(ServiceException.class, () -> dockService.validateDockExists(404L));
        assertEquals(DOCK_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    public void testGetDockMap() {
        assertTrue(dockService.getDockMap(Collections.emptyList()).isEmpty());

        when(dockMapper.selectBatchIds(anyCollection()))
                .thenReturn(List.of(WmsDockDO.builder().id(1L).build(), WmsDockDO.builder().id(2L).build()));
        Map<Long, WmsDockDO> map = dockService.getDockMap(List.of(1L, 2L));
        assertEquals(2, map.size());
        assertNotNull(map.get(1L));
    }

    @Test
    public void testQueryMethods() {
        when(dockMapper.selectById(1L)).thenReturn(WmsDockDO.builder().id(1L).build());
        when(dockMapper.selectPage(any(WmsDockPageReqVO.class))).thenReturn(PageResult.empty());

        assertNotNull(dockService.getDock(1L));
        assertNotNull(dockService.getDockPage(new WmsDockPageReqVO()));
    }

}
