package cn.iocoder.yudao.module.oa.service.meeting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.controller.admin.meeting.vo.OaMeetingReservationPageReqVO;
import cn.iocoder.yudao.module.oa.controller.admin.meeting.vo.OaMeetingReservationSaveReqVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.meeting.OaMeetingReservationDO;
import jakarta.validation.Valid;

import java.time.LocalDateTime;

/**
 * OA 会议室预约 Service 接口
 *
 * @author yudao
 */
public interface OaMeetingReservationService {

    /**
     * 创建会议室预约
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createReservation(@Valid OaMeetingReservationSaveReqVO createReqVO);

    /**
     * 更新会议室预约
     *
     * @param updateReqVO 更新信息
     */
    void updateReservation(@Valid OaMeetingReservationSaveReqVO updateReqVO);

    /**
     * 删除会议室预约
     *
     * @param id 编号
     */
    void deleteReservation(Long id);

    /**
     * 获得会议室预约
     *
     * @param id 编号
     * @return 会议室预约
     */
    OaMeetingReservationDO getReservation(Long id);

    /**
     * 获得会议室预约分页
     *
     * @param pageReqVO 分页查询
     * @return 会议室预约分页
     */
    PageResult<OaMeetingReservationDO> getReservationPage(OaMeetingReservationPageReqVO pageReqVO);

    /**
     * 检查会议室时段冲突
     *
     * @param roomId               会议室 ID
     * @param startTime            开始时间
     * @param endTime              结束时间
     * @param excludeReservationId 排除的预约 ID（更新时传入自身，新增时传 null）
     * @return 是否存在冲突
     */
    boolean checkTimeConflict(Long roomId, LocalDateTime startTime, LocalDateTime endTime, Long excludeReservationId);

}
