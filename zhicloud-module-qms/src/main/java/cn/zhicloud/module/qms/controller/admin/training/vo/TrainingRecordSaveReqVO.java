package cn.zhicloud.module.qms.controller.admin.training.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - QMS 培训记录新增/修改 Request VO")
@Data
public class TrainingRecordSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "TR202401001")
    @NotEmpty(message = "记录编号不能为空")
    private String recordNo;

    @Schema(description = "培训计划 ID", example = "2048")
    private Long planId;

    @Schema(description = "参训人员 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "参训人员 ID 不能为空")
    private Long traineeId;

    @Schema(description = "参训人员姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotEmpty(message = "参训人员姓名不能为空")
    private String traineeName;

    @Schema(description = "成绩", example = "95.5")
    private BigDecimal score;

    @Schema(description = "是否通过（0 否 1 是）", example = "1")
    private Integer passed;

    @Schema(description = "证书编号", example = "CERT202401001")
    private String certificateNo;

    @Schema(description = "证书到期日")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate certificateExpireDate;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "备注", example = "随便")
    private String remark;

}