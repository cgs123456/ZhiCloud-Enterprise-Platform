package cn.iocoder.yudao.module.wms.service.pda;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.module.wms.controller.admin.inventory.batch.vo.WmsInventoryBatchStrategyRespVO;
import cn.iocoder.yudao.module.wms.controller.app.pda.vo.WmsPdaCheckReqVO;
import cn.iocoder.yudao.module.wms.controller.app.pda.vo.WmsPdaLoginReqVO;
import cn.iocoder.yudao.module.wms.controller.app.pda.vo.WmsPdaLoginRespVO;
import cn.iocoder.yudao.module.wms.controller.app.pda.vo.WmsPdaPickReqVO;
import cn.iocoder.yudao.module.wms.controller.app.pda.vo.WmsPdaPutawayReqVO;
import cn.iocoder.yudao.module.wms.controller.app.pda.vo.WmsPdaReceiptReqVO;
import cn.iocoder.yudao.module.wms.controller.app.pda.vo.WmsPdaScanReqVO;
import cn.iocoder.yudao.module.wms.controller.app.pda.vo.WmsPdaScanRespVO;
import cn.iocoder.yudao.module.wms.controller.app.pda.vo.WmsPdaTaskRespVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.inventory.WmsInventoryBatchDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.inventory.WmsInventoryDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.item.WmsItemDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.item.WmsItemSkuDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.warehouse.WmsWarehouseDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.check.WmsCheckOrderDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.check.WmsCheckOrderDetailDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.receipt.WmsReceiptOrderDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.receipt.WmsReceiptOrderDetailDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.shipment.WmsShipmentOrderDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.shipment.WmsShipmentOrderDetailDO;
import cn.iocoder.yudao.module.wms.dal.mysql.inventory.WmsInventoryBatchMapper;
import cn.iocoder.yudao.module.wms.dal.mysql.inventory.WmsInventoryMapper;
import cn.iocoder.yudao.module.wms.dal.mysql.md.item.WmsItemSkuMapper;
import cn.iocoder.yudao.module.wms.dal.mysql.md.warehouse.WmsWarehouseMapper;
import cn.iocoder.yudao.module.wms.dal.mysql.order.check.WmsCheckOrderMapper;
import cn.iocoder.yudao.module.wms.dal.mysql.order.receipt.WmsReceiptOrderMapper;
import cn.iocoder.yudao.module.wms.dal.mysql.order.shipment.WmsShipmentOrderMapper;
import cn.iocoder.yudao.module.wms.enums.inventory.WmsInventoryChangeTypeEnum;
import cn.iocoder.yudao.module.wms.enums.order.WmsOrderStatusEnum;
import cn.iocoder.yudao.module.wms.enums.order.WmsOrderTypeEnum;
import cn.iocoder.yudao.module.wms.service.inventory.WmsInventoryService;
import cn.iocoder.yudao.module.wms.service.inventory.batch.WmsInventoryBatchService;
import cn.iocoder.yudao.module.wms.service.inventory.dto.WmsInventoryChangeReqDTO;
import cn.iocoder.yudao.module.wms.service.inventory.dto.WmsInventoryCheckReqDTO;
import cn.iocoder.yudao.module.wms.service.inventory.slotting.WmsSlottingService;
import cn.iocoder.yudao.module.wms.service.md.item.WmsItemService;
import cn.iocoder.yudao.module.wms.service.md.item.WmsItemSkuService;
import cn.iocoder.yudao.module.wms.service.md.warehouse.WmsWarehouseService;
import cn.iocoder.yudao.module.wms.service.order.check.WmsCheckOrderDetailService;
import cn.iocoder.yudao.module.wms.service.order.receipt.WmsReceiptOrderDetailService;
import cn.iocoder.yudao.module.wms.service.order.receipt.WmsReceiptOrderService;
import cn.iocoder.yudao.module.wms.service.order.shipment.WmsShipmentOrderDetailService;
import cn.iocoder.yudao.module.wms.service.order.shipment.WmsShipmentOrderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.INVENTORY_BATCH_FEFO_INSUFFICIENT;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.INVENTORY_NOT_EXISTS;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.ITEM_SKU_NOT_EXISTS;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.RECEIPT_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.SHIPMENT_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.WAREHOUSE_NOT_EXISTS;

