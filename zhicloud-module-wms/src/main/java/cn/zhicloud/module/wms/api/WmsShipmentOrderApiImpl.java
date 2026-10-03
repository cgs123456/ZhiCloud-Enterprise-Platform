package cn.zhicloud.module.wms.api;

import cn.zhicloud.module.wms.dal.dataobject.order.shipment.WmsShipmentOrderDO;
import cn.zhicloud.module.wms.dal.mysql.order.shipment.WmsShipmentOrderMapper;
import cn.zhicloud.module.wms.enums.order.WmsOrderStatusEnum;
import cn.zhicloud.module.wms.service.order.shipment.WmsShipmentOrderService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.SHIPMENT_ORDER_NOT_EXISTS;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.SHIPMENT_ORDER_NOT_SHIPPED_FOR_WAYBILL;

/**
 * {@link WmsShipmentOrderApi} 的实现类
 *
 * @author 智云
 */
@Service
public class WmsShipmentOrderApiImpl implements WmsShipmentOrderApi {

    @Resource
    private WmsShipmentOrderMapper shipmentOrderMapper;
    @Resource
    private WmsShipmentOrderService shipmentOrderService;

    @Override
    public void validateShippableForWaybill(Long shipmentOrderId) {
        WmsShipmentOrderDO order = shipmentOrderId == null ? null : shipmentOrderMapper.selectById(shipmentOrderId);
        if (order == null) {
            throw exception(SHIPMENT_ORDER_NOT_EXISTS);
        }
        // 只有「已发货」的出库单才在途，才谈得上被承运与签收；草稿/已拣货/已完成都不行
        if (!WmsOrderStatusEnum.SHIPPED.getStatus().equals(order.getStatus())) {
            throw exception(SHIPMENT_ORDER_NOT_SHIPPED_FOR_WAYBILL, shipmentOrderId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void signShipmentOrderByWaybill(Long shipmentOrderId, Long waybillId) {
        validateShippableForWaybill(shipmentOrderId);
        // 先登记运单号再推进状态：finish() 走 SHIPPED → FINISHED 并扣减库存，
        // 顺序反过来一旦 finish 成功而登记失败，出库单就完成了却查不到承运运单。
        WmsShipmentOrderDO updateObj = new WmsShipmentOrderDO();
        updateObj.setId(shipmentOrderId);
        updateObj.setWaybillId(waybillId);
        shipmentOrderMapper.updateById(updateObj);
        shipmentOrderService.finish(shipmentOrderId);
    }

}
