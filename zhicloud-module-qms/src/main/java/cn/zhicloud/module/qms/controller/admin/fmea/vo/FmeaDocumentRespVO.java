package cn.zhicloud.module.qms.controller.admin.fmea.vo;

import cn.zhicloud.framework.excel.core.annotations.DictFormat;
import cn.zhicloud.framework.excel.core.convert.DictConvert;
import cn.zhicloud.module.qms.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - QMS FMEA 文档 Response VO")
@Data
@ExcelIgnoreUnannotated
public class FmeaDocumentRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "FMEA 单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "FMEA20240101001")
    @ExcelProperty("FMEA 单号")
    private String fmeaNo;

    @Schema(description = "FMEA 类型", example = "10")
    @ExcelProperty(value = "FMEA 类型", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.FMEA_TYPE)
    private Integer fmeaType;

    @Schema(description = "产品 ID", example = "1024")
    private Long productId;

    @Schema(description = "工序 ID", example = "2048")
    private Long processId;

    @Schema(description = "版本", example = "v1.0")
    @ExcelProperty("版本")
    private String version;

    @Schema(description = "状态", example = "10")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.FMEA_STATUS)
    private Integer status;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
