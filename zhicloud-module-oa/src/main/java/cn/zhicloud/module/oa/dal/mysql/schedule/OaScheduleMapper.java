package cn.zhicloud.module.oa.dal.mysql.schedule;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.oa.controller.admin.schedule.vo.OaSchedulePageReqVO;
import cn.zhicloud.module.oa.dal.dataobject.schedule.OaScheduleDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OA 日程 Mapper
 *
 * @author 智云
 */
@Mapper
public interface OaScheduleMapper extends BaseMapperX<OaScheduleDO> {

    default PageResult<OaScheduleDO> selectPage(OaSchedulePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OaScheduleDO>()
                .eqIfPresent(OaScheduleDO::getUserId, reqVO.getUserId())
                .eqIfPresent(OaScheduleDO::getType, reqVO.getType())
                .eqIfPresent(OaScheduleDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(OaScheduleDO::getStartTime, reqVO.getStartTime())
                .orderByAsc(OaScheduleDO::getStartTime));
    }

    /**
     * 查询指定用户在时间范围内的日程
     */
    default List<OaScheduleDO> selectListByUserIdAndTimeRange(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return selectList(new LambdaQueryWrapperX<OaScheduleDO>()
                .eq(OaScheduleDO::getUserId, userId)
                .ge(OaScheduleDO::getStartTime, startTime)
                .le(OaScheduleDO::getStartTime, endTime)
                .orderByAsc(OaScheduleDO::getStartTime));
    }

    /**
     * 查询需要提醒的日程（未提醒 + 提醒时间已到）
     */
    default List<OaScheduleDO> selectListNeedRemind(LocalDateTime now) {
        return selectList(new LambdaQueryWrapperX<OaScheduleDO>()
                .eq(OaScheduleDO::getReminded, false)
                .isNotNull(OaScheduleDO::getRemindMinutes)
                .le(OaScheduleDO::getStartTime, now.plusMinutes(60))
                .ge(OaScheduleDO::getStartTime, now)
                .eq(OaScheduleDO::getStatus, 0));
    }

}
