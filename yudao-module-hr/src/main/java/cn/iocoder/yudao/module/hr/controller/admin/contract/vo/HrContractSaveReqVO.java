package cn.iocoder.yudao.module.hr.controller.admin.contract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - HR 合同新增/修改 Request VO")
@Data
public class HrContractSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "员工 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "员工不能为空")
    private Long employeeId;

    @Schema(description = "合同编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "HT202401001")
    @NotEmpty(message = "合同编号不能为空")
    private String contractNo;

    @Schema(description = "合同类型 1固定期限 2无固定期限 3完成任务 4实习", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "合同类型不能为空")
    private Integer contractType;

    @Schema(description = "合同开始日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01")
    @NotNull(message = "合同开始日期不能为空")
    private LocalDate startDate;

    @Schema(description = "合同结束日期(无固定期限为空)", example = "2026-12-31")
    private LocalDate endDate;

    @Schema(description = "签订日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01")
    @NotNull(message = "签订日期不能为空")
    private LocalDate signDate;

    @Schema(description = "试用期结束日期", example = "2024-03-31")
    private LocalDate probationEndDate;

    @Schema(description = "岗位 ID", example = "4096")
    private Long positionId;

    @Schema(description = "部门 ID", example = "2048")
    private Long departmentId;

    @Schema(description = "合同约定薪资", example = "10000.00")
    private BigDecimal salary;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "合同附件", example = "https://www.iocoder.cn/file.pdf")
    private String fileUrl;

    @Schema(description = "备注", example = "随便")
    private String remark;

}