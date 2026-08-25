package cn.zhicloud.module.oa.service.portal;

import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.framework.security.core.util.SecurityFrameworkUtils;
import cn.zhicloud.module.oa.controller.admin.announcement.vo.OaAnnouncementRespVO;
import cn.zhicloud.module.oa.controller.admin.portal.vo.OaPortalDashboardVO;
import cn.zhicloud.module.oa.dal.dataobject.announcement.OaAnnouncementDO;
import cn.zhicloud.module.oa.dal.dataobject.document.OaDocumentDO;
import cn.zhicloud.module.oa.dal.dataobject.reimburse.OaReimburseDO;
import cn.zhicloud.module.oa.dal.mysql.document.OaDocumentMapper;
import cn.zhicloud.module.oa.dal.mysql.reimburse.OaReimburseMapper;
import cn.zhicloud.module.oa.service.announcement.OaAnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OA 工作台门户 Service 实现类
 *
 * @author zhicloud
 */
@Service
@Validated
public class OaPortalServiceImpl implements OaPortalService {

    /**
     * 工作台展示的最新公告数量
     */
    private static final int ANNOUNCEMENT_LIMIT = 5;

    @Autowired(required = false)
    private OaAnnouncementService announcementService;
    @Autowired(required = false)
    private OaReimburseMapper reimburseMapper;
    @Autowired(required = false)
    private OaDocumentMapper documentMapper;

    @Override
    public OaPortalDashboardVO getDashboard() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        OaPortalDashboardVO dashboard = new OaPortalDashboardVO();
        // 最新公告（前 5 条）
        dashboard.setAnnouncements(getLatestAnnouncements());
        // 待办数：依赖 BPM task API，暂占位
        dashboard.setTodoCount(0);
        // 今日会议数：依赖会议 service，暂占位
        dashboard.setTodayMeetingCount(0);
        // 待审批报销数：依赖 BPM task API 获取当前用户待审批任务，暂占位
        dashboard.setPendingReimburseCount(0);
        // 我的报销数
        dashboard.setMyReimburseCount(countMyReimburses(userId));
        // 我的公文数
        dashboard.setMyDocumentsCount(countMyDocuments(userId));
        // 已发布知识文章数：知识库 service 未实现，暂占位
        dashboard.setPublishedKnowledgeCount(0);
        return dashboard;
    }

    private List<OaAnnouncementRespVO> getLatestAnnouncements() {
        if (announcementService == null) {
            return Collections.emptyList();
        }
        List<OaAnnouncementDO> list = announcementService.getPublishedList();
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        List<OaAnnouncementDO> top = list.stream().limit(ANNOUNCEMENT_LIMIT).collect(Collectors.toList());
        return BeanUtils.toBean(top, OaAnnouncementRespVO.class);
    }

    private Integer countMyReimburses(Long userId) {
        if (reimburseMapper == null || userId == null) {
            return 0;
        }
        Long count = reimburseMapper.selectCount(new LambdaQueryWrapperX<OaReimburseDO>()
                .eq(OaReimburseDO::getApplicantUserId, userId));
        return count == null ? 0 : count.intValue();
    }

    private Integer countMyDocuments(Long userId) {
        if (documentMapper == null || userId == null) {
            return 0;
        }
        Long count = documentMapper.selectCount(new LambdaQueryWrapperX<OaDocumentDO>()
                .eq(OaDocumentDO::getIssuerUserId, userId));
        return count == null ? 0 : count.intValue();
    }

}
