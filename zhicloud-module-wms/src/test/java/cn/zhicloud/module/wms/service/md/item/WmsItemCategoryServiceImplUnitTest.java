package cn.zhicloud.module.wms.service.md.item;

import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.module.wms.controller.admin.md.item.vo.category.WmsItemCategoryListReqVO;
import cn.zhicloud.module.wms.controller.admin.md.item.vo.category.WmsItemCategorySaveReqVO;
import cn.zhicloud.module.wms.dal.dataobject.md.item.WmsItemCategoryDO;
import cn.zhicloud.module.wms.dal.mysql.md.item.WmsItemCategoryMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link WmsItemCategoryServiceImpl} 的纯 Mockito 单元测试（Item C 覆盖率补测）
 *
 * @author ZhiCloud 平台加固
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class WmsItemCategoryServiceImplUnitTest {

    @Mock
    private WmsItemCategoryMapper categoryMapper;
    @Mock
    private WmsItemService itemService;

    @InjectMocks
    private WmsItemCategoryServiceImpl categoryService;

    private static WmsItemCategorySaveReqVO buildSaveReqVO(Long id, Long parentId) {
        WmsItemCategorySaveReqVO reqVO = new WmsItemCategorySaveReqVO();
        reqVO.setId(id);
        reqVO.setParentId(parentId);
        reqVO.setCode("C001");
        reqVO.setName("电子产品");
        reqVO.setSort(1);
        reqVO.setStatus(0);
        return reqVO;
    }

    @Test
    public void testCreateItemCategory_success() {
        when(categoryMapper.selectByCode("C001")).thenReturn(null);
        when(categoryMapper.selectByParentIdAndName(0L, "电子产品")).thenReturn(null);
        doAnswer(invocation -> {
            WmsItemCategoryDO arg = invocation.getArgument(0);
            arg.setId(500L);
            return 1;
        }).when(categoryMapper).insert(any(WmsItemCategoryDO.class));

        assertEquals(500L, categoryService.createItemCategory(buildSaveReqVO(null, 0L)));
    }

    @Test
    public void testCreateItemCategory_codeDuplicate() {
        when(categoryMapper.selectByCode("C001")).thenReturn(WmsItemCategoryDO.builder().id(1L).build());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> categoryService.createItemCategory(buildSaveReqVO(null, 0L)));
        assertEquals(ITEM_CATEGORY_CODE_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    public void testCreateItemCategory_nameDuplicate() {
        when(categoryMapper.selectByCode("C001")).thenReturn(null);
        when(categoryMapper.selectByParentIdAndName(0L, "电子产品"))
                .thenReturn(WmsItemCategoryDO.builder().id(1L).build());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> categoryService.createItemCategory(buildSaveReqVO(null, 0L)));
        assertEquals(ITEM_CATEGORY_NAME_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    public void testCreateItemCategory_parentNotExists() {
        when(categoryMapper.selectByCode("C001")).thenReturn(null);
        when(categoryMapper.selectByParentIdAndName(9L, "电子产品")).thenReturn(null);
        when(categoryMapper.selectById(9L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> categoryService.createItemCategory(buildSaveReqVO(null, 9L)));
        assertEquals(ITEM_CATEGORY_PARENT_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    public void testUpdateItemCategory_parentIsSelf() {
        when(categoryMapper.selectById(3L)).thenReturn(WmsItemCategoryDO.builder().id(3L).build());
        when(categoryMapper.selectByCode("C001")).thenReturn(null);
        when(categoryMapper.selectByParentIdAndName(3L, "电子产品")).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> categoryService.updateItemCategory(buildSaveReqVO(3L, 3L)));
        assertEquals(ITEM_CATEGORY_PARENT_ERROR.getCode(), ex.getCode());
    }

    @Test
    public void testUpdateItemCategory_parentIsChild() {
        // 3 -> 父 4，而 4 的父又是 3，形成环路
        when(categoryMapper.selectById(3L)).thenReturn(WmsItemCategoryDO.builder().id(3L).parentId(0L).build());
        when(categoryMapper.selectById(4L)).thenReturn(WmsItemCategoryDO.builder().id(4L).parentId(3L).build());
        when(categoryMapper.selectByCode("C001")).thenReturn(null);
        when(categoryMapper.selectByParentIdAndName(4L, "电子产品")).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> categoryService.updateItemCategory(buildSaveReqVO(3L, 4L)));
        assertEquals(ITEM_CATEGORY_PARENT_IS_CHILD.getCode(), ex.getCode());
    }

    @Test
    public void testUpdateItemCategory_success() {
        when(categoryMapper.selectById(3L)).thenReturn(WmsItemCategoryDO.builder().id(3L).parentId(0L).build());
        when(categoryMapper.selectByCode("C001")).thenReturn(WmsItemCategoryDO.builder().id(3L).build());
        when(categoryMapper.selectByParentIdAndName(0L, "电子产品"))
                .thenReturn(WmsItemCategoryDO.builder().id(3L).build());

        categoryService.updateItemCategory(buildSaveReqVO(3L, 0L));

        verify(categoryMapper).updateById(any(WmsItemCategoryDO.class));
    }

    @Test
    public void testDeleteItemCategory_success() {
        when(categoryMapper.selectById(3L)).thenReturn(WmsItemCategoryDO.builder().id(3L).build());
        when(categoryMapper.selectCountByParentId(3L)).thenReturn(0L);
        when(itemService.getItemCountByCategoryId(3L)).thenReturn(0L);

        categoryService.deleteItemCategory(3L);

        verify(categoryMapper).deleteById(3L);
    }

    @Test
    public void testDeleteItemCategory_hasChildren() {
        when(categoryMapper.selectById(3L)).thenReturn(WmsItemCategoryDO.builder().id(3L).build());
        when(categoryMapper.selectCountByParentId(3L)).thenReturn(2L);

        ServiceException ex = assertThrows(ServiceException.class, () -> categoryService.deleteItemCategory(3L));
        assertEquals(ITEM_CATEGORY_HAS_CHILDREN.getCode(), ex.getCode());
    }

    @Test
    public void testDeleteItemCategory_hasItem() {
        when(categoryMapper.selectById(3L)).thenReturn(WmsItemCategoryDO.builder().id(3L).build());
        when(categoryMapper.selectCountByParentId(3L)).thenReturn(0L);
        when(itemService.getItemCountByCategoryId(3L)).thenReturn(5L);

        ServiceException ex = assertThrows(ServiceException.class, () -> categoryService.deleteItemCategory(3L));
        assertEquals(ITEM_CATEGORY_HAS_ITEM.getCode(), ex.getCode());
    }

    @Test
    public void testValidateItemCategoryExists_notExists() {
        when(categoryMapper.selectById(404L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> categoryService.validateItemCategoryExists(404L));
        assertEquals(ITEM_CATEGORY_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    public void testGetSelfAndChildItemCategoryIdList() {
        assertNull(categoryService.getSelfAndChildItemCategoryIdList(null));

        when(categoryMapper.selectListByParentIds(Collections.singleton(1L)))
                .thenReturn(List.of(WmsItemCategoryDO.builder().id(2L).build(),
                        WmsItemCategoryDO.builder().id(3L).build()));
        when(categoryMapper.selectListByParentIds(Set.of(2L, 3L))).thenReturn(Collections.emptyList());

        Set<Long> ids = categoryService.getSelfAndChildItemCategoryIdList(1L);
        assertEquals(3, ids.size());
        assertTrue(ids.containsAll(List.of(1L, 2L, 3L)));
    }

    @Test
    public void testQueryMethods() {
        when(categoryMapper.selectById(1L)).thenReturn(WmsItemCategoryDO.builder().id(1L).build());
        when(categoryMapper.selectList(any(WmsItemCategoryListReqVO.class)))
                .thenReturn(List.of(WmsItemCategoryDO.builder().id(1L).build()));
        when(categoryMapper.selectByIds(anyCollection()))
                .thenReturn(List.of(WmsItemCategoryDO.builder().id(1L).build()));

        assertNotNull(categoryService.getItemCategory(1L));
        assertEquals(1, categoryService.getItemCategoryList(new WmsItemCategoryListReqVO()).size());
        assertEquals(1, categoryService.getItemCategoryList(List.of(1L)).size());
        assertTrue(categoryService.getItemCategoryList(Collections.emptyList()).isEmpty());
    }

}
