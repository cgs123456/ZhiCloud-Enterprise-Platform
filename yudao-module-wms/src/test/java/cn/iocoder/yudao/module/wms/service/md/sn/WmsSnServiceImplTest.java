package cn.iocoder.yudao.module.wms.service.md.sn;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.wms.controller.admin.md.sn.vo.WmsSnGenerateReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.md.sn.vo.WmsSnSaveReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.md.sn.vo.WmsSnTraceRespVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.item.WmsItemDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.sn.WmsSnDO;
import cn.iocoder.yudao.module.wms.dal.mysql.md.sn.WmsSnMapper;
import cn.iocoder.yudao.module.wms.enums.md.WmsSnStatusEnum;
import cn.iocoder.yudao.module.wms.service.md.item.WmsItemService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.ITEM_NOT_EXISTS;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.SN_DUPLICATE;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.SN_GENERATE_QUANTITY_INVALID;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.SN_NOT_BOUND;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.SN_NOT_EXISTS;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.SN_STATUS_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@Import(WmsSnServiceImpl.class)
public class WmsSnServiceImplTest extends BaseDbUnitTest {

    @Resource
    private WmsSnServiceImpl snService;
    @Resource
    private WmsSnMapper snMapper;
    @MockitoBean
    private WmsItemService itemService;

    @Test
    public void testGenerateSnList_success() {
        when(itemService.getItem(100L)).thenReturn(new WmsItemDO());
        WmsSnGenerateReqVO reqVO = new WmsSnGenerateReqVO();
        reqVO.setQuantity(new BigDecimal("3"));
        reqVO.setProductId(100L);
        reqVO.setPrefix("SN");
        List<WmsSnDO> list = snService.generateSnList(reqVO);
        assertEquals(3, list.size());
        assertEquals(3, snMapper.selectList(null).size());
        assertEquals(WmsSnStatusEnum.GENERATED.getStatus(), list.get(0).getStatus());
    }

    @Test
    public void testGenerateSnList_invalidQuantity() {
        WmsSnGenerateReqVO reqVO = new WmsSnGenerateReqVO();
        reqVO.setQuantity(BigDecimal.ZERO);
        assertServiceException(() -> snService.generateSnList(reqVO), SN_GENERATE_QUANTITY_INVALID);
    }

    @Test
    public void testGenerateSnList_productNotExists() {
        WmsSnGenerateReqVO reqVO = new WmsSnGenerateReqVO();
        reqVO.setQuantity(new BigDecimal("1"));
        reqVO.setProductId(999L);
        when(itemService.getItem(999L)).thenReturn(null);
        assertServiceException(() -> snService.generateSnList(reqVO), ITEM_NOT_EXISTS);
    }

    @Test
    public void testCreateSn_success() {
        when(itemService.getItem(100L)).thenReturn(new WmsItemDO());
        WmsSnSaveReqVO reqVO = new WmsSnSaveReqVO();
        reqVO.setSn("SN202608010001");
        reqVO.setProductId(100L);
        Long id = snService.createSn(reqVO);
        assertNotNull(id);
        WmsSnDO db = snMapper.selectById(id);
        assertEquals("SN202608010001", db.getSn());
        assertEquals(WmsSnStatusEnum.GENERATED.getStatus(), db.getStatus());
    }

    @Test
    public void testCreateSn_duplicate() {
        when(itemService.getItem(100L)).thenReturn(new WmsItemDO());
        WmsSnSaveReqVO reqVO = new WmsSnSaveReqVO();
        reqVO.setSn("DUP");
        reqVO.setProductId(100L);
        snService.createSn(reqVO);
        assertServiceException(() -> snService.createSn(reqVO), SN_DUPLICATE, "DUP");
    }

