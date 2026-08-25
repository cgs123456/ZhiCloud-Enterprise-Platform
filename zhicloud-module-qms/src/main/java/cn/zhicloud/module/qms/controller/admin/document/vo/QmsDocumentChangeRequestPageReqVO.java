package cn.zhicloud.module.qms.controller.admin.document.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static cn.zhicloud.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - QMS 文件变更申请分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class QmsDocumentChangeRequestPageReqVO extends PageParam {

    @Schema(description = "受控文档 ID", example = "1024")
    private Long documentId;

    @Schema(description = "变更类型", example = "20")
    private Integer changeType;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "申请人 ID", example = "1024")
    private Long applicantId;

    @Schema(description = "申请日期范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] applyDate;

}
