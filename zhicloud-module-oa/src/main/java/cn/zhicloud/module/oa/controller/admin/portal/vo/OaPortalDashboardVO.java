package cn.zhicloud.module.oa.controller.admin.portal.vo;

import cn.zhicloud.module.oa.controller.admin.announcement.vo.OaAnnouncementRespVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - OA 工作台门户 Dashboard Response VO")
@Data
public class OaPortalDashboardVO {

    @Schema(description = "最新公告列表（前 5 条）")
    private List<OaAnnouncementRespVO> announcements;

    @Schema(description = "待办数", example = "0")
    private Integer todoCount;

    @Schema(description = "今日会议数", example = "0")
    private Integer todayMeetingCount;

    @Schema(description = "待审批报销数", example = "0")
    private Integer pendingReimburseCount;

    @Schema(description = "我的报销数", example = "0")
    private Integer myReimburseCount;

    @Schema(description = "我的公文数", example = "0")
    private Integer myDocumentsCount;

    @Schema(description = "已发布知识文章数", example = "0")
    private Integer publishedKnowledgeCount;

}
