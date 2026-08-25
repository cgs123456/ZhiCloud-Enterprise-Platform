package cn.zhicloud.module.qms.controller.admin.msa.vo;

import cn.zhicloud.framework.common.validation.InEnum;
import cn.zhicloud.module.qms.enums.qms.MsaStatusEnum;
import cn.zhicloud.module.qms.enums.qms.MsaStudyTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Schema(description = "管理后台 - QMS MSA 研究新增/修改 Request VO")
@Data
public class MsaStudySaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "研究编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "MSA20240101001")
    @NotEmpty(message = "研究编号不能为空")
    private String studyNo;

    @Schema(description = "研究类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @InEnum(MsaStudyTypeEnum.class)
    private Integer studyType;

    @Schema(description = "特性名称", example = "外径")
    private String characteristicName;

    @Schema(description = "测量设备 ID", example = "1024")
    private Long equipmentId;

    @Schema(description = "评价人数量", example = "3")
    private Integer appraiserCount;

    @Schema(description = "试验次数", example = "3")
    private Integer trialCount;

    @Schema(description = "零件数量", example = "10")
    private Integer partCount;

    @Schema(description = "状态", example = "10")
    @InEnum(MsaStatusEnum.class)
    private Integer status;

    @Schema(description = "备注", example = "随便")
    private String remark;

}