/**
 * WMS PDA 移动端 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class WmsPdaServiceImpl implements WmsPdaService {

    @Resource
    private cn.iocoder.yudao.module.system.service.auth.AdminAuthService authService;
    @Resource
    private WmsInventoryService inventoryService;
    @Resource
    private WmsReceiptOrderService receiptOrderService;
    @Resource
    private WmsReceiptOrderDetailService receiptOrderDetailService;
    @Resource
    private WmsShipmentOrderService shipmentOrderService;
    @Resource
    private WmsShipmentOrderDetailService shipmentOrderDetailService;
    @Resource
    private WmsCheckOrderDetailService checkOrderDetailService;
    @Resource
    private WmsItemSkuService itemSkuService;
    @Resource
    private WmsItemService itemService;
    @Resource
    private WmsWarehouseService warehouseService;

    @Resource
    private WmsInventoryMapper inventoryMapper;
    @Resource
    private WmsInventoryBatchMapper inventoryBatchMapper;
    @Resource
    private WmsItemSkuMapper itemSkuMapper;
    @Resource
    private WmsWarehouseMapper warehouseMapper;
    @Resource
    private WmsReceiptOrderMapper receiptOrderMapper;
    @Resource
    private WmsShipmentOrderMapper shipmentOrderMapper;
    @Resource
    private WmsCheckOrderMapper checkOrderMapper;
    @Resource
    private WmsInventoryBatchService inventoryBatchService;
    @Resource
    private WmsSlottingService slottingService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WmsPdaLoginRespVO login(WmsPdaLoginReqVO reqVO) {
        // 1. 账号密码认证
        cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO user =
                authService.authenticate(reqVO.getUsername(), reqVO.getPassword());
        // 2. 组装返回
        WmsPdaLoginRespVO respVO = new WmsPdaLoginRespVO();
        respVO.setUserId(user.getId());
        respVO.setUsername(user.getUsername());
        respVO.setNickname(user.getNickname());
        respVO.setDeptId(user.getDeptId());
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, readOnly = true)
    public List<WmsPdaTaskRespVO> getTaskList() {
        List<WmsPdaTaskRespVO> tasks = new ArrayList<>();
        // 1. 拣货任务：查询草稿状态的出库单明细
        tasks.addAll(buildPickTasks());
        // 2. 上架任务：查询草稿状态的入库单明细
        tasks.addAll(buildPutawayTasks());
        // 3. 盘点任务：查询草稿状态的盘库单明细
        tasks.addAll(buildCheckTasks());
        return tasks;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, readOnly = true)
    public WmsPdaScanRespVO scan(WmsPdaScanReqVO reqVO) {
        if (WmsPdaScanReqVO.SCAN_TYPE_LOCATION.equals(reqVO.getScanType())) {
            return scanLocation(reqVO);
        }
        if (WmsPdaScanReqVO.SCAN_TYPE_ITEM.equals(reqVO.getScanType())) {
            return scanItem(reqVO);
        }
        throw new IllegalArgumentException("未知扫码类型：" + reqVO.getScanType());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceipt(WmsPdaReceiptReqVO reqVO) {
        // 1. 校验入库单存在
        WmsReceiptOrderDO receiptOrder = receiptOrderMapper.selectById(reqVO.getReceiptOrderId());
        if (receiptOrder == null) {
            throw exception(RECEIPT_ORDER_NOT_EXISTS);
        }
        // 2. 校验 SKU 存在
        itemSkuService.validateItemSkuExists(reqVO.getSkuId());
        // 3. 增加库存（PDA 收货确认 = 入库）
        changeInventory(receiptOrder.getId(), receiptOrder.getNo(), WmsOrderTypeEnum.RECEIPT.getType(),
                receiptOrder.getWarehouseId(), reqVO.getSkuId(), reqVO.getQuantity(), reqVO.getRemark());
        log.info("[confirmReceipt][PDA 收货确认成功，入库单={} SKU={} 数量={}]",
                receiptOrder.getNo(), reqVO.getSkuId(), reqVO.getQuantity());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executePutaway(WmsPdaPutawayReqVO reqVO) {
        // 1. 校验仓库存在
        warehouseService.validateWarehouseExists(reqVO.getWarehouseId());
        // 2. 校验 SKU 存在
        itemSkuService.validateItemSkuExists(reqVO.getSkuId());
        // 3. 校验批次未过期（P0-13：FEFO 保质期管理）
        slottingService.validateBatchNotExpired(reqVO.getBatchNo(), reqVO.getExpiryDate());
        // 4. 增加库存三维字段（PDA 上架 = 入库，使用 IN 类型同步更新 available）
        changeInventoryTyped(0L, "PDA-PUTAWAY", WmsOrderTypeEnum.RECEIPT.getType(),
                reqVO.getWarehouseId(), reqVO.getSkuId(), reqVO.getQuantity(),
                WmsInventoryChangeTypeEnum.IN, reqVO.getRemark());
        // 5. 上架 Slotting：合并到已有批次或新建批次
        WmsInventoryDO inventory = inventoryMapper.selectBySkuIdAndWarehouseId(
                reqVO.getSkuId(), reqVO.getWarehouseId());
        if (inventory == null) {
            // 理论上 changeInventory(IN) 已创建库存行，此处兜底
            throw exception(INVENTORY_NOT_EXISTS);
        }
        slottingService.mergeOrCreateBatch(inventory, reqVO.getBatchNo(),
                reqVO.getProductionDate(), reqVO.getExpiryDate(), reqVO.getQuantity());
        log.info("[executePutaway][PDA 上架成功，仓库={} SKU={} 数量={} 批次={}]",
                reqVO.getWarehouseId(), reqVO.getSkuId(), reqVO.getQuantity(), reqVO.getBatchNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executePick(WmsPdaPickReqVO reqVO) {
        // 1. 校验出库单存在
        WmsShipmentOrderDO shipmentOrder = shipmentOrderMapper.selectById(reqVO.getShipmentOrderId());
        if (shipmentOrder == null) {
            throw exception(SHIPMENT_ORDER_NOT_EXISTS);
        }
        // 2. 校验 SKU 存在
        itemSkuService.validateItemSkuExists(reqVO.getSkuId());
        // 3. 查询库存行（必须存在才能拣货）
        WmsInventoryDO inventory = inventoryMapper.selectBySkuIdAndWarehouseId(
                reqVO.getSkuId(), reqVO.getWarehouseId());
        if (inventory == null) {
            throw exception(INVENTORY_NOT_EXISTS);
        }
        // 4. P0-13：FEFO 批次分配——先到期先出
        applyFefoAndDeductBatches(inventory.getId(), reqVO.getQuantity());
        // 5. 扣减库存三维字段（PDA 拣货 = 出库，使用 OUT 类型同步校验 available）
        changeInventoryTyped(shipmentOrder.getId(), shipmentOrder.getNo(), WmsOrderTypeEnum.SHIPMENT.getType(),
                reqVO.getWarehouseId(), reqVO.getSkuId(), reqVO.getQuantity(),
                WmsInventoryChangeTypeEnum.OUT, reqVO.getRemark());
        log.info("[executePick][PDA 拣货成功，出库单={} 仓库={} SKU={} 数量={}]",
                shipmentOrder.getNo(), reqVO.getWarehouseId(), reqVO.getSkuId(), reqVO.getQuantity());
    }

    /**
     * P0-13：FEFO 批次分配并扣减批次数量
     *
     * <p>策略：
     * <ol>
     *   <li>调用 {@link WmsInventoryBatchService#applyFefoStrategy} 获取按过期日期升序的批次分配建议</li>
     *   <li>若分配不足 → 抛出 {@link ErrorCodeConstants#INVENTORY_BATCH_FEFO_INSUFFICIENT}</li>
     *   <li>按建议顺序逐个扣减批次 quantity（物理出库）</li>
     * </ol>
     *
     * <p><b>注意</b>：本方法只扣减批次表，库存表的三维字段由 {@link #changeInventoryTyped}(OUT) 统一处理。
     * 若库存行下无任何批次记录（兼容无批次管理场景），本方法跳过，不阻断拣货。
     *
     * @param inventoryId 库存编号
     * @param demandQuantity 需求数量（正数）
     */
    private void applyFefoAndDeductBatches(Long inventoryId, BigDecimal demandQuantity) {
        // 1. 检查是否有批次记录（无批次记录则跳过 FEFO，兼容无批次管理场景）
        List<WmsInventoryBatchDO> batches = inventoryBatchMapper.selectListByInventoryId(inventoryId);
        if (CollUtil.isEmpty(batches)) {
            return;
        }
        // 2. 应用 FEFO 策略获取分配建议
        WmsInventoryBatchStrategyRespVO strategy = inventoryBatchService.applyFefoStrategy(inventoryId, demandQuantity);
        if (strategy == null || !strategy.getSufficient()) {
            throw exception(INVENTORY_BATCH_FEFO_INSUFFICIENT,
                    inventoryId, inventoryId, demandQuantity, strategy == null ? BigDecimal.ZERO : strategy.getAllocatedQuantity());
        }
        // 3. 按分配建议逐个扣减批次 quantity
        for (WmsInventoryBatchStrategyRespVO.BatchAllocation alloc : strategy.getAllocations()) {
            WmsInventoryBatchDO batch = inventoryBatchMapper.selectById(alloc.getBatchId());
            if (batch == null) {
                continue;
            }
            BigDecimal newQty = (batch.getQuantity() == null ? BigDecimal.ZERO : batch.getQuantity())
                    .subtract(alloc.getAllocateQuantity());
            WmsInventoryBatchDO updateObj = new WmsInventoryBatchDO();
            updateObj.setId(batch.getId());
            updateObj.setQuantity(newQty);
            inventoryBatchMapper.updateById(updateObj);
            log.info("[applyFefoAndDeductBatches][FEFO 扣减批次 batchId={} batchNo={} deduct={} remaining={}]",
                    batch.getId(), batch.getBatchNo(), alloc.getAllocateQuantity(), newQty);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeCheck(WmsPdaCheckReqVO reqVO) {
        // 1. 校验盘库单存在
        WmsCheckOrderDO checkOrder = checkOrderMapper.selectById(reqVO.getCheckOrderId());
        if (checkOrder == null) {
            throw exception(cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.CHECK_ORDER_NOT_EXISTS);
        }
        // 2. 校验 SKU 存在
        itemSkuService.validateItemSkuExists(reqVO.getSkuId());
        // 3. 构建盘点请求
        WmsInventoryCheckReqDTO checkReqDTO = new WmsInventoryCheckReqDTO();
        checkReqDTO.setOrderId(checkOrder.getId());
        checkReqDTO.setOrderNo(checkOrder.getNo());
        checkReqDTO.setOrderType(WmsOrderTypeEnum.CHECK.getType());
        WmsInventoryCheckReqDTO.Item item = new WmsInventoryCheckReqDTO.Item();
        item.setInventoryId(reqVO.getInventoryId());
        item.setSkuId(reqVO.getSkuId());
        item.setWarehouseId(reqVO.getWarehouseId());
        item.setQuantity(reqVO.getBookQuantity());
        item.setCheckQuantity(reqVO.getCheckQuantity());
        item.setPrice(reqVO.getPrice());
        item.setRemark(reqVO.getRemark());
        checkReqDTO.setItems(Collections.singletonList(item));
        // 4. 执行盘点
        inventoryService.checkInventory(checkReqDTO);
        log.info("[executeCheck][PDA 盘点录入成功，盘库单={} SKU={} 实盘={}]",
                checkOrder.getNo(), reqVO.getSkuId(), reqVO.getCheckQuantity());
    }

    // ==================== 扫码 ====================

    private WmsPdaScanRespVO scanLocation(WmsPdaScanReqVO reqVO) {
        WmsPdaScanRespVO respVO = new WmsPdaScanRespVO();
        respVO.setScanType(WmsPdaScanReqVO.SCAN_TYPE_LOCATION);
        respVO.setScanCode(reqVO.getScanCode());
        // 1. 按仓库编码查询仓库（scanCode 作为仓库编码）
        WmsWarehouseDO warehouse = warehouseMapper.selectOne(WmsWarehouseDO::getCode, reqVO.getScanCode());
        if (warehouse == null && reqVO.getWarehouseId() != null) {
            warehouse = warehouseMapper.selectById(reqVO.getWarehouseId());
        }
        if (warehouse == null) {
            respVO.setSuccess(false);
            respVO.setInventories(Collections.emptyList());
            return respVO;
        }
        respVO.setSuccess(true);
        respVO.setWarehouseId(warehouse.getId());
        respVO.setWarehouseName(warehouse.getName());
        // 2. 查询仓库下的库存列表
        List<WmsInventoryDO> inventories = inventoryMapper.selectList(
                WmsInventoryDO::getWarehouseId, warehouse.getId());
        respVO.setInventories(buildInventoryItems(inventories));
        return respVO;
    }

    private WmsPdaScanRespVO scanItem(WmsPdaScanReqVO reqVO) {
        WmsPdaScanRespVO respVO = new WmsPdaScanRespVO();
        respVO.setScanType(WmsPdaScanReqVO.SCAN_TYPE_ITEM);
        respVO.setScanCode(reqVO.getScanCode());
        // 1. 按条码查询 SKU
        WmsItemSkuDO sku = itemSkuMapper.selectOne(WmsItemSkuDO::getBarCode, reqVO.getScanCode());
        if (sku == null) {
            // 尝试按 SKU 编码查询
            sku = itemSkuMapper.selectOne(WmsItemSkuDO::getCode, reqVO.getScanCode());
        }
        if (sku == null) {
            respVO.setSuccess(false);
            return respVO;
        }
        respVO.setSuccess(true);
        respVO.setSkuId(sku.getId());
        respVO.setSkuCode(sku.getCode());
        respVO.setSkuName(sku.getName());
        respVO.setItemId(sku.getItemId());
        respVO.setBarCode(sku.getBarCode());
        // 2. 查询商品信息
        WmsItemDO item = itemService.getItem(sku.getItemId());
        if (item != null) {
            respVO.setItemCode(item.getCode());
            respVO.setItemName(item.getName());
            respVO.setUnit(item.getUnit());
        }
        return respVO;
    }

    private List<WmsPdaScanRespVO.InventoryItem> buildInventoryItems(List<WmsInventoryDO> inventories) {
        if (CollUtil.isEmpty(inventories)) {
            return Collections.emptyList();
        }
        Set<Long> skuIds = inventories.stream()
                .map(WmsInventoryDO::getSkuId).collect(Collectors.toSet());
        Map<Long, WmsItemSkuDO> skuMap = itemSkuService.getItemSkuMap(skuIds);
        Set<Long> itemIds = skuMap.values().stream()
                .map(WmsItemSkuDO::getItemId).collect(Collectors.toSet());
        Map<Long, WmsItemDO> itemMap = itemService.getItemMap(itemIds);
        List<WmsPdaScanRespVO.InventoryItem> items = new ArrayList<>(inventories.size());
        for (WmsInventoryDO inventory : inventories) {
            WmsPdaScanRespVO.InventoryItem item = new WmsPdaScanRespVO.InventoryItem();
            item.setInventoryId(inventory.getId());
            item.setSkuId(inventory.getSkuId());
            item.setQuantity(inventory.getQuantity());
            item.setAvailableQuantity(inventory.getAvailableQuantity());
            MapUtils.findAndThen(skuMap, inventory.getSkuId(), sku -> {
                item.setSkuCode(sku.getCode());
                item.setSkuName(sku.getName());
                MapUtils.findAndThen(itemMap, sku.getItemId(), doItem -> {
                    item.setItemName(doItem.getName());
                    item.setUnit(doItem.getUnit());
                });
            });
            items.add(item);
        }
        return items;
    }

    // ==================== 任务列表构建 ====================

    private List<WmsPdaTaskRespVO> buildPickTasks() {
        // 查询草稿状态的出库单
        List<WmsShipmentOrderDO> orders = shipmentOrderMapper.selectList(
                WmsShipmentOrderDO::getStatus, WmsOrderStatusEnum.PREPARE.getStatus());
        if (CollUtil.isEmpty(orders)) {
            return Collections.emptyList();
        }
        List<Long> orderIds = orders.stream().map(WmsShipmentOrderDO::getId).collect(Collectors.toList());
        List<WmsShipmentOrderDetailDO> details = shipmentOrderDetailService.getShipmentOrderDetailList(orderIds);
        if (CollUtil.isEmpty(details)) {
            return Collections.emptyList();
        }
        Map<Long, WmsShipmentOrderDO> orderMap = orders.stream()
                .collect(Collectors.toMap(WmsShipmentOrderDO::getId, o -> o));
        // 填充名称
        Set<Long> skuIds = details.stream().map(WmsShipmentOrderDetailDO::getSkuId).collect(Collectors.toSet());
        Map<Long, WmsItemSkuDO> skuMap = itemSkuService.getItemSkuMap(skuIds);
        Set<Long> itemIds = skuMap.values().stream().map(WmsItemSkuDO::getItemId).collect(Collectors.toSet());
        Map<Long, WmsItemDO> itemMap = itemService.getItemMap(itemIds);
        Set<Long> warehouseIds = orders.stream().map(WmsShipmentOrderDO::getWarehouseId).collect(Collectors.toSet());
        Map<Long, WmsWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(warehouseIds);
        // 构建任务
        List<WmsPdaTaskRespVO> tasks = new ArrayList<>(details.size());
        for (WmsShipmentOrderDetailDO detail : details) {
            WmsShipmentOrderDO order = orderMap.get(detail.getOrderId());
            if (order == null) {
                continue;
            }
            WmsPdaTaskRespVO task = buildTaskBase(WmsPdaTaskRespVO.TYPE_PICK, order.getId(), order.getNo(),
                    order.getWarehouseId(), order.getOrderTime());
            task.setSkuId(detail.getSkuId());
            task.setDemandQuantity(detail.getQuantity());
            task.setExecutedQuantity(BigDecimal.ZERO);
            fillSkuAndItemInfo(task, detail.getSkuId(), skuMap, itemMap);
            MapUtils.findAndThen(warehouseMap, order.getWarehouseId(),
                    warehouse -> task.setWarehouseName(warehouse.getName()));
            tasks.add(task);
        }
        return tasks;
    }

    private List<WmsPdaTaskRespVO> buildPutawayTasks() {
        // 查询草稿状态的入库单
        List<WmsReceiptOrderDO> orders = receiptOrderMapper.selectList(
                WmsReceiptOrderDO::getStatus, WmsOrderStatusEnum.PREPARE.getStatus());
        if (CollUtil.isEmpty(orders)) {
            return Collections.emptyList();
        }
        List<Long> orderIds = orders.stream().map(WmsReceiptOrderDO::getId).collect(Collectors.toList());
        List<WmsReceiptOrderDetailDO> details = receiptOrderDetailService.getReceiptOrderDetailList(orderIds);
        if (CollUtil.isEmpty(details)) {
            return Collections.emptyList();
        }
        Map<Long, WmsReceiptOrderDO> orderMap = orders.stream()
                .collect(Collectors.toMap(WmsReceiptOrderDO::getId, o -> o));
        Set<Long> skuIds = details.stream().map(WmsReceiptOrderDetailDO::getSkuId).collect(Collectors.toSet());
        Map<Long, WmsItemSkuDO> skuMap = itemSkuService.getItemSkuMap(skuIds);
        Set<Long> itemIds = skuMap.values().stream().map(WmsItemSkuDO::getItemId).collect(Collectors.toSet());
        Map<Long, WmsItemDO> itemMap = itemService.getItemMap(itemIds);
        Set<Long> warehouseIds = orders.stream().map(WmsReceiptOrderDO::getWarehouseId).collect(Collectors.toSet());
        Map<Long, WmsWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(warehouseIds);
        List<WmsPdaTaskRespVO> tasks = new ArrayList<>(details.size());
        for (WmsReceiptOrderDetailDO detail : details) {
            WmsReceiptOrderDO order = orderMap.get(detail.getOrderId());
            if (order == null) {
                continue;
            }
            WmsPdaTaskRespVO task = buildTaskBase(WmsPdaTaskRespVO.TYPE_PUTAWAY, order.getId(), order.getNo(),
                    order.getWarehouseId(), order.getOrderTime());
            task.setSkuId(detail.getSkuId());
            task.setDemandQuantity(detail.getQuantity());
            task.setExecutedQuantity(BigDecimal.ZERO);
            fillSkuAndItemInfo(task, detail.getSkuId(), skuMap, itemMap);
            MapUtils.findAndThen(warehouseMap, order.getWarehouseId(),
                    warehouse -> task.setWarehouseName(warehouse.getName()));
            tasks.add(task);
        }
        return tasks;
    }

    private List<WmsPdaTaskRespVO> buildCheckTasks() {
        // 查询草稿状态的盘库单
        List<WmsCheckOrderDO> orders = checkOrderMapper.selectList(
                WmsCheckOrderDO::getStatus, WmsOrderStatusEnum.PREPARE.getStatus());
        if (CollUtil.isEmpty(orders)) {
            return Collections.emptyList();
        }
        List<Long> orderIds = orders.stream().map(WmsCheckOrderDO::getId).collect(Collectors.toList());
        List<WmsCheckOrderDetailDO> details = checkOrderDetailService.getCheckOrderDetailList(orderIds);
        if (CollUtil.isEmpty(details)) {
            return Collections.emptyList();
        }
        Map<Long, WmsCheckOrderDO> orderMap = orders.stream()
                .collect(Collectors.toMap(WmsCheckOrderDO::getId, o -> o));
        Set<Long> skuIds = details.stream().map(WmsCheckOrderDetailDO::getSkuId).collect(Collectors.toSet());
        Map<Long, WmsItemSkuDO> skuMap = itemSkuService.getItemSkuMap(skuIds);
        Set<Long> itemIds = skuMap.values().stream().map(WmsItemSkuDO::getItemId).collect(Collectors.toSet());
        Map<Long, WmsItemDO> itemMap = itemService.getItemMap(itemIds);
        Set<Long> warehouseIds = orders.stream().map(WmsCheckOrderDO::getWarehouseId).collect(Collectors.toSet());
        Map<Long, WmsWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(warehouseIds);
        List<WmsPdaTaskRespVO> tasks = new ArrayList<>(details.size());
        for (WmsCheckOrderDetailDO detail : details) {
            WmsCheckOrderDO order = orderMap.get(detail.getOrderId());
            if (order == null) {
                continue;
            }
            WmsPdaTaskRespVO task = buildTaskBase(WmsPdaTaskRespVO.TYPE_CHECK, order.getId(), order.getNo(),
                    order.getWarehouseId(), order.getOrderTime());
            task.setSkuId(detail.getSkuId());
            task.setDemandQuantity(detail.getQuantity());
            task.setExecutedQuantity(detail.getCheckQuantity());
            fillSkuAndItemInfo(task, detail.getSkuId(), skuMap, itemMap);
            MapUtils.findAndThen(warehouseMap, order.getWarehouseId(),
                    warehouse -> task.setWarehouseName(warehouse.getName()));
            tasks.add(task);
        }
        return tasks;
    }

    private WmsPdaTaskRespVO buildTaskBase(String taskType, Long orderId, String orderNo,
                                           Long warehouseId, java.time.LocalDateTime orderTime) {
        WmsPdaTaskRespVO task = new WmsPdaTaskRespVO();
        task.setTaskType(taskType);
        task.setOrderId(orderId);
        task.setOrderNo(orderNo);
        task.setWarehouseId(warehouseId);
        task.setOrderTime(orderTime);
        return task;
    }

    private void fillSkuAndItemInfo(WmsPdaTaskRespVO task, Long skuId,
                                    Map<Long, WmsItemSkuDO> skuMap, Map<Long, WmsItemDO> itemMap) {
        MapUtils.findAndThen(skuMap, skuId, sku -> {
            task.setSkuCode(sku.getCode());
            task.setSkuName(sku.getName());
            task.setItemId(sku.getItemId());
            MapUtils.findAndThen(itemMap, sku.getItemId(), item -> {
                task.setItemCode(item.getCode());
                task.setItemName(item.getName());
                task.setUnit(item.getUnit());
            });
        });
    }

    // ==================== 库存变更 ====================

    private void changeInventory(Long orderId, String orderNo, Integer orderType,
                                 Long warehouseId, Long skuId, BigDecimal quantity, String remark) {
        WmsInventoryChangeReqDTO reqDTO = new WmsInventoryChangeReqDTO();
        reqDTO.setOrderId(orderId);
        reqDTO.setOrderNo(orderNo);
        reqDTO.setOrderType(orderType);
        WmsInventoryChangeReqDTO.Item item = new WmsInventoryChangeReqDTO.Item();
        item.setSkuId(skuId);
        item.setWarehouseId(warehouseId);
        item.setQuantity(quantity);
        item.setRemark(remark);
        reqDTO.setItems(Collections.singletonList(item));
        inventoryService.changeInventory(reqDTO);
    }

    /**
     * P0-13：带变更类型的库存变更（IN/OUT/LOCK 等，同步三维字段）
     *
     * @param changeType 变更类型；IN/OUT 用正数 quantity，方向由类型决定
     */
    private void changeInventoryTyped(Long orderId, String orderNo, Integer orderType,
                                      Long warehouseId, Long skuId, BigDecimal quantity,
                                      WmsInventoryChangeTypeEnum changeType, String remark) {
        WmsInventoryChangeReqDTO reqDTO = new WmsInventoryChangeReqDTO();
        reqDTO.setOrderId(orderId);
        reqDTO.setOrderNo(orderNo);
        reqDTO.setOrderType(orderType);
        WmsInventoryChangeReqDTO.Item item = new WmsInventoryChangeReqDTO.Item();
        item.setSkuId(skuId);
        item.setWarehouseId(warehouseId);
        item.setQuantity(quantity);
        item.setChangeType(changeType);
        item.setRemark(remark);
        reqDTO.setItems(Collections.singletonList(item));
        inventoryService.changeInventory(reqDTO);
    }

}
