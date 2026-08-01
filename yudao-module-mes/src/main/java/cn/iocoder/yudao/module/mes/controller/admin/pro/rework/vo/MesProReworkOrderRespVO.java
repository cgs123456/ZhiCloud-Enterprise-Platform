package cn.iocoder.yudao.module.mes.controller.admin.pro.rework.vo;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.mes.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES 返工工单 Response VO")
@Data
@ExcelIgnoreUnannotated
public class MesProReworkOrderRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "返工工单号", example = "RW-001")
    @ExcelProperty("返工工单号")
    private String code;

    @Schema(description = "原工单 ID", example = "100")
    private Long originalWorkOrderId;

    @Schema(description = "原工单号", example = "WO-001")
    @ExcelProperty("原工单号")
    private String originalWorkOrderCode;

    @Schema(description = "产品 ID", example = "200")
    private Long productId;

    @Schema(description = "产品编码", example = "P-001")
    @ExcelProperty("产品编码")
    private String productCode;

    @Schema(description = "产品名称", example = "电路板 A")
    @ExcelProperty("产品名称")
    private String productName;

    @Schema(description = "返工数量", example = "10.00")
    @ExcelProperty("返工数量")
    private BigDecimal reworkQuantity;

    @Schema(description = "返工原因", example = "尺寸超差")
    @ExcelProperty("返工原因")
    private String reworkReason;

    @Schema(description = "返工类型", example = "10")
    @ExcelProperty(value = "返工类型", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.MES_PRO_REWORK_TYPE)
    private Integer reworkType;

    @Schema(description = "返工工序 ID", example = "300")
    private Long reworkProcessId;

    @Schema(description = "返工工序名称", example = "焊接")
    @ExcelProperty("返工工序名称")
    private String reworkProcessName;

    @Schema(description = "状态", example = "10")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.MES_PRO_REWORK_STATUS)
    private Integer status;

    @Schema(description = "责任人 ID", example = "400")
    private Long responsiblePersonId;

    @Schema(description = "责任部门 ID", example = "500")
    private Long responsibleDeptId;

    @Schema(description = "计划开始时间")
    @ExcelProperty("计划开始时间")
    private LocalDateTime plannedStartTime;

    @Schema(description = "计划结束时间")
    @ExcelProperty("计划结束时间")
    private LocalDateTime plannedEndTime;

    @Schema(description = "实际开始时间")
    @ExcelProperty("实际开始时间")
    private LocalDateTime actualStartTime;

    @Schema(description = "实际结束时间")
    @ExcelProperty("实际结束时间")
    private LocalDateTime actualEndTime;

    @Schema(description = "显示顺序", example = "0")
    private Integer sort;

    @Schema(description = "备注", example = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
