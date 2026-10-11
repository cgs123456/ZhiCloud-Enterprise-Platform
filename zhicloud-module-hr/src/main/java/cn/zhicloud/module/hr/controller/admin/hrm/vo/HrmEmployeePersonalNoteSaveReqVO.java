package cn.zhicloud.module.hr.controller.admin.hrm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HRM 员工个人备忘创建 Request VO")
@Data
public class HrmEmployeePersonalNoteSaveReqVO {

    @Schema(description = "备忘内容")
    @NotEmpty(message = "备忘内容不能为空")
    @Size(max = 255, message = "备忘内容不能超过 255 个字符")
    private String content;

    @Schema(description = "提醒时间")
    private LocalDateTime reminderTime;

}
