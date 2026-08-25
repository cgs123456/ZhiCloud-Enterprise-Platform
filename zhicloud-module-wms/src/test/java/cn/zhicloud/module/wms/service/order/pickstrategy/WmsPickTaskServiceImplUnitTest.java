package cn.zhicloud.module.wms.service.order.pickstrategy;

import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.security.core.util.SecurityFrameworkUtils;
import cn.zhicloud.module.wms.controller.admin.order.pickstrategy.vo.WmsPickTaskPageReqVO;
import cn.zhicloud.module.wms.dal.dataobject.order.pickstrategy.WmsPickTaskDO;
import cn.zhicloud.module.wms.dal.mysql.order.pickstrategy.WmsPickTaskMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;

import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.PICK_TASK_NOT_EXISTS;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.PICK_TASK_NOT_YOURS;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.PICK_TASK_STATUS_NOT_PICKABLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link WmsPickTaskServiceImpl} 的纯 Mockito 单元测试（Item C 覆盖率补测）
 *
 * @author ZhiCloud 平台加固
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class WmsPickTaskServiceImplUnitTest {

    @Mock
    private WmsPickTaskMapper pickTaskMapper;

    @InjectMocks
    private WmsPickTaskServiceImpl pickTaskService;

    @Test
    public void testConfirmPick_success() {
        Long mockUserId = 100L;
        try (var mockStatic = org.mockito.Mockito.mockStatic(SecurityFrameworkUtils.class)) {
            mockStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(mockUserId);

            when(pickTaskMapper.selectById(1L)).thenReturn(WmsPickTaskDO.builder().id(1L).status(10).pickerUserId(mockUserId).build());

            pickTaskService.confirmPick(1L, new BigDecimal("5"));

            ArgumentCaptor<WmsPickTaskDO> captor = ArgumentCaptor.forClass(WmsPickTaskDO.class);
            verify(pickTaskMapper).updateById(captor.capture());
            assertEquals(20, captor.getValue().getStatus());
            assertEquals(new BigDecimal("5"), captor.getValue().getPickedQuantity());
            assertNotNull(captor.getValue().getPickTime());
        }
    }

    @Test
    public void testConfirmPick_alreadyPickedStillAllowed() {
        Long mockUserId = 100L;
        try (var mockStatic = org.mockito.Mockito.mockStatic(SecurityFrameworkUtils.class)) {
            mockStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(mockUserId);

            when(pickTaskMapper.selectById(1L)).thenReturn(WmsPickTaskDO.builder().id(1L).status(20).pickerUserId(mockUserId).build());

            pickTaskService.confirmPick(1L, new BigDecimal("8"));

            verify(pickTaskMapper).updateById(any(WmsPickTaskDO.class));
        }
    }

    @Test
    public void testConfirmPick_notExists() {
        try (var mockStatic = org.mockito.Mockito.mockStatic(SecurityFrameworkUtils.class)) {
            mockStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(100L);

            when(pickTaskMapper.selectById(404L)).thenReturn(null);

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> pickTaskService.confirmPick(404L, BigDecimal.ONE));
            assertEquals(PICK_TASK_NOT_EXISTS.getCode(), ex.getCode());
        }
    }

    @Test
    public void testConfirmPick_statusNotPickable() {
        Long mockUserId = 100L;
        try (var mockStatic = org.mockito.Mockito.mockStatic(SecurityFrameworkUtils.class)) {
            mockStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(mockUserId);

            when(pickTaskMapper.selectById(1L)).thenReturn(WmsPickTaskDO.builder().id(1L).status(30).pickerUserId(mockUserId).build());

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> pickTaskService.confirmPick(1L, BigDecimal.ONE));
            assertEquals(PICK_TASK_STATUS_NOT_PICKABLE.getCode(), ex.getCode());
        }
    }

    @Test
    public void testConfirmPick_idorBlocked() {
        Long mockUserId = 100L;
        try (var mockStatic = org.mockito.Mockito.mockStatic(SecurityFrameworkUtils.class)) {
            mockStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(mockUserId);

            // 任务归属其他用户
            when(pickTaskMapper.selectById(1L)).thenReturn(WmsPickTaskDO.builder().id(1L).status(10).pickerUserId(999L).build());

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> pickTaskService.confirmPick(1L, BigDecimal.ONE));
            assertEquals(PICK_TASK_NOT_YOURS.getCode(), ex.getCode());
        }
    }

    @Test
    public void testGetMyPickTasks() {
        assertTrue(pickTaskService.getMyPickTasks(null).isEmpty());

        when(pickTaskMapper.selectListByPickerUserId(9L))
                .thenReturn(List.of(WmsPickTaskDO.builder().id(1L).build()));
        assertEquals(1, pickTaskService.getMyPickTasks(9L).size());
    }

    @Test
    public void testGetPickTasksByShipmentOrderId() {
        assertTrue(pickTaskService.getPickTasksByShipmentOrderId(null).isEmpty());

        when(pickTaskMapper.selectListByShipmentOrderId(7L))
                .thenReturn(List.of(WmsPickTaskDO.builder().id(1L).build()));
        assertEquals(1, pickTaskService.getPickTasksByShipmentOrderId(7L).size());
    }

    @Test
    public void testQueryMethods() {
        when(pickTaskMapper.selectById(1L)).thenReturn(WmsPickTaskDO.builder().id(1L).build());
        when(pickTaskMapper.selectPage(any(WmsPickTaskPageReqVO.class))).thenReturn(PageResult.empty());

        assertNotNull(pickTaskService.getPickTask(1L));
        assertNotNull(pickTaskService.getPickTaskPage(new WmsPickTaskPageReqVO()));
    }

}
