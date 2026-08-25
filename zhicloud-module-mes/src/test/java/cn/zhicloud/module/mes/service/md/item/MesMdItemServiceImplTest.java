package cn.zhicloud.module.mes.service.md.item;

import cn.zhicloud.framework.common.enums.CommonStatusEnum;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.mes.controller.admin.md.item.vo.MesMdItemImportExcelVO;
import cn.zhicloud.module.mes.controller.admin.md.item.vo.MesMdItemImportRespVO;
import cn.zhicloud.module.mes.controller.admin.md.item.vo.MesMdItemPageReqVO;
import cn.zhicloud.module.mes.controller.admin.md.item.vo.MesMdItemSaveReqVO;
import cn.zhicloud.module.mes.dal.dataobject.md.item.MesMdItemBatchConfigDO;
import cn.zhicloud.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.zhicloud.module.mes.dal.dataobject.md.item.MesMdItemTypeDO;
import cn.zhicloud.module.mes.dal.dataobject.md.item.MesMdProductBomDO;
import cn.zhicloud.module.mes.dal.dataobject.md.unitmeasure.MesMdUnitMeasureDO;
import cn.zhicloud.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.zhicloud.module.mes.enums.md.MesMdItemTypeEnum;
import cn.zhicloud.module.mes.enums.wm.BarcodeBizTypeEnum;
import cn.zhicloud.module.mes.service.md.autocode.MesMdAutoCodeRecordService;
import cn.zhicloud.module.mes.service.md.unitmeasure.MesMdUnitMeasureService;
import cn.zhicloud.module.mes.service.wm.barcode.MesWmBarcodeService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link MesMdItemServiceImpl} 的单元测试（mock mapper，无需 H2 表）
 */
@Import(MesMdItemServiceImpl.class)
public class MesMdItemServiceImplTest extends BaseDbUnitTest {

    @MockitoBean
    private MesMdItemMapper itemMapper;
    @MockitoBean
    private MesMdItemTypeService itemTypeService;
    @MockitoBean
    private MesMdUnitMeasureService unitMeasureService;
    @MockitoBean
    private MesMdItemBatchConfigService itemBatchConfigService;
    @MockitoBean
    private MesMdProductBomService productBomService;
    @MockitoBean
    private MesMdProductSopService productSopService;
    @MockitoBean
    private MesMdProductSipService productSipService;
    @MockitoBean
    private MesWmBarcodeService barcodeService;
    @MockitoBean
    private MesMdAutoCodeRecordService autoCodeRecordService;

    @Resource
    private MesMdItemServiceImpl itemService;

    @BeforeEach
    public void setUp() {
        // insert 时回填主键
        doAnswer(inv -> {
            MesMdItemDO item = inv.getArgument(0);
            item.setId(item.getId() == null ? 100L : item.getId());
            return 1;
        }).when(itemMapper).insert(any(MesMdItemDO.class));
        when(itemMapper.updateById(any(MesMdItemDO.class))).thenReturn(1);
        when(itemMapper.deleteById(anyLong())).thenReturn(1);
        // 物料分类：默认存在，且为叶子分类、物料类型
        when(itemTypeService.getItemType(anyLong())).thenReturn(buildItemType(MesMdItemTypeEnum.ITEM.getValue()));
        when(itemTypeService.getItemTypeChildrenList(anyLong())).thenReturn(Collections.emptyList());
        // 计量单位：默认存在
        when(unitMeasureService.getUnitMeasure(anyLong())).thenReturn(buildUnitMeasure());
        when(unitMeasureService.getUnitMeasureByCode(anyString())).thenReturn(buildUnitMeasure());
        // 自动编码
        when(autoCodeRecordService.generateAutoCode(anyString())).thenReturn("AUTO001");
    }

