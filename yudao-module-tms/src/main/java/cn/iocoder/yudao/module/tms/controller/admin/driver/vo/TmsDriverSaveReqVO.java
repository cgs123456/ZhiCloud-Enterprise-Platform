package cn.iocoder.yudao.module.tms.controller.admin.driver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Schema(description = "管理后台 - TMS 司机新增/修改 Request VO")
@Data
public class TmsDriverSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "司机姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotEmpty(message = "司机姓名不能为空")
    private String name;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "驾照号", example = "DL-001")
    private String licenseNo;

    @Schema(description = "准驾车型", example = "A2")
    private String licenseType;

    @Schema(description = "承运商编号", example = "2048")
    private Long carrierId;

    @Schema(description = "状态（10 可用 / 20 运输中 / 30 休假）", example = "10")
    private Integer status;

    @Schema(description = "备注", example = "随便")
    private String remark;

}
