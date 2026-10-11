package cn.zhicloud.module.hr.controller.admin.hrm.portal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 员工自助端 - 首页日历事项 VO
 *
 * 对齐前端 HrmHomeCalendarItemVO 契约。
 *
 * @author zhicloud
 */
@Schema(description = "员工自助端 - 首页日历事项 VO")
@Data
public class HrmPortalCalendarItemVO {

    @Schema(description = "员工个人备忘编号")
    private Long personalNoteId;

    @Schema(description = "事项类型")
    private Integer type;

    @Schema(description = "事项类型名称")
    private String typeName;

    @Schema(description = "事项内容")
    private String content;

    @Schema(description = "关联业务编号")
    private Long typeId;

    @Schema(description = "事项日期（yyyy-MM-dd）")
    private String date;

    @Schema(description = "事项时间")
    private LocalDateTime eventTime;

}
