package cn.zhicloud.module.qms.controller.admin.inspectionrecord.vo;

import cn.zhicloud.framework.excel.core.annotations.DictFormat;
import cn.zhicloud.framework.excel.core.convert.DictConvert;
import cn.zhicloud.module.qms.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - QMS 检验记录 Response VO")
@Data
@ExcelIgnoreUnannotated
public class InspectionRecordRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "检验单 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @ExcelProperty("检验单 ID")
    private Long orderId;

    @Schema(description = "检验项目 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "3072")
    @ExcelProperty("检验项目 ID")
    private Long itemId;

    @Schema(description = "实测值", example = "10.02")
    @ExcelProperty("实测值")
    private String measuredValue;

    @Schema(description = "检验结果", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty(value = "检验结果", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.INSPECTION_RESULT)
    private Integer result;

    @Schema(description = "检验员", example = "芋头")
    @ExcelProperty("检验员")
    private String inspector;

    @Schema(description = "检验时间")
    @ExcelProperty("检验时间")
    private LocalDateTime inspectTime;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
