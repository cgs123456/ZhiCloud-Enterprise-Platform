package cn.zhicloud.module.hr.controller.admin.hrm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HRM 员工证书 VO（列表 / 新增 / 修改共用）")
@Data
public class HrmEmployeeCertificateVO {

    @Schema(description = "证书编号")
    private Long id;

    @Schema(description = "员工编号")
    @NotNull(message = "员工编号不能为空")
    private Long employeeId;

    @Schema(description = "证书名称")
    @NotEmpty(message = "证书名称不能为空")
    private String name;

    @Schema(description = "证书级别")
    private String level;

    @Schema(description = "证书编码")
    private String no;

    @Schema(description = "有效开始日期")
    private LocalDateTime startTime;

    @Schema(description = "有效结束日期")
    private LocalDateTime endTime;

    @Schema(description = "发证机构")
    private String issuingAuthority;

    @Schema(description = "发证日期")
    private LocalDateTime issuingTime;

    @Schema(description = "备注")
    private String remark;

}
