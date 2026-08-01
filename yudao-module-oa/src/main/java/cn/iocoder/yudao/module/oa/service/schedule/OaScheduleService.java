package cn.iocoder.yudao.module.oa.service.schedule;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.controller.admin.schedule.vo.OaSchedulePageReqVO;
import cn.iocoder.yudao.module.oa.controller.admin.schedule.vo.OaScheduleSaveReqVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.schedule.OaScheduleDO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OA 日程 Service 接口
 *
 * @author 芋道源码
 */
public interface OaScheduleService {

    /**
     * 创建日程
     */
    Long createSchedule(OaScheduleSaveReqVO createReqVO);

    /**
     * 更新日程
     */
    void updateSchedule(OaScheduleSaveReqVO updateReqVO);

    /**
     * 删除日程
     */
    void deleteSchedule(Long id);

    /**
     * 获取日程
     */
    OaScheduleDO getSchedule(Long id);

    /**
     * 获取日程分页
     */
    PageResult<OaScheduleDO> getSchedulePage(OaSchedulePageReqVO pageReqVO);

    /**
     * 获取用户在指定时间范围内的日程
     */
    List<OaScheduleDO> getScheduleListByUser(Long userId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 完成日程
     */
    void completeSchedule(Long id);

    /**
     * 取消日程
     */
    void cancelSchedule(Long id);

    /**
     * 获取需要提醒的日程列表
     */
    List<OaScheduleDO> getScheduleListNeedRemind();

    /**
     * 标记日程已提醒
     */
    void markReminded(Long id);

}
