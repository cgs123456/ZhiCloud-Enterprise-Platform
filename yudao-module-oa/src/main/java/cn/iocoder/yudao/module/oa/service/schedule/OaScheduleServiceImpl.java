package cn.iocoder.yudao.module.oa.service.schedule;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.oa.controller.admin.schedule.vo.OaSchedulePageReqVO;
import cn.iocoder.yudao.module.oa.controller.admin.schedule.vo.OaScheduleSaveReqVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.schedule.OaScheduleDO;
import cn.iocoder.yudao.module.oa.dal.mysql.schedule.OaScheduleMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.oa.enums.ErrorCodeConstants.*;

/**
 * OA 日程 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class OaScheduleServiceImpl implements OaScheduleService {

    @Resource
    private OaScheduleMapper scheduleMapper;

    @Override
    public Long createSchedule(OaScheduleSaveReqVO createReqVO) {
        OaScheduleDO schedule = BeanUtils.toBean(createReqVO, OaScheduleDO.class);
        // 默认值
        if (schedule.getStatus() == null) {
            schedule.setStatus(0); // 未完成
        }
        if (schedule.getAllDay() == null) {
            schedule.setAllDay(false);
        }
        if (schedule.getRepeatType() == null) {
            schedule.setRepeatType(0); // 不重复
        }
        schedule.setReminded(false);
        scheduleMapper.insert(schedule);
        return schedule.getId();
    }

    @Override
    public void updateSchedule(OaScheduleSaveReqVO updateReqVO) {
        validateScheduleExists(updateReqVO.getId());
        OaScheduleDO updateObj = BeanUtils.toBean(updateReqVO, OaScheduleDO.class);
        // 禁止通过通用更新修改状态
        updateObj.setStatus(null);
        scheduleMapper.updateById(updateObj);
    }

    @Override
    public void deleteSchedule(Long id) {
        validateScheduleExists(id);
        scheduleMapper.deleteById(id);
    }

    @Override
    public OaScheduleDO getSchedule(Long id) {
        return scheduleMapper.selectById(id);
    }

    @Override
    public PageResult<OaScheduleDO> getSchedulePage(OaSchedulePageReqVO pageReqVO) {
        return scheduleMapper.selectPage(pageReqVO);
    }

    @Override
    public List<OaScheduleDO> getScheduleListByUser(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return scheduleMapper.selectListByUserIdAndTimeRange(userId, startTime, endTime);
    }

    @Override
    public void completeSchedule(Long id) {
        validateScheduleExists(id);
        OaScheduleDO updateObj = new OaScheduleDO();
        updateObj.setId(id);
        updateObj.setStatus(1); // 已完成
        scheduleMapper.updateById(updateObj);
    }

    @Override
    public void cancelSchedule(Long id) {
        validateScheduleExists(id);
        OaScheduleDO updateObj = new OaScheduleDO();
        updateObj.setId(id);
        updateObj.setStatus(2); // 已取消
        scheduleMapper.updateById(updateObj);
    }

    @Override
    public List<OaScheduleDO> getScheduleListNeedRemind() {
        return scheduleMapper.selectListNeedRemind(LocalDateTime.now());
    }

    @Override
    public void markReminded(Long id) {
        OaScheduleDO updateObj = new OaScheduleDO();
        updateObj.setId(id);
        updateObj.setReminded(true);
        scheduleMapper.updateById(updateObj);
    }

    private void validateScheduleExists(Long id) {
        if (scheduleMapper.selectById(id) == null) {
            throw exception(OA_SCHEDULE_NOT_EXISTS);
        }
    }

}
