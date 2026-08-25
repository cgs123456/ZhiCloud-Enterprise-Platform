package cn.zhicloud.module.qms.controller.admin.document.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static cn.zhicloud.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - QMS 文档分发记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class QmsDocumentDistributePageReqVO extends PageParam {

    @Schema(description = "受控文档 ID", example = "1024")
    private Long documentId;

    @Schema(description = "分发对象", example = "质量部")
    private String distributeTo;

    @Schema(description = "签收人", example = "张三")
    private String receivedBy;

    @Schema(description = "分发日期范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] distributeDate;

}
