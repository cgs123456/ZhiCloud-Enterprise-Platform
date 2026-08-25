package cn.zhicloud.module.hr.controller.admin.position.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - HR 职位分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HrPositionPageReqVO extends PageParam {

    @Schema(description = "职位编码", example = "P001")
    private String code;

    @Schema(description = "职位名称", example = "Java")
    private String name;

    @Schema(description = "所属部门 ID", example = "2048")
    private Long deptId;

    @Schema(description = "职级", example = "10")
    private Integer level;

}