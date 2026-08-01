package cn.iocoder.yudao.module.tms.service.shipment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.tms.controller.admin.shipment.vo.TmsShipmentDispatchReqVO;
import cn.iocoder.yudao.module.tms.controller.admin.shipment.vo.TmsShipmentPageReqVO;
import cn.iocoder.yudao.module.tms.controller.admin.shipment.vo.TmsShipmentSaveReqVO;
import cn.iocoder.yudao.module.tms.dal.dataobject.shipment.TmsShipmentDO;
import cn.iocoder.yudao.module.tms.dal.dataobject.shipment.TmsShipmentStopDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * TMS 运单 Service 接口
 *
 * @author yudao
 */
public interface TmsShipmentService {

    Long createShipment(@Valid TmsShipmentSaveReqVO createReqVO);

    void updateShipment(@Valid TmsShipmentSaveReqVO updateReqVO);

    void deleteShipment(Long id);

    TmsShipmentDO getShipment(Long id);

    PageResult<TmsShipmentDO> getShipmentPage(TmsShipmentPageReqVO pageReqVO);

    /**
     * 获得运单站点列表
     *
     * @param shipmentId 运单编号
     * @return 站点列表
     */
    List<TmsShipmentStopDO> getShipmentStopList(Long shipmentId);

    /**
     * 调度：匹配车辆/司机
     *
     * @param dispatchReqVO 调度信息
     */
    void dispatch(@Valid TmsShipmentDispatchReqVO dispatchReqVO);

    /**
     * 确认到达
     *
     * @param id 运单编号
     */
    void confirmArrival(Long id);

    /**
     * 确认签收
     *
     * @param id 运单编号
     */
    void confirmSign(Long id);

}
