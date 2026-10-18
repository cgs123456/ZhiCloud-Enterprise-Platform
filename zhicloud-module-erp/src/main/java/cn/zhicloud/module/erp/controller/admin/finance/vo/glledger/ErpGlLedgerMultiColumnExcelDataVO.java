package cn.zhicloud.module.erp.controller.admin.finance.vo.glledger;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理后台 - ERP 账表多栏账 Excel 数据 VO（动态表头）
 *
 * <p>框架 {@code ExcelUtils} 仅支持类头（{@code @ExcelProperty} 固定列），多栏账的专栏随所选科目的
 * 直接子科目变化，无法用类头表达；本 VO 以「表头 + 行」形态交给控制器的动态写出方法处理
 * （单场景直接实现，不扩框架工具类，见 ADR 266 §8-11）。
 *
 * @author 智云
 */
@Data
@Schema(description = "管理后台 - ERP 账表多栏账 Excel 数据 VO（动态表头）")
public class ErpGlLedgerMultiColumnExcelDataVO {

    @Schema(description = "表头（每列为单元素列表）", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private List<List<String>> head = new ArrayList<>();

    @Schema(description = "行数据（与表头列一一对应）", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private List<List<Object>> rows = new ArrayList<>();

}