    private MesMdItemTypeDO buildItemType(String itemOrProduct) {
        return new MesMdItemTypeDO().setId(10L).setCode("T001").setName("分类A")
                .setItemOrProduct(itemOrProduct).setStatus(CommonStatusEnum.ENABLE.getStatus());
    }

    private MesMdUnitMeasureDO buildUnitMeasure() {
        return new MesMdUnitMeasureDO().setId(200L).setCode("PCS").setName("个")
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
    }

    private MesMdItemDO buildItem() {
        return new MesMdItemDO().setId(100L).setCode("I001").setName("物料A")
                .setItemTypeId(10L).setUnitMeasureId(200L)
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
    }

    private MesMdItemSaveReqVO buildSaveReq() {
        return new MesMdItemSaveReqVO().setCode("I001").setName("物料A")
                .setItemTypeId(10L).setUnitMeasureId(200L);
    }

    private MesMdItemImportExcelVO buildImportVO() {
        return MesMdItemImportExcelVO.builder().code("I001").name("物料A")
                .unitMeasureCode("PCS").itemTypeId(10L).build();
    }

    // ========== createItem ==========

    @Test
    public void testCreateItem_success() {
        Long id = itemService.createItem(buildSaveReq());
        assertEquals(100L, id);
        verify(itemMapper).insert(any(MesMdItemDO.class));
        verify(barcodeService).autoGenerateBarcode(eq(BarcodeBizTypeEnum.ITEM.getValue()),
                eq(100L), eq("I001"), eq("物料A"));
    }

