package cn.iocoder.yudao.module.qms.controller.admin.training.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - QMS 培训记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TrainingRecordPageReqVO extends PageParam {

    @Schema(description = "记录编号", example = "TR202401001")
    private String recordNo;

    @Schema(description = "培训计划 ID", example = "2048")
    private Long planId;

    @Schema(description = "参训人员 ID", example = "1024")
    private Long traineeId;

    @Schema(description = "状态", example = "10")
    private Integer status;

}