package cn.iocoder.yudao.module.qms.controller.admin.training.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - QMS 岗位资格 Response VO")
@Data
public class QualificationRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "用户 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long userId;

    @Schema(description = "用户姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    private String userName;

    @Schema(description = "岗位 ID", example = "2048")
    private Long postId;

    @Schema(description = "岗位名称", example = "质量工程师")
    private String postName;

    @Schema(description = "资格名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "内审员资格")
    private String qualificationName;

    @Schema(description = "取得日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate qualifyDate;

    @Schema(description = "到期日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expireDate;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

}