package cn.iocoder.yudao.module.wms.service.inventory.alert;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.wms.controller.admin.inventory.alert.vo.WmsSafetyStockConfigPageReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.inventory.alert.vo.WmsSafetyStockConfigSaveReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.inventory.WmsSafetyStockConfigDO;
import cn.iocoder.yudao.module.wms.dal.mysql.inventory.WmsSafetyStockConfigMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.SAFETY_STOCK_CONFIG_DUPLICATE;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.SAFETY_STOCK_CONFIG_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link WmsSafetyStockConfigServiceImpl} 的纯 Mockito 单元测试（Item C 覆盖率补测）
 *
 * @author ZhiCloud 平台加固
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class WmsSafetyStockConfigServiceImplUnitTest {

    @Mock
    private WmsSafetyStockConfigMapper safetyStockConfigMapper;

    @InjectMocks
    private WmsSafetyStockConfigServiceImpl safetyStockConfigService;

    private static WmsSafetyStockConfigSaveReqVO buildSaveReqVO(Long id) {
        WmsSafetyStockConfigSaveReqVO reqVO = new WmsSafetyStockConfigSaveReqVO();
        reqVO.setId(id);
        reqVO.setWarehouseId(1L);
        reqVO.setProductId(2L);
        reqVO.setSafetyStock(new BigDecimal("100"));
        reqVO.setMaxStock(new BigDecimal("500"));
        reqVO.setMinStock(new BigDecimal("50"));
        return reqVO;
    }

    @Test
    public void testCreateSafetyStockConfig_success() {
        when(safetyStockConfigMapper.selectByWarehouseIdAndProductId(1L, 2L)).thenReturn(null);
        doAnswer(invocation -> {
            WmsSafetyStockConfigDO arg = invocation.getArgument(0);
            arg.setId(600L);
            return 1;
        }).when(safetyStockConfigMapper).insert(any(WmsSafetyStockConfigDO.class));

        assertEquals(600L, safetyStockConfigService.createSafetyStockConfig(buildSaveReqVO(null)));
    }

    @Test
    public void testCreateSafetyStockConfig_duplicate() {
        when(safetyStockConfigMapper.selectByWarehouseIdAndProductId(1L, 2L))
                .thenReturn(WmsSafetyStockConfigDO.builder().id(9L).build());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> safetyStockConfigService.createSafetyStockConfig(buildSaveReqVO(null)));
        assertEquals(SAFETY_STOCK_CONFIG_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    public void testUpdateSafetyStockConfig_success() {
        when(safetyStockConfigMapper.selectById(5L)).thenReturn(WmsSafetyStockConfigDO.builder().id(5L).build());
        when(safetyStockConfigMapper.selectByWarehouseIdAndProductId(1L, 2L))
                .thenReturn(WmsSafetyStockConfigDO.builder().id(5L).build());

        safetyStockConfigService.updateSafetyStockConfig(buildSaveReqVO(5L));

        verify(safetyStockConfigMapper).updateById(any(WmsSafetyStockConfigDO.class));
    }

    @Test
    public void testUpdateSafetyStockConfig_notExists() {
        when(safetyStockConfigMapper.selectById(5L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> safetyStockConfigService.updateSafetyStockConfig(buildSaveReqVO(5L)));
        assertEquals(SAFETY_STOCK_CONFIG_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    public void testUpdateSafetyStockConfig_duplicateWithOther() {
        when(safetyStockConfigMapper.selectById(5L)).thenReturn(WmsSafetyStockConfigDO.builder().id(5L).build());
        when(safetyStockConfigMapper.selectByWarehouseIdAndProductId(1L, 2L))
                .thenReturn(WmsSafetyStockConfigDO.builder().id(6L).build());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> safetyStockConfigService.updateSafetyStockConfig(buildSaveReqVO(5L)));
        assertEquals(SAFETY_STOCK_CONFIG_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    public void testDeleteSafetyStockConfig_success() {
        when(safetyStockConfigMapper.selectById(5L)).thenReturn(WmsSafetyStockConfigDO.builder().id(5L).build());

        safetyStockConfigService.deleteSafetyStockConfig(5L);

        verify(safetyStockConfigMapper).deleteById(5L);
    }

    @Test
    public void testQueryMethods() {
        when(safetyStockConfigMapper.selectById(1L)).thenReturn(WmsSafetyStockConfigDO.builder().id(1L).build());
        when(safetyStockConfigMapper.selectPage(any(WmsSafetyStockConfigPageReqVO.class)))
                .thenReturn(PageResult.empty());
        when(safetyStockConfigMapper.selectList()).thenReturn(List.of(WmsSafetyStockConfigDO.builder().id(1L).build()));
        when(safetyStockConfigMapper.selectByWarehouseIdAndProductId(1L, 2L))
                .thenReturn(WmsSafetyStockConfigDO.builder().id(1L).build());
        when(safetyStockConfigMapper.selectListByProductIds(anyCollection()))
                .thenReturn(List.of(WmsSafetyStockConfigDO.builder().id(1L).build()));

        assertNotNull(safetyStockConfigService.getSafetyStockConfig(1L));
        assertNotNull(safetyStockConfigService.getSafetyStockConfigPage(new WmsSafetyStockConfigPageReqVO()));
        assertEquals(1, safetyStockConfigService.getSafetyStockConfigList().size());
        assertNotNull(safetyStockConfigService.getSafetyStockConfigByWarehouseAndProduct(1L, 2L));
        assertEquals(1, safetyStockConfigService.getSafetyStockConfigListByProductIds(List.of(2L)).size());
        assertTrue(safetyStockConfigService.getSafetyStockConfigListByProductIds(Collections.emptyList()).isEmpty());
        assertTrue(safetyStockConfigService.getSafetyStockConfigListByProductIds(null).isEmpty());
    }

}
