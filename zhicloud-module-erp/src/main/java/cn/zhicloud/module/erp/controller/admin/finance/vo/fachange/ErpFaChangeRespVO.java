package cn.zhicloud.module.erp.controller.admin.finance.vo.fachange;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - ERP 固定资产变动 Response VO")
@Data
public class ErpFaChangeRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;
    @Schema(description = "固定资产编号", example = "1")
    private Long assetId;
    @Schema(description = "变动类型", example = "10")
    private Integer changeType;
    @Schema(description = "变动类型名称", example = "部门转移")
    private String changeTypeName;
    @Schema(description = "变更前值", example = "100000.00")
    private String beforeValue;
    @Schema(description = "变更后值", example = "120000.00")
    private String afterValue;
    @Schema(description = "变更日期")
    private LocalDate changeDate;
    @Schema(description = "变更原因", example = "资产评估增值")
    private String changeReason;
    @Schema(description = "操作员编号", example = "1")
    private Long operatorId;
    @Schema(description = "审批人编号", example = "2")
    private Long approverId;
    @Schema(description = "状态（10 待审核 / 20 已审核 / 30 已驳回）", example = "10")
    private Integer status;
    @Schema(description = "状态名称", example = "待审核")
    private String statusName;
    @Schema(description = "驳回原因", example = "原值调整依据不足")
    private String rejectReason;
    @Schema(description = "备注", example = "备注")
    private String remark;

}
