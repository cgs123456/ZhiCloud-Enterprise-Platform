package cn.iocoder.yudao.module.wms.service.order.shipment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.wms.controller.admin.order.shipment.vo.order.WmsShipmentOrderPageReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.order.shipment.vo.order.WmsShipmentOrderSaveReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.shipment.WmsShipmentOrderDO;
import jakarta.validation.Valid;

/**
 * WMS 出库单 Service 接口
 *
 * @author 芋道源码
 */
public interface WmsShipmentOrderService {

    /**
     * 创建出库单
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createShipmentOrder(@Valid WmsShipmentOrderSaveReqVO createReqVO);

    /**
     * 更新出库单
     *
     * @param updateReqVO 更新信息
     */
    void updateShipmentOrder(@Valid WmsShipmentOrderSaveReqVO updateReqVO);

    /**
     * 删除出库单
     *
     * @param id 编号
     */
    void deleteShipmentOrder(Long id);

    /**
     * 完成出库（兼容接口：草稿 -> 已完成，跳过中间状态）
     *
     * @param id 编号
     */
    void completeShipmentOrder(Long id);

    /**
     * 作废出库单（任意非 FINISHED 状态 -> 已作废）
     *
     * @param id 编号
     */
    void cancelShipmentOrder(Long id);

    /**
     * 开始拣货（草稿 -> 拣货中）
     *
     * @param id 编号
     */
    void startPick(Long id);

    /**
     * 完成拣货（拣货中 -> 已拣货）
     *
     * @param id 编号
     */
    void completePick(Long id);

    /**
     * 复核（已拣货 -> 已复核）
     *
     * @param id 编号
     */
    void review(Long id);

    /**
     * 打包（已复核 -> 已打包）
     *
     * @param id 编号
     */
    void pack(Long id);

    /**
     * 发货（已打包 -> 已发货）
     *
     * @param id 编号
     */
    void ship(Long id);

    /**
     * 完成出库（已发货 -> 已完成，扣减库存）
     *
     * @param id 编号
     */
    void finish(Long id);

    /**
     * 获得出库单
     *
     * @param id 编号
     * @return 出库单
     */
    WmsShipmentOrderDO getShipmentOrder(Long id);

    /**
     * 获得出库单分页
     *
     * @param pageReqVO 分页查询
     * @return 出库单分页
     */
    PageResult<WmsShipmentOrderDO> getShipmentOrderPage(WmsShipmentOrderPageReqVO pageReqVO);

    /**
     * 获得指定往来企业的出库单数量
     *
     * @param merchantId 往来企业编号
     * @return 出库单数量
     */
    long getShipmentOrderCountByMerchantId(Long merchantId);

    /**
     * 获得指定仓库的出库单数量
     *
     * @param warehouseId 仓库编号
     * @return 出库单数量
     */
    long getShipmentOrderCountByWarehouseId(Long warehouseId);

}