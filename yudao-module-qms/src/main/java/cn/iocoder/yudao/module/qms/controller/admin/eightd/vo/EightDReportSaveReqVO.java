package cn.iocoder.yudao.module.qms.controller.admin.eightd.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Schema(description = "管理后台 - QMS 8D 报告新增/修改 Request VO")
@Data
public class EightDReportSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "8D 报告编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "8D20240101001")
    @NotEmpty(message = "8D 报告编号不能为空")
    private String reportNo;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "XX 产品外观不良 8D 报告")
    @NotEmpty(message = "标题不能为空")
    private String title;

    @Schema(description = "关联 NCR 编号 ID", example = "2048")
    private Long ncrId;

    @Schema(description = "关联 CAPA 编号 ID", example = "3072")
    private Long capaId;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "D1 团队成员", example = "张三、李四、王五")
    private String d1TeamMembers;

    @Schema(description = "D2 问题描述", example = "客户反馈外观划痕")
    private String d2ProblemDescription;

    @Schema(description = "D3 临时遏制措施", example = "100% 全检隔离")
    private String d3InterimAction;

    @Schema(description = "D4 根本原因分析", example = "工装定位松动")
    private String d4RootCause;

    @Schema(description = "D5 永久纠正措施", example = "更换工装并增加防错")
    private String d5PermanentAction;

    @Schema(description = "D6 实施并验证结果", example = "验证连续 3 批合格")
    private String d6ImplementationResult;

    @Schema(description = "D7 预防再发生措施", example = "更新 PFMEA 与控制计划")
    private String d7PreventionAction;

    @Schema(description = "D8 团队表彰", example = "通报表扬并发放奖金")
    private String d8TeamRecognition;

    @Schema(description = "备注", example = "随便")
    private String remark;

}