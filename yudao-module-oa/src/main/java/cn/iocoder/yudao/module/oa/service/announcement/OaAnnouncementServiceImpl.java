package cn.iocoder.yudao.module.oa.service.announcement;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.oa.controller.admin.announcement.vo.OaAnnouncementPageReqVO;
import cn.iocoder.yudao.module.oa.controller.admin.announcement.vo.OaAnnouncementSaveReqVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.announcement.OaAnnouncementDO;
import cn.iocoder.yudao.module.oa.dal.mysql.announcement.OaAnnouncementMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.oa.enums.ErrorCodeConstants.OA_ANNOUNCEMENT_NOT_EXISTS;
import static cn.iocoder.yudao.module.oa.enums.ErrorCodeConstants.OA_ANNOUNCEMENT_STATUS_INVALID;

/**
 * OA 公告 Service 实现类
 *
 * @author yudao
 */
@Service
@Validated
public class OaAnnouncementServiceImpl implements OaAnnouncementService {

    /**
     * 草稿状态
     */
    private static final int STATUS_DRAFT = 10;
    /**
     * 已发布状态
     */
    private static final int STATUS_PUBLISHED = 20;
    /**
     * 已下架状态
     */
    private static final int STATUS_TAKEN_DOWN = 30;

    @Resource
    private OaAnnouncementMapper announcementMapper;

    @Override
    public Long createAnnouncement(OaAnnouncementSaveReqVO createReqVO) {
        // 插入公告（默认草稿状态）
        OaAnnouncementDO announcement = BeanUtils.toBean(createReqVO, OaAnnouncementDO.class);
        if (announcement.getStatus() == null) {
            announcement.setStatus(STATUS_DRAFT);
        }
        if (announcement.getTopFlag() == null) {
            announcement.setTopFlag(false);
        }
        if (announcement.getViewCount() == null) {
            announcement.setViewCount(0);
        }
        announcementMapper.insert(announcement);
        return announcement.getId();
    }

    @Override
    public void updateAnnouncement(OaAnnouncementSaveReqVO updateReqVO) {
        // 校验存在 & 状态（仅草稿可修改）
        OaAnnouncementDO announcement = validateAnnouncementExists(updateReqVO.getId());
        if (!Integer.valueOf(STATUS_DRAFT).equals(announcement.getStatus())) {
            throw exception(OA_ANNOUNCEMENT_STATUS_INVALID);
        }
        // 更新公告
        OaAnnouncementDO updateObj = BeanUtils.toBean(updateReqVO, OaAnnouncementDO.class);
        announcementMapper.updateById(updateObj);
    }

    @Override
    public void deleteAnnouncement(Long id) {
        validateAnnouncementExists(id);
        announcementMapper.deleteById(id);
    }

    @Override
    public OaAnnouncementDO getAnnouncement(Long id) {
        return announcementMapper.selectById(id);
    }

    @Override
    public PageResult<OaAnnouncementDO> getAnnouncementPage(OaAnnouncementPageReqVO pageReqVO) {
        return announcementMapper.selectPage(pageReqVO);
    }

    @Override
    public void publishAnnouncement(Long id) {
        // 校验存在 & 状态（仅草稿可发布）
        OaAnnouncementDO announcement = validateAnnouncementExists(id);
        if (!Integer.valueOf(STATUS_DRAFT).equals(announcement.getStatus())) {
            throw exception(OA_ANNOUNCEMENT_STATUS_INVALID);
        }
        // 更新为已发布，并设置发布时间
        OaAnnouncementDO updateObj = new OaAnnouncementDO();
        updateObj.setId(id);
        updateObj.setStatus(STATUS_PUBLISHED);
        updateObj.setPublishTime(LocalDateTime.now());
        announcementMapper.updateById(updateObj);
    }

    @Override
    public void takeDownAnnouncement(Long id) {
        // 校验存在 & 状态（仅已发布可下架）
        OaAnnouncementDO announcement = validateAnnouncementExists(id);
        if (!Integer.valueOf(STATUS_PUBLISHED).equals(announcement.getStatus())) {
            throw exception(OA_ANNOUNCEMENT_STATUS_INVALID);
        }
        // 更新为已下架
        OaAnnouncementDO updateObj = new OaAnnouncementDO();
        updateObj.setId(id);
        updateObj.setStatus(STATUS_TAKEN_DOWN);
        announcementMapper.updateById(updateObj);
    }

    @Override
    public List<OaAnnouncementDO> getPublishedList() {
        return announcementMapper.selectListPublished();
    }

    private OaAnnouncementDO validateAnnouncementExists(Long id) {
        OaAnnouncementDO announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw exception(OA_ANNOUNCEMENT_NOT_EXISTS);
        }
        return announcement;
    }

}
