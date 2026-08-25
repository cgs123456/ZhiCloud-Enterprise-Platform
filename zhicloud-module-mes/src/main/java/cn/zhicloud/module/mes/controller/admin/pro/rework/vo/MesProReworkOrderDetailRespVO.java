package cn.zhicloud.module.mes.controller.admin.pro.rework.vo;

import cn.zhicloud.framework.excel.core.annotations.DictFormat;
import cn.zhicloud.framework.excel.core.convert.DictConvert;
import cn.zhicloud.module.mes.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES 返工工单明细 Response VO")
@Data
@ExcelIgnoreUnannotated
public class MesProReworkOrderDetailRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "返工工单 ID", example = "100")
    private Long reworkOrderId;

    @Schema(description = "缺陷描述", example = "尺寸超差 0.5mm")
    @ExcelProperty("缺陷描述")
    private String defectDescription;

    @Schema(description = "缺陷数量", example = "5.00")
    @ExcelProperty("缺陷数量")
    private BigDecimal defectQuantity;

    @Schema(description = "缺陷类型", example = "10")
    @ExcelProperty(value = "缺陷类型", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.MES_DEFECT_TYPE)
    private Integer defectType;

    @Schema(description = "处理方式", example = "10")
    @ExcelProperty(value = "处理方式", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.MES_REPAIR_METHOD)
    private Integer repairMethod;

    @Schema(description = "处理描述", example = "重新焊接")
    @ExcelProperty("处理描述")
    private String repairDescription;

    @Schema(description = "已处理数量", example = "5.00")
    @ExcelProperty("已处理数量")
    private BigDecimal repairedQuantity;

    @Schema(description = "显示顺序", example = "1")
    private Integer sort;

    @Schema(description = "备注", example = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
