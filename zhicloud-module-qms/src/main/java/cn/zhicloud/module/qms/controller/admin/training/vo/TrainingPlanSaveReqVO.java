package cn.zhicloud.module.qms.controller.admin.training.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import cn.zhicloud.module.qms.enums.qms.TrainingPlanStatusEnum;
import cn.zhicloud.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - QMS 培训计划新增/修改 Request VO")
@Data
public class TrainingPlanSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "计划编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "TP202401001")
    @NotEmpty(message = "计划编号不能为空")
    private String planNo;

    @Schema(description = "计划名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024 年度质量培训")
    @NotEmpty(message = "计划名称不能为空")
    private String planName;

    @Schema(description = "年度", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024")
    @NotNull(message = "年度不能为空")
    private Integer year;

    @Schema(description = "课程名称", example = "FMEA 实战")
    private String courseName;

    @Schema(description = "讲师", example = "张老师")
    private String trainer;

    @Schema(description = "计划日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate planDate;

    /**
     * 状态。**服务端只读，不参与写入**：新增由服务层强制置为初始值（§12.235.5）。
     * @InEnum 用于在边界处拒绝垃圾值（此前连枚举约束都没有）。
     */
    @Schema(description = "状态（服务端只读，不参与写入）", example = "10")
    @InEnum(TrainingPlanStatusEnum.class)
    private Integer status;

    @Schema(description = "备注", example = "随便")
    private String remark;

}