package cn.iocoder.yudao.module.airag.controller.admin.document.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - AI RAG 文档分页 Request VO")
@Data
public class AiragDocumentPageReqVO extends PageParam {

    @Schema(description = "知识库编号", example = "1")
    private Long knowledgeId;

    @Schema(description = "文档名称", example = "Java 开发手册")
    private String name;

    @Schema(description = "处理状态（0待处理 1处理中 2已完成 3失败）", example = "2")
    private Integer status;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
