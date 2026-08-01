package cn.iocoder.yudao.module.oa.service.announcement;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.controller.admin.announcement.vo.OaAnnouncementPageReqVO;
import cn.iocoder.yudao.module.oa.controller.admin.announcement.vo.OaAnnouncementSaveReqVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.announcement.OaAnnouncementDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * OA 公告 Service 接口
 *
 * @author yudao
 */
public interface OaAnnouncementService {

    /**
     * 创建公告
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createAnnouncement(@Valid OaAnnouncementSaveReqVO createReqVO);

    /**
     * 更新公告
     *
     * @param updateReqVO 更新信息
     */
    void updateAnnouncement(@Valid OaAnnouncementSaveReqVO updateReqVO);

    /**
     * 删除公告
     *
     * @param id 编号
     */
    void deleteAnnouncement(Long id);

    /**
     * 获得公告
     *
     * @param id 编号
     * @return 公告
     */
    OaAnnouncementDO getAnnouncement(Long id);

    /**
     * 获得公告分页
     *
     * @param pageReqVO 分页查询
     * @return 公告分页
     */
    PageResult<OaAnnouncementDO> getAnnouncementPage(OaAnnouncementPageReqVO pageReqVO);

    /**
     * 发布公告：草稿 -> 已发布
     *
     * @param id 编号
     */
    void publishAnnouncement(Long id);

    /**
     * 下架公告：已发布 -> 已下架
     *
     * @param id 编号
     */
    void takeDownAnnouncement(Long id);

    /**
     * 获取已发布且未过期的公告列表
     *
     * @return 已发布公告列表
     */
    List<OaAnnouncementDO> getPublishedList();

}
