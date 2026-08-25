package cn.zhicloud.module.mes.controller.admin.md.ecn.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - MES ECN 工程变更单 Response VO")
@Data
public class MesMdEcnOrderRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "ECN 单号", example = "ECN202503001")
    private String no;

    @Schema(description = "变更名称", example = "X 产品 BOM 替换物料")
    private String ecnName;

    @Schema(description = "变更类型（10 新增 BOM 20 修改 BOM 30 删除 BOM 40 替换物料）", example = "20")
    private Integer changeType;

    @Schema(description = "原 BOM 编号", example = "1")
    private Long bomId;

    @Schema(description = "原 BOM 编号（展示用）", example = "BOM001")
    private String bomNo;

    @Schema(description = "新 BOM 编号", example = "2")
    private Long newBomId;

    @Schema(description = "新 BOM 编号（展示用）", example = "BOM002")
    private String newBomNo;

    @Schema(description = "变更原因", example = "客户要求升级材料")
    private String changeReason;

    @Schema(description = "变更说明", example = "替换 BOM 中子件 A 为子件 B")
    private String changeDescription;

    @Schema(description = "状态（10 草稿 20 审核中 30 已批准 40 已驳回 50 已执行）", example = "10")
    private Integer status;

    @Schema(description = "申请人", example = "1")
    private Long applicantUserId;

    @Schema(description = "审批人", example = "2")
    private Long approveUserId;

    @Schema(description = "审批日期")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approveDate;

    @Schema(description = "生效日期", example = "2025-04-01")
    private LocalDate effectiveDate;

    @Schema(description = "备注", example = "备注")
    private String remark;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "变更明细列表")
    private List<MesMdEcnOrderItemRespVO> items;

}
