package cn.iocoder.yudao.module.ai.controller.admin.prompt.vo;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - AI Prompt 模板的分页 Request VO")
@Data
public class AiPromptTemplatePageReqVO extends PageParam {

    @Schema(description = "模板名称", example = "RAG")
    private String name;

    @Schema(description = "模板编码", example = "rag_qa_system")
    private String code;

    @Schema(description = "分类", example = "RAG")
    @InEnum(cn.iocoder.yudao.module.ai.enums.prompt.AiPromptTemplateCategoryEnum.class)
    private String category;

    @Schema(description = "状态", example = "0")
    @InEnum(CommonStatusEnum.class)
    private Integer status;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
