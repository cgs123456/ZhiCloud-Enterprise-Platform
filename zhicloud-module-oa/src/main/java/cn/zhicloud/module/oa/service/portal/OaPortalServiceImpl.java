package cn.zhicloud.module.oa.service.portal;

import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.framework.security.core.util.SecurityFrameworkUtils;
import cn.zhicloud.module.oa.controller.admin.announcement.vo.OaAnnouncementRespVO;
import cn.zhicloud.module.oa.controller.admin.portal.vo.OaPortalDashboardVO;
import cn.zhicloud.module.oa.dal.dataobject.announcement.OaAnnouncementDO;
import cn.zhicloud.module.oa.dal.dataobject.document.OaDocumentDO;
import cn.zhicloud.module.oa.dal.dataobject.knowledge.OaKnowledgeArticleDO;
import cn.zhicloud.module.oa.dal.dataobject.meeting.OaMeetingReservationDO;
import cn.zhicloud.module.oa.dal.dataobject.reimburse.OaReimburseDO;
import cn.zhicloud.module.oa.dal.mysql.document.OaDocumentMapper;
import cn.zhicloud.module.oa.dal.mysql.knowledge.OaKnowledgeArticleMapper;
import cn.zhicloud.module.oa.dal.mysql.meeting.OaMeetingReservationMapper;
import cn.zhicloud.module.oa.dal.mysql.reimburse.OaReimburseMapper;
import cn.zhicloud.module.oa.service.announcement.OaAnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
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

    /**
     * 会议预约「已取消」状态。取值为 {@code OaMeetingReservationServiceImpl} 的私有常量
     * 之外唯一一处硬编码——那边是 30=已取消（mapper 的 selectConflictList 也按 30 排除），
     * 此处需排除已取消会议，故必须重复一次。
     *
     * <p><b>为什么重复而不是复用</b>：那个常量在 Service 里是 private，
     * 提成公共常量要改另一个类的可见性，影响面大于收益。状态码在 DO 注释里已有语义，
     * 但 DO 上没有枚举（本模块会议域未建枚举）。待会议域补枚举时一并收敛。</p>
     */
    private static final int MEETING_STATUS_CANCELED = 30;

    /**
     * 知识文章「已发布」状态（10 草稿 / 20 已发布 / 30 已下架，见 {@code OaKnowledgeArticleDO}）。
     * 同上，知识域尚无枚举，先按 DO 注释取值。
     */
    private static final int KNOWLEDGE_STATUS_PUBLISHED = 20;

    @Autowired(required = false)
    private OaAnnouncementService announcementService;
    @Autowired(required = false)
    private OaReimburseMapper reimburseMapper;
    @Autowired(required = false)
    private OaDocumentMapper documentMapper;
    @Autowired(required = false)
    private OaKnowledgeArticleMapper knowledgeArticleMapper;
    @Autowired(required = false)
    private OaMeetingReservationMapper meetingReservationMapper;

    @Override
    public OaPortalDashboardVO getDashboard() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        OaPortalDashboardVO dashboard = new OaPortalDashboardVO();
        // 最新公告（前 5 条）
        dashboard.setAnnouncements(getLatestAnnouncements());
        // 待办数：需要 BPM 侧暴露「当前用户待办任务查询」SPI（BpmProcessInstanceApi /
        // BpmProcessTaskApi 目前都没有该方法），OA 不能越界访问 BPM DAO。保持 0。
        dashboard.setTodoCount(0);
        // 今日会议数：本模块 OaMeetingReservationService/Mapper 已完整实现，直接统计
        dashboard.setTodayMeetingCount(countTodayMeetings());
        // 待审批报销数：依赖 BPM task API 获取当前用户待审批任务，暂占位
        dashboard.setPendingReimburseCount(0);
        // 我的报销数
        dashboard.setMyReimburseCount(countMyReimburses(userId));
        // 我的公文数
        dashboard.setMyDocumentsCount(countMyDocuments(userId));
        // 已发布知识文章数：同今日会议数，知识库已实现，直接统计
        dashboard.setPublishedKnowledgeCount(countPublishedKnowledgeArticles());
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

    /**
     * 今日会议数：统计开始时间落在「今天 00:00 ~ 明天 00:00」内的预约。
     *
     * <p>不含已取消（status=30）——与 {@code OaMeetingReservationMapper#selectConflictList}
     * 的口径一致，避免把取消掉的会议算进工作台。</p>
     */
    private Integer countTodayMeetings() {
        if (meetingReservationMapper == null) {
            return 0;
        }
        LocalDate today = LocalDate.now();
        Long count = meetingReservationMapper.selectCount(new LambdaQueryWrapperX<OaMeetingReservationDO>()
                .ge(OaMeetingReservationDO::getStartTime, today.atStartOfDay())
                .lt(OaMeetingReservationDO::getStartTime, today.plusDays(1).atStartOfDay())
                .ne(OaMeetingReservationDO::getStatus, MEETING_STATUS_CANCELED));
        return count == null ? 0 : count.intValue();
    }

    /**
     * 已发布知识文章数：status=20（10 草稿 / 20 已发布 / 30 已下架，见 DO 注释）。
     */
    private Integer countPublishedKnowledgeArticles() {
        if (knowledgeArticleMapper == null) {
            return 0;
        }
        Long count = knowledgeArticleMapper.selectCount(new LambdaQueryWrapperX<OaKnowledgeArticleDO>()
                .eq(OaKnowledgeArticleDO::getStatus, KNOWLEDGE_STATUS_PUBLISHED));
        return count == null ? 0 : count.intValue();
    }

}
