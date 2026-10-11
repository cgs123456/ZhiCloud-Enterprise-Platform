package cn.zhicloud.module.hr.controller.admin.hrm.recruit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Schema(description = "管理后台 - HRM 招聘淘汰原因保存 Request VO")
@Data
public class HrmRecruitEliminateReasonSaveReqVO {

    @Schema(description = "淘汰原因列表")
    @NotNull(message = "淘汰原因不能为空")
    private List<String> reasons;

}