    @Test
    public void testUpdateSn_success() {
        when(itemService.getItem(100L)).thenReturn(new WmsItemDO());
        WmsSnSaveReqVO create = new WmsSnSaveReqVO();
        create.setSn("U1");
        create.setProductId(100L);
        Long id = snService.createSn(create);
        WmsSnSaveReqVO update = new WmsSnSaveReqVO();
        update.setId(id);
        update.setSn("U1U");
        update.setProductId(100L);
        snService.updateSn(update);
        assertEquals("U1U", snMapper.selectById(id).getSn());
    }

    @Test
    public void testUpdateSn_notExists() {
        WmsSnSaveReqVO update = new WmsSnSaveReqVO();
        update.setId(999L);
        update.setSn("X");
        assertServiceException(() -> snService.updateSn(update), SN_NOT_EXISTS);
    }

    @Test
    public void testDeleteSn_success() {
        when(itemService.getItem(100L)).thenReturn(new WmsItemDO());
        WmsSnSaveReqVO create = new WmsSnSaveReqVO();
        create.setSn("D1");
        create.setProductId(100L);
        Long id = snService.createSn(create);
        snService.deleteSn(id);
        assertNull(snMapper.selectById(id));
    }

    @Test
    public void testDeleteSn_statusInvalid() {
        when(itemService.getItem(100L)).thenReturn(new WmsItemDO());
        WmsSnSaveReqVO create = new WmsSnSaveReqVO();
        create.setSn("D2");
        create.setProductId(100L);
        Long id = snService.createSn(create);
        WmsSnDO upd = new WmsSnDO();
        upd.setId(id);
        upd.setStatus(WmsSnStatusEnum.IN_STOCK.getStatus());
        snMapper.updateById(upd);
        assertServiceException(() -> snService.deleteSn(id), SN_STATUS_INVALID, WmsSnStatusEnum.IN_STOCK.getStatus());
    }

    @Test
    public void testGetSn() {
        when(itemService.getItem(100L)).thenReturn(new WmsItemDO());
        WmsSnSaveReqVO create = new WmsSnSaveReqVO();
        create.setSn("G1");
        create.setProductId(100L);
        Long id = snService.createSn(create);
        assertNotNull(snService.getSn(id));
        assertNotNull(snService.getSnBySn("G1"));
        assertNull(snService.getSn(999L));
        assertNull(snService.getSnBySn("NO_SUCH"));
    }

    @Test
    public void testGetSnList_empty() {
        assertTrue(snService.getSnList(null).isEmpty());
        assertTrue(snService.getSnList(List.of()).isEmpty());
    }

    @Test
    public void testBindInventory_success() {
        when(itemService.getItem(100L)).thenReturn(new WmsItemDO());
        WmsSnSaveReqVO create = new WmsSnSaveReqVO();
        create.setSn("B1");
        create.setProductId(100L);
        Long id = snService.createSn(create);
        snService.bindInventory(id, 10L, 20L, 30L, 40L, 50L, 60L);
        WmsSnDO db = snMapper.selectById(id);
        assertEquals(50L, db.getLocationId());
        assertEquals(WmsSnStatusEnum.IN_STOCK.getStatus(), db.getStatus());
        assertNotNull(db.getBoundTime());
    }

    @Test
    public void testBindInventory_noLocation() {
        when(itemService.getItem(100L)).thenReturn(new WmsItemDO());
        WmsSnSaveReqVO create = new WmsSnSaveReqVO();
        create.setSn("B2");
        create.setProductId(100L);
        Long id = snService.createSn(create);
        snService.bindInventory(id, 10L, 20L, 30L, 40L, null, 60L);
        assertEquals(WmsSnStatusEnum.BOUND.getStatus(), snMapper.selectById(id).getStatus());
    }

