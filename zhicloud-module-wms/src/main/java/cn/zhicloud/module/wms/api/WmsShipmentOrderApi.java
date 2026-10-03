package cn.zhicloud.module.wms.api;

/**
 * WMS 出库单 API
 *
 * <p>供承运侧（TMS）在「出库单 → 运单 → 签收回传」链路上调用，
 * 调用方应只依赖本 api 包，不要 import WMS 的 dal/service（P1-3 模块边界治理）。
 *
 * @author 智云
 */
public interface WmsShipmentOrderApi {

    /**
     * 校验出库单当前可以建运单承运：必须存在，且状态为「已发货」（SHIPPED）
     *
     * @param shipmentOrderId 出库单编号
     */
    void validateShippableForWaybill(Long shipmentOrderId);

    /**
     * 运单签收后回传：给出库单登记运单号，并推进到「已完成」（FINISHED，含库存扣减）
     *
     * @param shipmentOrderId 出库单编号
     * @param waybillId       TMS 运单编号
     */
    void signShipmentOrderByWaybill(Long shipmentOrderId, Long waybillId);

}
