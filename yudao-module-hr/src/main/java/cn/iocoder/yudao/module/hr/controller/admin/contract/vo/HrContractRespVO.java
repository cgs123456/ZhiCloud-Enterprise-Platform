package cn.iocoder.yudao.module.hr.controller.admin.contract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HR 合同 Response VO")
@Data
public class HrContractRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "员工 ID", example = "2048")
    private Long employeeId;

    @Schema(description = "合同编号", example = "HT202401001")
    private String contractNo;

    @Schema(description = "合同类型", example = "1")
    private Integer contractType;

    @Schema(description = "合同开始日期", example = "2024-01-01")
    private LocalDate startDate;

    @Schema(description = "合同结束日期", example = "2026-12-31")
    private LocalDate endDate;

    @Schema(description = "签订日期", example = "2024-01-01")
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

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}