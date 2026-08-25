package cn.zhicloud.module.wms.service.inventory.batch;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.wms.controller.admin.inventory.batch.vo.WmsInventoryBatchSaveReqVO;
import cn.zhicloud.module.wms.controller.admin.inventory.batch.vo.WmsInventoryBatchStrategyRespVO;
import cn.zhicloud.module.wms.dal.dataobject.inventory.WmsInventoryBatchDO;
import cn.zhicloud.module.wms.dal.dataobject.inventory.WmsInventoryDO;
import cn.zhicloud.module.wms.dal.mysql.inventory.WmsInventoryBatchMapper;
import cn.zhicloud.module.wms.dal.mysql.inventory.WmsInventoryMapper;
import cn.zhicloud.module.wms.enums.inventory.WmsInventoryBatchStatusEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.INVENTORY_BATCH_NOT_EXISTS;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.INVENTORY_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(WmsInventoryBatchServiceImpl.class)
public class WmsInventoryBatchServiceImplTest extends BaseDbUnitTest {

    @Resource
    private WmsInventoryBatchServiceImpl batchService;
    @Resource
    private WmsInventoryBatchMapper batchMapper;
    @Resource
    private WmsInventoryMapper inventoryMapper;

    private Long insertInventory() {
        WmsInventoryDO inv = new WmsInventoryDO();
        inv.setSkuId(200L);
        inv.setWarehouseId(100L);
        inv.setQuantity(new BigDecimal("100"));
        inventoryMapper.insert(inv);
        return inv.getId();
    }

    private WmsInventoryBatchDO buildBatch(Long inventoryId, String batchNo, BigDecimal qty) {
        return WmsInventoryBatchDO.builder()
                .inventoryId(inventoryId)
                .batchNo(batchNo)
                .quantity(qty)
                .lockedQuantity(BigDecimal.ZERO)
                .status(WmsInventoryBatchStatusEnum.AVAILABLE.getStatus())
                .build();
    }

    @Test
    public void testCreateBatch_success() {
        Long invId = insertInventory();
        WmsInventoryBatchSaveReqVO req = new WmsInventoryBatchSaveReqVO();
        req.setInventoryId(invId);
        req.setBatchNo("B001");
        req.setQuantity(new BigDecimal("10"));
        Long id = batchService.createBatch(req);
        assertNotNull(id);
        WmsInventoryBatchDO db = batchMapper.selectById(id);
        assertEquals("B001", db.getBatchNo());
        assertEquals(WmsInventoryBatchStatusEnum.AVAILABLE.getStatus(), db.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(db.getLockedQuantity()));
    }

    @Test
    public void testCreateBatch_inventoryNotExists() {
        WmsInventoryBatchSaveReqVO req = new WmsInventoryBatchSaveReqVO();
        req.setInventoryId(999L);
        req.setBatchNo("B002");
        req.setQuantity(new BigDecimal("10"));
        assertServiceException(() -> batchService.createBatch(req), INVENTORY_NOT_EXISTS);
    }

    @Test
    public void testCreateBatch_duplicateBatchNo() {
        Long invId = insertInventory();
        WmsInventoryBatchSaveReqVO req = new WmsInventoryBatchSaveReqVO();
        req.setInventoryId(invId);
        req.setBatchNo("DUP");
        req.setQuantity(new BigDecimal("10"));
        batchService.createBatch(req);
        assertServiceException(() -> batchService.createBatch(req), INVENTORY_BATCH_NOT_EXISTS);
    }

    @Test
    public void testUpdateBatch_success() {
        Long invId = insertInventory();
        WmsInventoryBatchSaveReqVO req = new WmsInventoryBatchSaveReqVO();
        req.setInventoryId(invId);
        req.setBatchNo("U1");
        req.setQuantity(new BigDecimal("10"));
        Long id = batchService.createBatch(req);
        WmsInventoryBatchSaveReqVO upd = new WmsInventoryBatchSaveReqVO();
        upd.setId(id);
        upd.setInventoryId(invId);
        upd.setBatchNo("U2");
        batchService.updateBatch(upd);
        assertEquals("U2", batchMapper.selectById(id).getBatchNo());
    }

    @Test
    public void testUpdateBatch_notExists() {
        WmsInventoryBatchSaveReqVO upd = new WmsInventoryBatchSaveReqVO();
        upd.setId(999L);
        upd.setInventoryId(1L);
        upd.setBatchNo("X");
        assertServiceException(() -> batchService.updateBatch(upd), INVENTORY_BATCH_NOT_EXISTS);
    }

