package cn.zhicloud.module.qms.controller.admin.training.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - QMS 岗位资格分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class QualificationPageReqVO extends PageParam {

    @Schema(description = "用户 ID", example = "1024")
    private Long userId;

    @Schema(description = "用户姓名", example = "张三")
    private String userName;

    @Schema(description = "岗位 ID", example = "2048")
    private Long postId;

    @Schema(description = "状态", example = "10")
    private Integer status;

}