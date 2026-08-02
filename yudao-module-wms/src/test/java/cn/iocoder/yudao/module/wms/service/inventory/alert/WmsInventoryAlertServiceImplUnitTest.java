package cn.iocoder.yudao.module.wms.service.inventory.alert;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.wms.controller.admin.inventory.alert.vo.WmsInventoryAlertPageReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.inventory.WmsInventoryAlertDO;
import cn.iocoder.yudao.module.wms.dal.mysql.inventory.WmsInventoryAlertMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.INVENTORY_ALERT_NOT_EXISTS;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.INVENTORY_ALERT_STATUS_INVALID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * {@link WmsInventoryAlertServiceImpl} 的纯 Mockito 单元测试（Item C 覆盖率补测）
 *
 * @author ZhiCloud 平台加固
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class WmsInventoryAlertServiceImplUnitTest {

    @Mock
    private WmsInventoryAlertMapper inventoryAlertMapper;

    @InjectMocks
    private WmsInventoryAlertServiceImpl inventoryAlertService;

    @Test
    public void testCreateInventoryAlert_defaultStatus() {
        doAnswer(invocation -> {
            WmsInventoryAlertDO arg = invocation.getArgument(0);
            arg.setId(700L);
            return 1;
        }).when(inventoryAlertMapper).insert(any(WmsInventoryAlertDO.class));

        WmsInventoryAlertDO alert = new WmsInventoryAlertDO();
        Long id = inventoryAlertService.createInventoryAlert(alert);

        assertEquals(700L, id);
        // status 为空时兜底为「未处理」0
        assertEquals(0, alert.getStatus());
    }

    @Test
    public void testCreateInventoryAlertList_empty() {
        inventoryAlertService.createInventoryAlertList(Collections.emptyList());
        inventoryAlertService.createInventoryAlertList(null);

        verify(inventoryAlertMapper, never()).insertBatch(anyList());
    }

    @Test
    public void testCreateInventoryAlertList_success() {
        List<WmsInventoryAlertDO> alerts = new ArrayList<>();
        alerts.add(new WmsInventoryAlertDO());
        WmsInventoryAlertDO withStatus = new WmsInventoryAlertDO();
        withStatus.setStatus(2);
        alerts.add(withStatus);

        inventoryAlertService.createInventoryAlertList(alerts);

        verify(inventoryAlertMapper).insertBatch(alerts);
        assertEquals(0, alerts.get(0).getStatus());
        assertEquals(2, alerts.get(1).getStatus());
    }

    @Test
    public void testConfirmInventoryAlert_success() {
        WmsInventoryAlertDO alert = new WmsInventoryAlertDO();
        alert.setId(1L);
        alert.setStatus(0);
        when(inventoryAlertMapper.selectById(1L)).thenReturn(alert);

        inventoryAlertService.confirmInventoryAlert(1L);

        ArgumentCaptor<WmsInventoryAlertDO> captor = ArgumentCaptor.forClass(WmsInventoryAlertDO.class);
        verify(inventoryAlertMapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getStatus());
    }

    @Test
    public void testConfirmInventoryAlert_statusInvalid() {
        WmsInventoryAlertDO alert = new WmsInventoryAlertDO();
        alert.setId(1L);
        alert.setStatus(2);
        when(inventoryAlertMapper.selectById(1L)).thenReturn(alert);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inventoryAlertService.confirmInventoryAlert(1L));
        assertEquals(INVENTORY_ALERT_STATUS_INVALID.getCode(), ex.getCode());
    }

    @Test
    public void testProcessInventoryAlert_success() {
        WmsInventoryAlertDO alert = new WmsInventoryAlertDO();
        alert.setId(1L);
        alert.setStatus(1);
        when(inventoryAlertMapper.selectById(1L)).thenReturn(alert);

        inventoryAlertService.processInventoryAlert(1L);

        ArgumentCaptor<WmsInventoryAlertDO> captor = ArgumentCaptor.forClass(WmsInventoryAlertDO.class);
        verify(inventoryAlertMapper).updateById(captor.capture());
        assertEquals(2, captor.getValue().getStatus());
    }

    @Test
    public void testProcessInventoryAlert_statusInvalid() {
        WmsInventoryAlertDO alert = new WmsInventoryAlertDO();
        alert.setId(1L);
        alert.setStatus(0);
        when(inventoryAlertMapper.selectById(1L)).thenReturn(alert);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inventoryAlertService.processInventoryAlert(1L));
        assertEquals(INVENTORY_ALERT_STATUS_INVALID.getCode(), ex.getCode());
    }

    @Test
    public void testValidateInventoryAlertExists_notExists() {
        when(inventoryAlertMapper.selectById(404L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inventoryAlertService.validateInventoryAlertExists(404L));
        assertEquals(INVENTORY_ALERT_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    public void testQueryMethods() {
        when(inventoryAlertMapper.selectById(1L)).thenReturn(new WmsInventoryAlertDO());
        when(inventoryAlertMapper.selectPage(any(WmsInventoryAlertPageReqVO.class))).thenReturn(PageResult.empty());

        assertNotNull(inventoryAlertService.getInventoryAlert(1L));
        assertNotNull(inventoryAlertService.getInventoryAlertPage(new WmsInventoryAlertPageReqVO()));
    }

}
