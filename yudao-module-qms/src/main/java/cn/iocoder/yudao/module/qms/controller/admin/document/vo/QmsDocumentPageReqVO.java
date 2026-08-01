package cn.iocoder.yudao.module.qms.controller.admin.document.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - QMS 受控文档分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class QmsDocumentPageReqVO extends PageParam {

    @Schema(description = "文件编号", example = "QM-PR-001")
    private String docNo;

    @Schema(description = "标题", example = "质量手册")
    private String title;

    @Schema(description = "文件类型", example = "10")
    private Integer docType;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "归属部门 ID", example = "2048")
    private Long ownerDeptId;

    @Schema(description = "生效日期范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] effectiveDate;

}
