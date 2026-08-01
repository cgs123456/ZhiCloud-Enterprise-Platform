package cn.iocoder.yudao.module.qms.controller.admin.msa.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - QMS MSA 研究分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MsaStudyPageReqVO extends PageParam {

    @Schema(description = "研究编号", example = "MSA20240101001")
    private String studyNo;

    @Schema(description = "研究类型", example = "10")
    private Integer studyType;

    @Schema(description = "状态", example = "10")
    private Integer status;

}
