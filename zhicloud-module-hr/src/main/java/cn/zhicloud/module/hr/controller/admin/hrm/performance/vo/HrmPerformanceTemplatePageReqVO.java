package cn.zhicloud.module.hr.controller.admin.hrm.performance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import cn.zhicloud.framework.common.pojo.PageParam;

@Schema(description = "管理后台 - HRM 绩效模板分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HrmPerformanceTemplatePageReqVO extends PageParam {

    @Schema(description = "模板名称，模糊匹配")
    private String name;

}