    @Test
    public void testCreateItem_defaultDisableAndClearStock() {
        MesMdItemSaveReqVO reqVO = buildSaveReq().setSafeStockFlag(false)
                .setMinStock(new BigDecimal("10")).setMaxStock(new BigDecimal("99"));
        itemService.createItem(reqVO);

        ArgumentCaptor<MesMdItemDO> captor = ArgumentCaptor.forClass(MesMdItemDO.class);
        verify(itemMapper).insert(captor.capture());
        MesMdItemDO inserted = captor.getValue();
        assertEquals(CommonStatusEnum.DISABLE.getStatus(), inserted.getStatus());
        assertEquals(0, inserted.getMinStock().compareTo(BigDecimal.ZERO));
        assertEquals(0, inserted.getMaxStock().compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testCreateItem_keepStockWhenSafeStockFlag() {
        MesMdItemSaveReqVO reqVO = buildSaveReq().setSafeStockFlag(true)
                .setMinStock(new BigDecimal("10")).setMaxStock(new BigDecimal("99"));
        itemService.createItem(reqVO);

        ArgumentCaptor<MesMdItemDO> captor = ArgumentCaptor.forClass(MesMdItemDO.class);
        verify(itemMapper).insert(captor.capture());
        MesMdItemDO inserted = captor.getValue();
        assertEquals(0, inserted.getMinStock().compareTo(new BigDecimal("10")));
        assertEquals(0, inserted.getMaxStock().compareTo(new BigDecimal("99")));
    }

    @Test
    public void testCreateItem_codeDuplicate() {
        when(itemMapper.selectByCode("I001")).thenReturn(buildItem());
        assertServiceException(() -> itemService.createItem(buildSaveReq()), MD_ITEM_CODE_DUPLICATE);
    }

    @Test
    public void testCreateItem_nameDuplicate() {
        when(itemMapper.selectByName("物料A")).thenReturn(buildItem());
        assertServiceException(() -> itemService.createItem(buildSaveReq()), MD_ITEM_NAME_DUPLICATE);
    }

    @Test
    public void testCreateItem_itemTypeNotExists() {
        when(itemTypeService.getItemType(10L)).thenReturn(null);
        assertServiceException(() -> itemService.createItem(buildSaveReq()), MD_ITEM_TYPE_NOT_EXISTS);
    }

    @Test
    public void testCreateItem_itemTypeNotLeaf() {
        when(itemTypeService.getItemTypeChildrenList(10L))
                .thenReturn(Collections.singletonList(buildItemType(MesMdItemTypeEnum.ITEM.getValue())));
        assertServiceException(() -> itemService.createItem(buildSaveReq()), MD_ITEM_TYPE_NOT_LEAF);
    }

    @Test
    public void testCreateItem_unitMeasureNotExists() {
        when(unitMeasureService.getUnitMeasure(200L)).thenReturn(null);
        assertServiceException(() -> itemService.createItem(buildSaveReq()), MD_UNIT_MEASURE_NOT_EXISTS);
    }

    // ========== updateItem ==========

    @Test
    public void testUpdateItem_success() {
        when(itemMapper.selectById(100L)).thenReturn(buildItem());
        itemService.updateItem(buildSaveReq().setId(100L).setName("物料B"));
        verify(itemMapper).updateById(any(MesMdItemDO.class));
    }

    @Test
    public void testUpdateItem_notExists() {
        when(itemMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> itemService.updateItem(buildSaveReq().setId(100L)), MD_ITEM_NOT_EXISTS);
    }

    @Test
    public void testUpdateItem_codeDuplicateOnOther() {
        when(itemMapper.selectById(100L)).thenReturn(buildItem());
        when(itemMapper.selectByCode("I001")).thenReturn(buildItem().setId(101L));
        assertServiceException(() -> itemService.updateItem(buildSaveReq().setId(100L)), MD_ITEM_CODE_DUPLICATE);
    }

    @Test
    public void testUpdateItem_codeSelfNotDuplicate() {
        when(itemMapper.selectById(100L)).thenReturn(buildItem());
        when(itemMapper.selectByCode("I001")).thenReturn(buildItem());
        when(itemMapper.selectByName("物料A")).thenReturn(buildItem());
        itemService.updateItem(buildSaveReq().setId(100L));
        verify(itemMapper).updateById(any(MesMdItemDO.class));
    }

    // ========== updateItemStatus ==========

    @Test
    public void testUpdateItemStatus_disable() {
        when(itemMapper.selectById(100L)).thenReturn(buildItem());
        itemService.updateItemStatus(100L, CommonStatusEnum.DISABLE.getStatus());
        verify(itemMapper).updateById(any(MesMdItemDO.class));
        verify(itemBatchConfigService, never()).getItemBatchConfigByItemId(anyLong());
    }

    @Test
    public void testUpdateItemStatus_enableSuccess() {
        when(itemMapper.selectById(100L)).thenReturn(buildItem());
        itemService.updateItemStatus(100L, CommonStatusEnum.ENABLE.getStatus());
        verify(itemMapper).updateById(any(MesMdItemDO.class));
    }

    @Test
    public void testUpdateItemStatus_enableBatchConfigNotExists() {
        when(itemMapper.selectById(100L)).thenReturn(buildItem().setBatchFlag(true));
        when(itemBatchConfigService.getItemBatchConfigByItemId(100L)).thenReturn(null);
        assertServiceException(() -> itemService.updateItemStatus(100L, CommonStatusEnum.ENABLE.getStatus()),
                MD_ITEM_BATCH_CONFIG_NOT_EXISTS);
    }

    @Test
    public void testUpdateItemStatus_enableBatchConfigNoFlag() {
        when(itemMapper.selectById(100L)).thenReturn(buildItem().setBatchFlag(true));
        when(itemBatchConfigService.getItemBatchConfigByItemId(100L))
                .thenReturn(new MesMdItemBatchConfigDO().setId(1L).setItemId(100L));
        assertServiceException(() -> itemService.updateItemStatus(100L, CommonStatusEnum.ENABLE.getStatus()),
                MD_ITEM_BATCH_CONFIG_AT_LEAST_ONE_FLAG);
    }

    @Test
    public void testUpdateItemStatus_enableBatchConfigHasFlag() {
        when(itemMapper.selectById(100L)).thenReturn(buildItem().setBatchFlag(true));
        when(itemBatchConfigService.getItemBatchConfigByItemId(100L))
                .thenReturn(new MesMdItemBatchConfigDO().setId(1L).setItemId(100L).setLotNumberFlag(true));
        itemService.updateItemStatus(100L, CommonStatusEnum.ENABLE.getStatus());
        verify(itemMapper).updateById(any(MesMdItemDO.class));
    }

    @Test
    public void testUpdateItemStatus_enableProductBomRequired() {
        when(itemMapper.selectById(100L)).thenReturn(buildItem());
        when(itemTypeService.getItemType(10L)).thenReturn(buildItemType(MesMdItemTypeEnum.PRODUCT.getValue()));
        when(productBomService.getProductBomListByItemId(100L)).thenReturn(Collections.emptyList());
        assertServiceException(() -> itemService.updateItemStatus(100L, CommonStatusEnum.ENABLE.getStatus()),
                MD_ITEM_PRODUCT_BOM_REQUIRED);
    }

    @Test
    public void testUpdateItemStatus_enableProductWithBom() {
        when(itemMapper.selectById(100L)).thenReturn(buildItem());
        when(itemTypeService.getItemType(10L)).thenReturn(buildItemType(MesMdItemTypeEnum.PRODUCT.getValue()));
        when(productBomService.getProductBomListByItemId(100L))
                .thenReturn(Collections.singletonList(new MesMdProductBomDO().setId(1L).setItemId(100L)));
        itemService.updateItemStatus(100L, CommonStatusEnum.ENABLE.getStatus());
        verify(itemMapper).updateById(any(MesMdItemDO.class));
    }

    @Test
    public void testUpdateItemStatus_notExists() {
        when(itemMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> itemService.updateItemStatus(100L, CommonStatusEnum.ENABLE.getStatus()),
                MD_ITEM_NOT_EXISTS);
    }

    // ========== deleteItem ==========

    @Test
    public void testDeleteItem_success() {
        when(itemMapper.selectById(100L)).thenReturn(buildItem());
        itemService.deleteItem(100L);
        verify(itemBatchConfigService).deleteItemBatchConfigByItemId(100L);
        verify(productBomService).deleteProductBomByItemId(100L);
        verify(productSopService).deleteProductSopByItemId(100L);
        verify(productSipService).deleteProductSipByItemId(100L);
        verify(itemMapper).deleteById(100L);
    }

    @Test
    public void testDeleteItem_notExists() {
        when(itemMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> itemService.deleteItem(100L), MD_ITEM_NOT_EXISTS);
        verify(itemMapper, never()).deleteById(anyLong());
    }

    // ========== validate ==========

    @Test
    public void testValidateItemExists_exists() {
        when(itemMapper.selectById(100L)).thenReturn(buildItem());
        assertNotNull(itemService.validateItemExists(100L));
    }

    @Test
    public void testValidateItemExists_notExists() {
        when(itemMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> itemService.validateItemExists(100L), MD_ITEM_NOT_EXISTS);
    }

    @Test
    public void testValidateItemExistsAndEnable_enabled() {
        when(itemMapper.selectById(100L)).thenReturn(buildItem());
        assertNotNull(itemService.validateItemExistsAndEnable(100L));
    }

    @Test
    public void testValidateItemExistsAndEnable_disabled() {
        when(itemMapper.selectById(100L)).thenReturn(buildItem().setStatus(CommonStatusEnum.DISABLE.getStatus()));
        assertServiceException(() -> itemService.validateItemExistsAndEnable(100L), MD_ITEM_IS_DISABLE);
    }

    // ========== get / list / page / count ==========

    @Test
    public void testGetItem_exists() {
        when(itemMapper.selectById(100L)).thenReturn(buildItem());
        assertNotNull(itemService.getItem(100L));
    }

    @Test
    public void testGetItem_notExists() {
        when(itemMapper.selectById(100L)).thenReturn(null);
        assertNull(itemService.getItem(100L));
    }

    @Test
    public void testGetItemPage_withoutItemType() {
        PageResult<MesMdItemDO> page = new PageResult<>(Collections.singletonList(buildItem()), 1L);
        when(itemMapper.selectPage(any(MesMdItemPageReqVO.class), ArgumentMatchers.<Collection<Long>>any()))
                .thenReturn(page);
        PageResult<MesMdItemDO> result = itemService.getItemPage(new MesMdItemPageReqVO());
        assertEquals(1L, result.getTotal());
        verify(itemTypeService, never()).getItemTypeChildrenList(anyLong());
    }

    @Test
    public void testGetItemPage_withItemType() {
        PageResult<MesMdItemDO> page = new PageResult<>(Collections.emptyList(), 0L);
        when(itemMapper.selectPage(any(MesMdItemPageReqVO.class), ArgumentMatchers.<Collection<Long>>any()))
                .thenReturn(page);
        when(itemTypeService.getItemTypeChildrenList(10L))
                .thenReturn(Collections.singletonList(buildItemType(MesMdItemTypeEnum.ITEM.getValue()).setId(11L)));

        MesMdItemPageReqVO reqVO = new MesMdItemPageReqVO();
        reqVO.setItemTypeId(10L);
        PageResult<MesMdItemDO> result = itemService.getItemPage(reqVO);
        assertEquals(0L, result.getTotal());
        verify(itemTypeService).getItemTypeChildrenList(10L);
    }

    @Test
    public void testGetItemList_empty() {
        assertTrue(itemService.getItemList(Collections.emptyList()).isEmpty());
        verify(itemMapper, never()).selectByIds(any());
    }

    @Test
    public void testGetItemList_nonEmpty() {
        when(itemMapper.selectByIds(any())).thenReturn(Arrays.asList(buildItem()));
        List<MesMdItemDO> list = itemService.getItemList(Arrays.asList(100L));
        assertEquals(1, list.size());
    }

    @Test
    public void testGetItemCountByItemTypeId() {
        when(itemMapper.selectCountByItemTypeId(10L)).thenReturn(3L);
        assertEquals(3L, itemService.getItemCountByItemTypeId(10L));
    }

    @Test
    public void testGetItemCountByUnitMeasureId() {
        when(itemMapper.selectCountByUnitMeasureId(200L)).thenReturn(0L);
        assertEquals(0L, itemService.getItemCountByUnitMeasureId(200L));
    }

    // ========== importItemList ==========

    @Test
    public void testImportItemList_empty() {
        assertServiceException(() -> itemService.importItemList(Collections.emptyList(), true),
                MD_ITEM_IMPORT_LIST_IS_EMPTY);
    }

    @Test
    public void testImportItemList_create() {
        MesMdItemImportRespVO resp = itemService.importItemList(Arrays.asList(buildImportVO()), true);
        assertEquals(1, resp.getCreateCodes().size());
        assertTrue(resp.getFailureCodes().isEmpty());
        verify(itemMapper).insert(any(MesMdItemDO.class));
        verify(barcodeService).autoGenerateBarcode(eq(BarcodeBizTypeEnum.ITEM.getValue()),
                eq(100L), eq("I001"), eq("物料A"));
    }

    @Test
    public void testImportItemList_blankCodeAutoGenerate() {
        MesMdItemImportExcelVO vo = MesMdItemImportExcelVO.builder().name("物料A")
                .unitMeasureCode("PCS").itemTypeId(10L).build();
        MesMdItemImportRespVO resp = itemService.importItemList(Arrays.asList(vo), true);
        assertEquals(1, resp.getCreateCodes().size());
        assertEquals("AUTO001", resp.getCreateCodes().get(0));
    }

    @Test
    public void testImportItemList_blankName() {
        MesMdItemImportExcelVO vo = MesMdItemImportExcelVO.builder().code("I001")
                .unitMeasureCode("PCS").itemTypeId(10L).build();
        MesMdItemImportRespVO resp = itemService.importItemList(Arrays.asList(vo), true);
        assertEquals(1, resp.getFailureCodes().size());
        assertEquals("物料名称不能为空", resp.getFailureCodes().get("I001"));
    }

    @Test
    public void testImportItemList_blankUnitMeasureCode() {
        MesMdItemImportExcelVO vo = MesMdItemImportExcelVO.builder().code("I001").name("物料A")
                .itemTypeId(10L).build();
        MesMdItemImportRespVO resp = itemService.importItemList(Arrays.asList(vo), true);
        assertEquals(1, resp.getFailureCodes().size());
        assertEquals("单位编码不能为空", resp.getFailureCodes().get("I001"));
    }

    @Test
    public void testImportItemList_unitMeasureNotExists() {
        when(unitMeasureService.getUnitMeasureByCode("PCS")).thenReturn(null);
        MesMdItemImportRespVO resp = itemService.importItemList(Arrays.asList(buildImportVO()), true);
        assertEquals(1, resp.getFailureCodes().size());
        verify(itemMapper, never()).insert(any(MesMdItemDO.class));
    }

    @Test
    public void testImportItemList_itemTypeIdNull() {
        MesMdItemImportExcelVO vo = MesMdItemImportExcelVO.builder().code("I001").name("物料A")
                .unitMeasureCode("PCS").build();
        MesMdItemImportRespVO resp = itemService.importItemList(Arrays.asList(vo), true);
        assertEquals(1, resp.getFailureCodes().size());
        assertEquals("物料分类编号不能为空", resp.getFailureCodes().get("I001"));
    }

    @Test
    public void testImportItemList_itemTypeNotExists() {
        when(itemTypeService.getItemType(10L)).thenReturn(null);
        MesMdItemImportRespVO resp = itemService.importItemList(Arrays.asList(buildImportVO()), true);
        assertEquals(1, resp.getFailureCodes().size());
        verify(itemMapper, never()).insert(any(MesMdItemDO.class));
    }

    @Test
    public void testImportItemList_nameDuplicate() {
        when(itemMapper.selectByName("物料A")).thenReturn(buildItem());
        MesMdItemImportRespVO resp = itemService.importItemList(Arrays.asList(buildImportVO()), true);
        assertEquals(1, resp.getFailureCodes().size());
        verify(itemMapper, never()).insert(any(MesMdItemDO.class));
    }

    @Test
    public void testImportItemList_update() {
        when(itemMapper.selectByCode("I001")).thenReturn(buildItem());
        MesMdItemImportRespVO resp = itemService.importItemList(Arrays.asList(buildImportVO()), true);
        assertEquals(1, resp.getUpdateCodes().size());
        verify(itemMapper).updateById(any(MesMdItemDO.class));
    }

    @Test
    public void testImportItemList_updateNameDuplicate() {
        when(itemMapper.selectByCode("I001")).thenReturn(buildItem());
        when(itemMapper.selectByName("物料A")).thenReturn(buildItem().setId(101L));
        MesMdItemImportRespVO resp = itemService.importItemList(Arrays.asList(buildImportVO()), true);
        assertEquals(1, resp.getFailureCodes().size());
        verify(itemMapper, never()).updateById(any(MesMdItemDO.class));
    }

    @Test
    public void testImportItemList_noUpdateSupport() {
        when(itemMapper.selectByCode("I001")).thenReturn(buildItem());
        MesMdItemImportRespVO resp = itemService.importItemList(Arrays.asList(buildImportVO()), false);
        assertEquals(1, resp.getFailureCodes().size());
        assertEquals("物料编码已存在", resp.getFailureCodes().get("I001"));
        verify(itemMapper, never()).updateById(any(MesMdItemDO.class));
    }

}
