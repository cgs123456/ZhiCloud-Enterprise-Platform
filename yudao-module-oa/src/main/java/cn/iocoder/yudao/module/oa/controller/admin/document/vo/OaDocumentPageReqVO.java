package cn.iocoder.yudao.module.oa.controller.admin.document.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Schema(description = "管理后台 - OA 公文分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OaDocumentPageReqVO extends PageParam {

    @Schema(description = "公文编号", example = "GW20240101001")
    private String no;

    @Schema(description = "标题", example = "关于季度复盘的通知")
    private String title;

    @Schema(description = "公文类型", example = "10")
    private Integer documentType;

    @Schema(description = "紧急程度", example = "10")
    private Integer urgency;

    @Schema(description = "保密级别", example = "10")
    private Integer confidentiality;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "发文人 ID", example = "2048")
    private Long issuerUserId;

    @Schema(description = "发文日期范围")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate[] issueDate;

    @Schema(description = "核稿人 ID", example = "2049")
    private Long reviewerUserId;

    @Schema(description = "签发人 ID", example = "2050")
    private Long signerUserId;

    @Schema(description = "归档人 ID", example = "2051")
    private Long archiverUserId;

    @Schema(description = "归档编号", example = "ARC20240101001")
    private String archiveNo;

}
