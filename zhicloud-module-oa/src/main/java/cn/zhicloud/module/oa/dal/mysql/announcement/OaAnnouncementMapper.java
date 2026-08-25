package cn.zhicloud.module.oa.dal.mysql.announcement;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.oa.controller.admin.announcement.vo.OaAnnouncementPageReqVO;
import cn.zhicloud.module.oa.dal.dataobject.announcement.OaAnnouncementDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OA 公告 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface OaAnnouncementMapper extends BaseMapperX<OaAnnouncementDO> {

    /**
     * 已发布状态
     */
    int STATUS_PUBLISHED = 20;

    default PageResult<OaAnnouncementDO> selectPage(OaAnnouncementPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OaAnnouncementDO>()
                .likeIfPresent(OaAnnouncementDO::getTitle, reqVO.getTitle())
                .eqIfPresent(OaAnnouncementDO::getCategory, reqVO.getCategory())
                .eqIfPresent(OaAnnouncementDO::getPriority, reqVO.getPriority())
                .eqIfPresent(OaAnnouncementDO::getStatus, reqVO.getStatus())
                .eqIfPresent(OaAnnouncementDO::getPublisherUserId, reqVO.getPublisherUserId())
                .betweenIfPresent(OaAnnouncementDO::getPublishTime, reqVO.getPublishTime())
                .orderByDesc(OaAnnouncementDO::getId));
    }

    /**
     * 查询已发布且未过期的公告，按置顶、发布时间倒序
     *
     * @return 已发布公告列表
     */
    default List<OaAnnouncementDO> selectListPublished() {
        return selectList(new LambdaQueryWrapperX<OaAnnouncementDO>()
                .eq(OaAnnouncementDO::getStatus, STATUS_PUBLISHED)
                .and(w -> w.isNull(OaAnnouncementDO::getExpireTime)
                        .or().gt(OaAnnouncementDO::getExpireTime, LocalDateTime.now()))
                .orderByDesc(OaAnnouncementDO::getTopFlag)
                .orderByDesc(OaAnnouncementDO::getPublishTime));
    }

}
