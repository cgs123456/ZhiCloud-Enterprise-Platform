package cn.iocoder.yudao.module.mes.controller.app.pro.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "用户 APP - PDA 设备扫描 Response VO")
@Data
public class MesPdaMachineryRespVO {

    @Schema(description = "设备编号", example = "1")
    private Long id;

    @Schema(description = "设备编码", example = "CNC-001")
    private String machineryCode;

    @Schema(description = "设备名称", example = "CNC 加工中心")
    private String machineryName;

    @Schema(description = "规格型号", example = "VMC-850")
    private String specification;

    @Schema(description = "设备状态", example = "0")
    private Integer status;

    @Schema(description = "设备状态名称", example = "正常运行")
    private String statusName;

    @Schema(description = "当前工单编号（如有）", example = "WO202503001")
    private String currentWorkOrderNo;

    @Schema(description = "当前工单名称（如有）", example = "生产 A 产品 1000 件")
    private String currentWorkOrderName;

    @Schema(description = "扫描时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scanTime;

}
