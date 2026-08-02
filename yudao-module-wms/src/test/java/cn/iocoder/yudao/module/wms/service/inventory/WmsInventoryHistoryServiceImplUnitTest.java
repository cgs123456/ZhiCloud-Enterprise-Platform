package cn.iocoder.yudao.module.wms.service.inventory;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.wms.controller.admin.inventory.vo.history.WmsInventoryHistoryPageReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.inventory.WmsInventoryHistoryDO;
import cn.iocoder.yudao.module.wms.dal.mysql.inventory.WmsInventoryHistoryMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * {@link WmsInventoryHistoryServiceImpl} 的纯 Mockito 单元测试（Item C 覆盖率补测）
 *
 * @author ZhiCloud 平台加固
 */
@ExtendWith(MockitoExtension.class)
public class WmsInventoryHistoryServiceImplUnitTest {

    @Mock
    private WmsInventoryHistoryMapper inventoryHistoryMapper;

    @InjectMocks
    private WmsInventoryHistoryServiceImpl inventoryHistoryService;

    @Test
    public void testGetInventoryHistoryPage() {
        when(inventoryHistoryMapper.selectPage(any(WmsInventoryHistoryPageReqVO.class)))
                .thenReturn(PageResult.empty());

        assertNotNull(inventoryHistoryService.getInventoryHistoryPage(new WmsInventoryHistoryPageReqVO()));
    }

    @Test
    public void testCreateInventoryHistoryList_empty() {
        inventoryHistoryService.createInventoryHistoryList(Collections.emptyList());
        inventoryHistoryService.createInventoryHistoryList(null);

        verify(inventoryHistoryMapper, never()).insertBatch(anyList());
    }

    @Test
    public void testCreateInventoryHistoryList_success() {
        List<WmsInventoryHistoryDO> list = List.of(
                WmsInventoryHistoryDO.builder().id(1L).build(),
                WmsInventoryHistoryDO.builder().id(2L).build());

        inventoryHistoryService.createInventoryHistoryList(list);

        verify(inventoryHistoryMapper).insertBatch(list);
    }

}
