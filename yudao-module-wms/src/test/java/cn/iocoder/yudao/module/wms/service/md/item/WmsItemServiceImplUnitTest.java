package cn.iocoder.yudao.module.wms.service.md.item;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.wms.controller.admin.md.item.vo.item.WmsItemListReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.md.item.vo.item.WmsItemPageReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.md.item.vo.item.WmsItemSaveReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.item.WmsItemDO;
import cn.iocoder.yudao.module.wms.dal.mysql.md.item.WmsItemMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.ITEM_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.ITEM_NAME_DUPLICATE;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.ITEM_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link WmsItemServiceImpl} 的纯 Mockito 单元测试（Item C 覆盖率补测）
 *
 * @author ZhiCloud 平台加固
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class WmsItemServiceImplUnitTest {

    @Mock
    private WmsItemMapper itemMapper;
    @Mock
    private WmsItemCategoryService categoryService;
    @Mock
    private WmsItemBrandService brandService;
    @Mock
    private WmsItemSkuService itemSkuService;

    @InjectMocks
    private WmsItemServiceImpl itemService;

    private static WmsItemSaveReqVO buildSaveReqVO(Long id, Long brandId) {
        WmsItemSaveReqVO reqVO = new WmsItemSaveReqVO();
        reqVO.setId(id);
        reqVO.setCode("I001");
        reqVO.setName("笔记本电脑");
        reqVO.setCategoryId(10L);
        reqVO.setBrandId(brandId);
        reqVO.setUnit("台");
        return reqVO;
    }

    @Test
    public void testCreateItem_success() {
        when(itemMapper.selectByCode("I001")).thenReturn(null);
        when(itemMapper.selectByName("笔记本电脑")).thenReturn(null);
        doAnswer(invocation -> {
            WmsItemDO arg = invocation.getArgument(0);
            arg.setId(800L);
            return 1;
        }).when(itemMapper).insert(any(WmsItemDO.class));

        Long id = itemService.createItem(buildSaveReqVO(null, 20L));

        assertEquals(800L, id);
        verify(categoryService).validateItemCategoryExists(10L);
        verify(brandService).validateItemBrandExists(20L);
        verify(itemSkuService).createItemSkuList(eq(800L), any());
    }

    @Test
    public void testCreateItem_brandIdNullSkipsBrandCheck() {
        when(itemMapper.selectByCode("I001")).thenReturn(null);
        when(itemMapper.selectByName("笔记本电脑")).thenReturn(null);

        itemService.createItem(buildSaveReqVO(null, null));

        verify(brandService, never()).validateItemBrandExists(any());
    }

    @Test
    public void testCreateItem_codeDuplicate() {
        when(itemMapper.selectByCode("I001")).thenReturn(WmsItemDO.builder().id(1L).build());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> itemService.createItem(buildSaveReqVO(null, 20L)));
        assertEquals(ITEM_CODE_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    public void testCreateItem_nameDuplicate() {
        when(itemMapper.selectByCode("I001")).thenReturn(null);
        when(itemMapper.selectByName("笔记本电脑")).thenReturn(WmsItemDO.builder().id(1L).build());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> itemService.createItem(buildSaveReqVO(null, 20L)));
        assertEquals(ITEM_NAME_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    public void testUpdateItem_success() {
        when(itemMapper.selectById(5L)).thenReturn(WmsItemDO.builder().id(5L).build());
        when(itemMapper.selectByCode("I001")).thenReturn(WmsItemDO.builder().id(5L).build());
        when(itemMapper.selectByName("笔记本电脑")).thenReturn(WmsItemDO.builder().id(5L).build());

        itemService.updateItem(buildSaveReqVO(5L, 20L));

        verify(itemMapper).updateById(any(WmsItemDO.class));
        verify(itemSkuService).updateItemSkuList(eq(5L), any());
    }

    @Test
    public void testUpdateItem_notExists() {
        when(itemMapper.selectById(5L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> itemService.updateItem(buildSaveReqVO(5L, 20L)));
        assertEquals(ITEM_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    public void testDeleteItem_success() {
        when(itemMapper.selectById(5L)).thenReturn(WmsItemDO.builder().id(5L).build());

        itemService.deleteItem(5L);

        verify(itemSkuService).deleteItemSkuListByItemId(5L);
        verify(itemMapper).deleteById(5L);
    }

    @Test
    public void testValidateItemExists_notExists() {
        when(itemMapper.selectById(404L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> itemService.validateItemExists(404L));
        assertEquals(ITEM_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    public void testQueryMethods() {
        when(itemMapper.selectById(1L)).thenReturn(WmsItemDO.builder().id(1L).build());
        when(itemMapper.selectPage(any(WmsItemPageReqVO.class), ArgumentMatchers.<Collection<Long>>any()))
                .thenReturn(PageResult.empty());
        when(itemMapper.selectList(any(WmsItemListReqVO.class), any()))
                .thenReturn(List.of(WmsItemDO.builder().id(1L).build()));
        when(itemMapper.selectByIds(anyCollection())).thenReturn(List.of(WmsItemDO.builder().id(1L).build()));
        when(itemMapper.selectCountByCategoryId(10L)).thenReturn(3L);
        when(itemMapper.selectCountByBrandId(20L)).thenReturn(4L);

        assertNotNull(itemService.getItem(1L));
        assertNotNull(itemService.getItemPage(new WmsItemPageReqVO()));
        assertEquals(1, itemService.getItemList(new WmsItemListReqVO()).size());
        assertEquals(1, itemService.getItemList(List.of(1L)).size());
        assertTrue(itemService.getItemList(Collections.emptyList()).isEmpty());
        assertEquals(3L, itemService.getItemCountByCategoryId(10L));
        assertEquals(4L, itemService.getItemCountByBrandId(20L));
    }

}
