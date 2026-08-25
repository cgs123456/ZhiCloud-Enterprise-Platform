package cn.zhicloud.module.erp.controller.admin.finance.vo.period;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ERP 会计期间 Response VO（P0-6）
 *
 * @author 智云
 */
@Schema(description = "管理后台 - ERP 会计期间 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpPeriodRespVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "年度")
    @ExcelProperty("年度")
    private Integer year;

    @Schema(description = "月份")
    @ExcelProperty("月份")
    private Integer month;

    @Schema(description = "期间编码")
    @ExcelProperty("期间编码")
    private String code;

    @Schema(description = "起始日期")
    @ExcelProperty("起始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    @ExcelProperty("结束日期")
    private LocalDate endDate;

    @Schema(description = "状态")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "状态名称")
    @ExcelProperty("状态名称")
    private String statusName;

    @Schema(description = "关账人")
    @ExcelProperty("关账人")
    private String closedBy;

    @Schema(description = "关账时间")
    @ExcelProperty("关账时间")
    private LocalDateTime closedTime;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
