package cn.zhicloud.module.qms.controller.admin.training.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import cn.zhicloud.module.qms.enums.qms.QualificationStatusEnum;
import cn.zhicloud.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - QMS 岗位资格新增/修改 Request VO")
@Data
public class QualificationSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "用户 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    @Schema(description = "用户姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotEmpty(message = "用户姓名不能为空")
    private String userName;

    @Schema(description = "岗位 ID", example = "2048")
    private Long postId;

    @Schema(description = "岗位名称", example = "质量工程师")
    private String postName;

    @Schema(description = "资格名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "内审员资格")
    @NotEmpty(message = "资格名称不能为空")
    private String qualificationName;

    @Schema(description = "取得日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate qualifyDate;

    @Schema(description = "到期日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expireDate;

    /**
     * 状态。**服务端只读，不参与写入**：新增由服务层强制置为初始值（§12.235.5）。
     * @InEnum 用于在边界处拒绝垃圾值（此前连枚举约束都没有）。
     */
    @Schema(description = "状态（服务端只读，不参与写入）", example = "10")
    @InEnum(QualificationStatusEnum.class)
    private Integer status;

    @Schema(description = "备注", example = "随便")
    private String remark;

}