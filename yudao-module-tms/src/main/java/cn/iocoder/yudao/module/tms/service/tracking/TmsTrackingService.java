package cn.iocoder.yudao.module.tms.service.tracking;

import cn.iocoder.yudao.module.tms.controller.admin.tracking.vo.TmsTrackingEventSaveReqVO;
import cn.iocoder.yudao.module.tms.dal.dataobject.tracking.TmsTrackingEventDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * TMS 跟踪事件 Service 接口
 *
 * @author yudao
 */
public interface TmsTrackingService {

    /**
     * 上报跟踪事件
     *
     * @param saveReqVO 跟踪事件信息
     * @return 编号
     */
    Long reportEvent(@Valid TmsTrackingEventSaveReqVO saveReqVO);

    /**
     * 删除跟踪事件
     *
     * @param id 编号
     */
    void deleteTrackingEvent(Long id);

    /**
     * 获得跟踪事件
     *
     * @param id 编号
     * @return 跟踪事件
     */
    TmsTrackingEventDO getTrackingEvent(Long id);

    /**
     * 获得运单的跟踪事件列表
     *
     * @param shipmentId 运单编号
     * @return 跟踪事件列表
     */
    List<TmsTrackingEventDO> getTrackingEventListByShipmentId(Long shipmentId);

}