    @Test
    public void testDeleteBatch_success() {
        Long invId = insertInventory();
        WmsInventoryBatchSaveReqVO req = new WmsInventoryBatchSaveReqVO();
        req.setInventoryId(invId);
        req.setBatchNo("D1");
        req.setQuantity(new BigDecimal("10"));
        Long id = batchService.createBatch(req);
        batchService.deleteBatch(id);
        assertNull(batchMapper.selectById(id));
    }

    @Test
    public void testDeleteBatch_notExists() {
        assertServiceException(() -> batchService.deleteBatch(999L), INVENTORY_BATCH_NOT_EXISTS);
    }

    @Test
    public void testGetBatch_and_list() {
        Long invId = insertInventory();
        WmsInventoryBatchSaveReqVO req = new WmsInventoryBatchSaveReqVO();
        req.setInventoryId(invId);
        req.setBatchNo("G1");
        req.setQuantity(new BigDecimal("10"));
        Long id = batchService.createBatch(req);
        assertNotNull(batchService.getBatch(id));
        assertNull(batchService.getBatch(999L));
        assertEquals(1, batchService.getBatchesByInventoryId(invId).size());
    }

    @Test
    public void testGetExpiringBatches() {
        Long invId = insertInventory();
        WmsInventoryBatchDO batch = buildBatch(invId, "EXP", new BigDecimal("10"));
        batch.setExpiryDate(LocalDate.now().plusDays(5));
        batchMapper.insert(batch);
        List<WmsInventoryBatchDO> expiring = batchService.getExpiringBatches(30);
        assertEquals(1, expiring.size());
    }

    @Test
    public void testGetExpiredBatches() {
        Long invId = insertInventory();
        WmsInventoryBatchDO batch = buildBatch(invId, "OLD", new BigDecimal("10"));
        batch.setExpiryDate(LocalDate.now().minusDays(10));
        batchMapper.insert(batch);
        List<WmsInventoryBatchDO> expired = batchService.getExpiredBatches();
        assertEquals(1, expired.size());
    }

    @Test
    public void testApplyFifoStrategy_success() {
        Long invId = insertInventory();
        WmsInventoryBatchDO b1 = buildBatch(invId, "F1", new BigDecimal("10"));
        b1.setProductionDate(LocalDate.now().minusDays(2));
        batchMapper.insert(b1);
        WmsInventoryBatchDO b2 = buildBatch(invId, "F2", new BigDecimal("10"));
        b2.setProductionDate(LocalDate.now());
        batchMapper.insert(b2);
        WmsInventoryBatchStrategyRespVO resp = batchService.applyFifoStrategy(invId, new BigDecimal("15"));
        assertTrue(resp.getSufficient());
        assertEquals(0, new BigDecimal("15").compareTo(resp.getAllocatedQuantity()));
        assertEquals(2, resp.getAllocations().size());
        assertEquals(b1.getId(), resp.getAllocations().get(0).getBatchId());
    }

    @Test
    public void testApplyFifoStrategy_insufficient() {
        Long invId = insertInventory();
        WmsInventoryBatchDO b1 = buildBatch(invId, "F1", new BigDecimal("5"));
        batchMapper.insert(b1);
        WmsInventoryBatchStrategyRespVO resp = batchService.applyFifoStrategy(invId, new BigDecimal("100"));
        assertFalse(resp.getSufficient());
        assertEquals(0, new BigDecimal("5").compareTo(resp.getAllocatedQuantity()));
    }

    @Test
    public void testApplyFefoStrategy_success() {
        Long invId = insertInventory();
        WmsInventoryBatchDO b1 = buildBatch(invId, "E1", new BigDecimal("10"));
        b1.setExpiryDate(LocalDate.now().plusDays(10));
        batchMapper.insert(b1);
        WmsInventoryBatchDO b2 = buildBatch(invId, "E2", new BigDecimal("10"));
        b2.setExpiryDate(LocalDate.now().plusDays(2));
        batchMapper.insert(b2);
        WmsInventoryBatchStrategyRespVO resp = batchService.applyFefoStrategy(invId, new BigDecimal("15"));
        assertTrue(resp.getSufficient());
        assertEquals(b2.getId(), resp.getAllocations().get(0).getBatchId());
    }

    @Test
    public void testApplyFifoStrategy_zeroQuantity() {
        Long invId = insertInventory();
        WmsInventoryBatchStrategyRespVO resp = batchService.applyFifoStrategy(invId, BigDecimal.ZERO);
        assertFalse(resp.getSufficient());
        assertEquals(BigDecimal.ZERO, resp.getAllocatedQuantity());
    }

}
