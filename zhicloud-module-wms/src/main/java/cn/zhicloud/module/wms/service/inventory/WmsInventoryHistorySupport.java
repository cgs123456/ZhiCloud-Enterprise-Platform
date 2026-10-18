package cn.zhicloud.module.wms.service.inventory;

import cn.zhicloud.framework.common.util.number.MoneyUtils;
import cn.zhicloud.module.wms.dal.dataobject.inventory.WmsInventoryHistoryDO;
import cn.zhicloud.module.wms.service.inventory.dto.WmsInventoryChangeReqDTO;
import cn.zhicloud.module.wms.service.inventory.dto.WmsInventoryCheckReqDTO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * WMS 库存流水构建静态协作者（包内）
 *
 * <p>V211（ADR 269 D7/D8）：自 {@link WmsInventoryServiceImpl} 抽出的流水构建——行级口径：
 * P-deduct 参与行逐行一条（各带行库位与行级前后量），非分配明细单条（明细行库位），
 * 盘点单条（被盘行库位）。
 *
 * @author 智云
 */
final class WmsInventoryHistorySupport {

    private WmsInventoryHistorySupport() {
    }

    /**
     * 变更流水（V211 行级）：按明细顺序展开「行级记录」逐行构建；
     * 单据字段（orderId/orderNo/orderType）随明细头；价格/备注沿用明细原值。
     */
    static List<WmsInventoryHistoryDO> buildChangeHistories(WmsInventoryChangeReqDTO reqDTO,
                                                           List<WmsInventoryChangeReqDTO.Item> items,
                                                           Map<WmsInventoryChangeReqDTO.Item, List<WmsInventoryRowChange>> rowChangesMap) {
        List<WmsInventoryHistoryDO> histories = new ArrayList<>(items.size());
        for (WmsInventoryChangeReqDTO.Item item : items) {
            for (WmsInventoryRowChange rowChange : rowChangesMap.get(item)) {
                histories.add(new WmsInventoryHistoryDO()
                        .setWarehouseId(item.getWarehouseId()).setSkuId(item.getSkuId())
                        .setQuantity(rowChange.getQuantity()).setBeforeQuantity(rowChange.getBeforeQuantity())
                        .setAfterQuantity(rowChange.getAfterQuantity())
                        .setPrice(item.getPrice()).setTotalPrice(item.getTotalPrice()).setRemark(item.getRemark())
                        .setLocationId(rowChange.getLocationId())
                        .setOrderId(reqDTO.getOrderId()).setOrderNo(reqDTO.getOrderNo()).setOrderType(reqDTO.getOrderType()));
            }
        }
        return histories;
    }

    /**
     * 盘点流水（单条）：数量 = 盘点后 - 盘点前，金额按单价折算；库位取自被盘行（V211）。
     */
    static WmsInventoryHistoryDO buildCheckHistory(WmsInventoryCheckReqDTO reqDTO,
                                                   WmsInventoryCheckReqDTO.Item item,
                                                   BigDecimal beforeQuantity,
                                                   BigDecimal afterQuantity,
                                                   Long locationId) {
        BigDecimal quantity = afterQuantity.subtract(beforeQuantity);
        return new WmsInventoryHistoryDO().setWarehouseId(item.getWarehouseId()).setSkuId(item.getSkuId())
                .setQuantity(quantity).setBeforeQuantity(beforeQuantity).setAfterQuantity(afterQuantity)
                .setPrice(item.getPrice()).setTotalPrice(MoneyUtils.priceMultiply(item.getPrice(), quantity))
                .setLocationId(locationId == null ? 0L : locationId)
                .setOrderId(reqDTO.getOrderId()).setOrderNo(reqDTO.getOrderNo()).setOrderType(reqDTO.getOrderType())
                .setRemark(item.getRemark());
    }

}
