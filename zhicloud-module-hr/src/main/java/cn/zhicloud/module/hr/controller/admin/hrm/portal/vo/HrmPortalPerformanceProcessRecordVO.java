package cn.zhicloud.module.hr.controller.admin.hrm.portal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 员工自助端 - 绩效流程记录 VO
 *
 * 对齐前端 PerformanceProcessRecordVO 契约。
 *
 * @author zhicloud
 */
@Schema(description = "员工自助端 - 绩效流程记录 VO")
@Data
public class HrmPortalPerformanceProcessRecordVO {

    @Schema(description = "记录编号")
    private Long id;

    @Schema(description = "记录标题")
    private String title;

    @Schema(description = "记录内容")
    private String content;

    @Schema(description = "记录来源：ACTION/BUSINESS/STAGE")
    private String source;

    @Schema(description = "流程状态")
    private Integer status;

    @Schema(description = "操作人姓名")
    private String operatorName;

    @Schema(description = "操作时间")
    private LocalDateTime operateTime;

    @Schema(description = "附件地址列表")
    private List<String> fileUrls;

}
