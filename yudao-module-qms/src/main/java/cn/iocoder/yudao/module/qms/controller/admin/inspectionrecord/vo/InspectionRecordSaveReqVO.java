package cn.iocoder.yudao.module.qms.controller.admin.inspectionrecord.vo;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.qms.enums.qms.InspectionResultEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - QMS 检验记录新增/修改 Request VO")
@Data
public class InspectionRecordSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "检验单 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "检验单 ID 不能为空")
    private Long orderId;

    @Schema(description = "检验项目 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "3072")
    @NotNull(message = "检验项目 ID 不能为空")
    private Long itemId;

    @Schema(description = "实测值", example = "10.02")
    private String measuredValue;

    @Schema(description = "检验结果", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "检验结果不能为空")
    @InEnum(InspectionResultEnum.class)
    private Integer result;

    @Schema(description = "检验员", example = "芋头")
    private String inspector;

    @Schema(description = "检验时间")
    private LocalDateTime inspectTime;

    @Schema(description = "备注", example = "随便")
    private String remark;

}
