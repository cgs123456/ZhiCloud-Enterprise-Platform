package cn.zhicloud.module.hr.controller.admin.leave.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - HR 请假单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HrLeaveRequestPageReqVO extends PageParam {

    @Schema(description = "员工 ID", example = "2048")
    private Long employeeId;

    @Schema(description = "假期类型 ID", example = "1")
    private Long leaveTypeId;

    @Schema(description = "状态", example = "0")
    private Integer status;

}