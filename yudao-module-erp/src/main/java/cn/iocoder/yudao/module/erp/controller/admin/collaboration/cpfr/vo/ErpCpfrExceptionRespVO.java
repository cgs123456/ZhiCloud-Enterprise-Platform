package cn.iocoder.yudao.module.erp.controller.admin.collaboration.cpfr.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP CPFR 异常 Response VO")
@Data
public class ErpCpfrExceptionRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "预测编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long forecastId;

    @Schema(description = "异常类型（10 预测偏差超限 / 20 库存异常 / 30 补货异常）", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer exceptionType;

    @Schema(description = "异常描述", example = "预测偏差超过 20%")
    private String exceptionDescription;

    @Schema(description = "处理状态（10 待处理 / 20 处理中 / 30 已解决）", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer handlingStatus;

    @Schema(description = "处理人编号", example = "1")
    private Long handlerUserId;

    @Schema(description = "处理时间")
    private LocalDateTime handlingTime;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
