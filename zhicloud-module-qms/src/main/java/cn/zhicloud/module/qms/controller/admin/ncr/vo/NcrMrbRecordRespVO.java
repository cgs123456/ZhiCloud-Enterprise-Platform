package cn.zhicloud.module.qms.controller.admin.ncr.vo;

import cn.zhicloud.framework.excel.core.annotations.DictFormat;
import cn.zhicloud.framework.excel.core.convert.DictConvert;
import cn.zhicloud.module.qms.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * QMS MRB 评审记录 Response VO
 *
 * @author 智云
 */
@Schema(description = "管理后台 - QMS MRB 评审记录 Response VO")
@Data
@ExcelIgnoreUnannotated
public class NcrMrbRecordRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "NCR 报告 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("NCR 报告 ID")
    private Long ncrId;

    @Schema(description = "评审日期")
    @ExcelProperty("评审日期")
    private LocalDateTime mrbDate;

    @Schema(description = "评审成员", example = "张三,李四,王五")
    @ExcelProperty("评审成员")
    private String mrbMembers;

    @Schema(description = "决议", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty(value = "决议", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.NCR_MRB_DECISION)
    private Integer decision;

    @Schema(description = "附加条件", example = "返工后需全检")
    @ExcelProperty("附加条件")
    private String conditionTerms;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
