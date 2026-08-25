package cn.zhicloud.module.tms.service.tracking;

import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.tms.controller.admin.tracking.vo.TmsTrackingEventSaveReqVO;
import cn.zhicloud.module.tms.dal.dataobject.tracking.TmsTrackingEventDO;
import cn.zhicloud.module.tms.dal.mysql.tracking.TmsTrackingEventMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.tms.enums.ErrorCodeConstants.TMS_TRACKING_EVENT_NOT_EXISTS;

/**
 * TMS 跟踪事件 Service 实现类
 *
 * @author zhicloud
 */
@Service
@Validated
public class TmsTrackingServiceImpl implements TmsTrackingService {

    @Resource
    private TmsTrackingEventMapper trackingEventMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long reportEvent(TmsTrackingEventSaveReqVO saveReqVO) {
        TmsTrackingEventDO event = BeanUtils.toBean(saveReqVO, TmsTrackingEventDO.class);
        // 事件时间为空时，默认取当前时间
        if (event.getEventTime() == null) {
            event.setEventTime(LocalDateTime.now());
        }
        trackingEventMapper.insert(event);
        return event.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTrackingEvent(Long id) {
        if (trackingEventMapper.selectById(id) == null) {
            throw exception(TMS_TRACKING_EVENT_NOT_EXISTS);
        }
        trackingEventMapper.deleteById(id);
    }

    @Override
    public TmsTrackingEventDO getTrackingEvent(Long id) {
        return trackingEventMapper.selectById(id);
    }

    @Override
    public List<TmsTrackingEventDO> getTrackingEventListByShipmentId(Long shipmentId) {
        return trackingEventMapper.selectListByShipmentId(shipmentId);
    }

}
