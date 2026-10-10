package cn.zhicloud.module.fms.controller.admin.config.vo.auxiliary;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FMS 辅助核算项目 Excel 导入 VO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FmsAuxiliaryItemImportExcelVO {

    @ExcelProperty("辅助核算类别编号")
    private Long auxiliaryTypeId;

    @ExcelProperty("项目编码")
    private String code;

    @ExcelProperty("项目名称")
    private String name;

    @ExcelProperty("状态")
    private Integer status;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("规格")
    private String specification;

    @ExcelProperty("单位")
    private String unit;

}
