package cn.zhicloud.module.hr.controller.admin.hrm.performance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - HRM 绩效结果模板 VO（新增/修改/响应共用）")
@Data
public class HrmPerformanceResultTemplateVO {

    @Schema(description = "结果模板编号")
    private Long id;

    @Schema(description = "结果模板名称")
    @NotEmpty(message = "结果模板名称不能为空")
    private String name;

    @Schema(description = "结果等级列表 [{name,minScore,maxScore,coefficient}]")
    private List<Map<String, Object>> levels;

    @Schema(description = "状态：0开启 1关闭")
    private Integer status;

    @Schema(description = "创建人")
    private String creator;

    @Schema(description = "创建人名称")
    private String creatorName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
