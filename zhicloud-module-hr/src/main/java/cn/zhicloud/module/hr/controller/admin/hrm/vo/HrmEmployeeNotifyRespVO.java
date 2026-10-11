package cn.zhicloud.module.hr.controller.admin.hrm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - HRM 员工通知发送结果 Response VO")
@Data
public class HrmEmployeeNotifyRespVO {

    @Schema(description = "发送成功数量")
    private Integer successCount;

    @Schema(description = "无后台账号跳过数量")
    private Integer skippedCount;

    @Schema(description = "发送失败数量")
    private Integer failureCount;

}
