package cn.zhicloud.module.qms.controller.admin.electronicsignature.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - QMS 电子签名记录 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ElectronicSignatureLogRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "用户 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("用户 ID")
    private Long userId;

    @Schema(description = "签名含义", example = "批准")
    @ExcelProperty("签名含义")
    private String signatureMeaning;

    @Schema(description = "操作类型", example = "CAPA_CLOSE")
    @ExcelProperty("操作类型")
    private String operationType;

    @Schema(description = "操作内容", example = "关闭 CAPA 文档 ID=1024")
    @ExcelProperty("操作内容")
    private String operationContent;

    @Schema(description = "签名时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("签名时间")
    private LocalDateTime signatureTime;

    @Schema(description = "IP 地址", example = "192.168.1.1")
    @ExcelProperty("IP 地址")
    private String ipAddress;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
