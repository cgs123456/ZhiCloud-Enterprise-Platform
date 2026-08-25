package cn.zhicloud.module.oa.dal.mysql.meeting;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.oa.controller.admin.meeting.vo.OaMeetingRoomPageReqVO;
import cn.zhicloud.module.oa.dal.dataobject.meeting.OaMeetingRoomDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * OA 会议室 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface OaMeetingRoomMapper extends BaseMapperX<OaMeetingRoomDO> {

    default PageResult<OaMeetingRoomDO> selectPage(OaMeetingRoomPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OaMeetingRoomDO>()
                .likeIfPresent(OaMeetingRoomDO::getName, reqVO.getName())
                .eqIfPresent(OaMeetingRoomDO::getStatus, reqVO.getStatus())
                .orderByDesc(OaMeetingRoomDO::getId));
    }

    /**
     * 悲观锁查询会议室（用于会议室预订的并发控制，确保同一会议室的预订请求串行化）
     *
     * @param id 会议室编号
     * @return 会议室 DO
     */
    @Select("SELECT * FROM oa_meeting_room WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    OaMeetingRoomDO selectByIdForUpdate(@Param("id") Long id);

}
