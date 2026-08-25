package cn.zhicloud.module.tms.controller.admin.driver.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - TMS 司机分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TmsDriverPageReqVO extends PageParam {

    @Schema(description = "司机姓名", example = "张三")
    private String name;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "承运商编号", example = "2048")
    private Long carrierId;

    @Schema(description = "状态（10 可用 / 20 运输中 / 30 休假）", example = "10")
    private Integer status;

}