    @Test
    public void testBindInventory_statusInvalid() {
        when(itemService.getItem(100L)).thenReturn(new WmsItemDO());
        WmsSnSaveReqVO create = new WmsSnSaveReqVO();
        create.setSn("B3");
        create.setProductId(100L);
        Long id = snService.createSn(create);
        WmsSnDO upd = new WmsSnDO();
        upd.setId(id);
        upd.setStatus(WmsSnStatusEnum.SHIPPED.getStatus());
        snMapper.updateById(upd);
        assertServiceException(() -> snService.bindInventory(id, 1L, 2L, 3L, 4L, 5L, 6L), SN_STATUS_INVALID, WmsSnStatusEnum.SHIPPED.getStatus());
    }

    @Test
    public void testUnbindAndShip_success() {
        when(itemService.getItem(100L)).thenReturn(new WmsItemDO());
        WmsSnSaveReqVO create = new WmsSnSaveReqVO();
        create.setSn("S1");
        create.setProductId(100L);
        Long id = snService.createSn(create);
        snService.bindInventory(id, 1L, 2L, 3L, 4L, 5L, 6L);
        snService.unbindAndShip(id, 99L);
        WmsSnDO db = snMapper.selectById(id);
        assertEquals(WmsSnStatusEnum.SHIPPED.getStatus(), db.getStatus());
        assertEquals(99L, db.getOutboundOrderId());
        assertNotNull(db.getShippedTime());
    }

    @Test
    public void testUnbindAndShip_notBound() {
        when(itemService.getItem(100L)).thenReturn(new WmsItemDO());
        WmsSnSaveReqVO create = new WmsSnSaveReqVO();
        create.setSn("S2");
        create.setProductId(100L);
        Long id = snService.createSn(create);
        assertServiceException(() -> snService.unbindAndShip(id, 99L), SN_NOT_BOUND);
    }

    @Test
    public void testReturnSn_success() {
        when(itemService.getItem(100L)).thenReturn(new WmsItemDO());
        WmsSnSaveReqVO create = new WmsSnSaveReqVO();
        create.setSn("R1");
        create.setProductId(100L);
        Long id = snService.createSn(create);
        snService.bindInventory(id, 1L, 2L, 3L, 4L, 5L, 6L);
        snService.unbindAndShip(id, 99L);
        snService.returnSn(id, 7L, 8L);
        WmsSnDO db = snMapper.selectById(id);
        assertEquals(WmsSnStatusEnum.RETURNED.getStatus(), db.getStatus());
        assertEquals(7L, db.getWarehouseId());
    }

    @Test
    public void testReturnSn_statusInvalid() {
        when(itemService.getItem(100L)).thenReturn(new WmsItemDO());
        WmsSnSaveReqVO create = new WmsSnSaveReqVO();
        create.setSn("R2");
        create.setProductId(100L);
        Long id = snService.createSn(create);
        assertServiceException(() -> snService.returnSn(id, 7L, 8L), SN_STATUS_INVALID, WmsSnStatusEnum.GENERATED.getStatus());
    }

    @Test
    public void testTrace_success() {
        WmsItemDO item = new WmsItemDO().setCode("PCODE").setName("PNAME");
        when(itemService.getItem(100L)).thenReturn(item);
        WmsSnSaveReqVO create = new WmsSnSaveReqVO();
        create.setSn("T1");
        create.setProductId(100L);
        Long id = snService.createSn(create);
        WmsSnTraceRespVO resp = snService.trace(id);
        assertEquals("PCODE", resp.getProductCode());
        assertEquals("PNAME", resp.getProductName());
    }

    @Test
    public void testTrace_itemNull() {
        when(itemService.getItem(100L)).thenReturn(new WmsItemDO());
        WmsSnSaveReqVO create = new WmsSnSaveReqVO();
        create.setSn("T2");
        create.setProductId(100L);
        Long id = snService.createSn(create);
        // 创建成功后，模拟商品已删除，验证 trace 不抛异常且商品信息为空
        when(itemService.getItem(100L)).thenReturn(null);
        WmsSnTraceRespVO resp = snService.trace(id);
        assertNull(resp.getProductCode());
    }

}
