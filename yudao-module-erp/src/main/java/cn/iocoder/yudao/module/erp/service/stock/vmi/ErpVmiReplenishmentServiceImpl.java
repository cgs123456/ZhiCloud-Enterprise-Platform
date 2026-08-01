package cn.iocoder.yudao.module.erp.service.stock.vmi;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vmi.vo.ErpVmiReplenishmentPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.vmi.ErpVmiInventoryDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.vmi.ErpVmiReplenishmentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.vmi.ErpVmiReplenishmentItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.vmi.ErpVmiReplenishmentItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.vmi.ErpVmiReplenishmentMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.VMI_REPLENISHMENT_NOT_CONVERTIBLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.VMI_REPLENISHMENT_NOT_EXISTS;

/**
 * ERP VMI 补货建议 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpVmiReplenishmentServiceImpl implements ErpVmiReplenishmentService {

    /**
     * 补货建议状态：待处理
     */
    private static final int STATUS_PENDING = 10;
    /**
     * 补货建议状态：已生成采购订单
     */
    private static final int STATUS_PURCHASE_ORDER = 20;

    @Resource
    private ErpVmiReplenishmentMapper vmiReplenishmentMapper;
    @Resource
    private ErpVmiReplenishmentItemMapper vmiReplenishmentItemMapper;
    @Resource
    private ErpVmiInventoryService vmiInventoryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteVmiReplenishment(Long id) {
        validateVmiReplenishmentExists(id);
        vmiReplenishmentItemMapper.deleteByReplenishmentId(id);
        vmiReplenishmentMapper.deleteById(id);
    }

    private void validateVmiReplenishmentExists(Long id) {
        if (vmiReplenishmentMapper.selectById(id) == null) {
            throw exception(VMI_REPLENISHMENT_NOT_EXISTS);
        }
    }

    @Override
    public ErpVmiReplenishmentDO getVmiReplenishment(Long id) {
        return vmiReplenishmentMapper.selectById(id);
    }

    @Override
    public List<ErpVmiReplenishmentItemDO> getVmiReplenishmentItemList(Long replenishmentId) {
        return vmiReplenishmentItemMapper.selectListByReplenishmentId(replenishmentId);
    }

    @Override
    public PageResult<ErpVmiReplenishmentDO> getVmiReplenishmentPage(ErpVmiReplenishmentPageReqVO pageReqVO) {
        return vmiReplenishmentMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> generateReplenishment() {
        // 1. 扫描需要补货的 VMI 库存
        List<ErpVmiInventoryDO> inventoryList = vmiInventoryService.checkReplenishment();
        if (inventoryList.isEmpty()) {
            return new ArrayList<>();
        }
        // 2. 按供应商 + 仓库分组
        Map<String, List<ErpVmiInventoryDO>> grouped = inventoryList.stream()
                .collect(Collectors.groupingBy(i -> i.getSupplierId() + "_" + i.getWarehouseId()));
        // 3. 每组生成一条补货建议
        List<Long> result = new ArrayList<>();
        for (List<ErpVmiInventoryDO> group : grouped.values()) {
            ErpVmiInventoryDO first = group.get(0);
            ErpVmiReplenishmentDO replenishment = ErpVmiReplenishmentDO.builder()
                    .no(generateReplenishmentNo())
                    .supplierId(first.getSupplierId())
                    .warehouseId(first.getWarehouseId())
                    .status(STATUS_PENDING)
                    .totalQuantity(BigDecimal.ZERO)
                    .build();
            vmiReplenishmentMapper.insert(replenishment);
            // 生成明细
            BigDecimal totalQuantity = BigDecimal.ZERO;
            List<ErpVmiReplenishmentItemDO> items = new ArrayList<>();
            for (ErpVmiInventoryDO inventory : group) {
                // 建议补货数量 = 最高库存 - 当前库存
                BigDecimal suggested = inventory.getMaxQuantity().subtract(inventory.getQuantity());
                if (suggested.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                ErpVmiReplenishmentItemDO item = ErpVmiReplenishmentItemDO.builder()
                        .replenishmentId(replenishment.getId())
                        .productId(inventory.getProductId())
                        .productName(inventory.getProductName())
                        .quantity(suggested)
                        .currentQuantity(inventory.getQuantity())
                        .suggestedQuantity(suggested)
                        .build();
                items.add(item);
                totalQuantity = totalQuantity.add(suggested);
            }
            for (ErpVmiReplenishmentItemDO item : items) {
                vmiReplenishmentItemMapper.insert(item);
            }
            // 更新合计数量
            replenishment.setTotalQuantity(totalQuantity);
            vmiReplenishmentMapper.updateById(replenishment);
            result.add(replenishment.getId());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String convertToPurchaseOrder(Long id) {
        // 1. 校验存在
        ErpVmiReplenishmentDO replenishment = vmiReplenishmentMapper.selectById(id);
        if (replenishment == null) {
            throw exception(VMI_REPLENISHMENT_NOT_EXISTS);
        }
        // 2. 校验状态：只有待处理状态可转采购订单
        if (!Integer.valueOf(STATUS_PENDING).equals(replenishment.getStatus())) {
            throw exception(VMI_REPLENISHMENT_NOT_CONVERTIBLE);
        }
        // 3. 生成采购订单号（这里仅更新补货建议状态，实际采购订单由采购模块创建）
        String purchaseOrderNo = generatePurchaseOrderNo();
        // 4. 更新补货建议状态为已生成采购订单
        ErpVmiReplenishmentDO updateObj = new ErpVmiReplenishmentDO();
        updateObj.setId(id);
        updateObj.setStatus(STATUS_PURCHASE_ORDER);
        vmiReplenishmentMapper.updateById(updateObj);
        return purchaseOrderNo;
    }

    private String generateReplenishmentNo() {
        return "VMI" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    private String generatePurchaseOrderNo() {
        return "PO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(1000, 9999);
    }

}
