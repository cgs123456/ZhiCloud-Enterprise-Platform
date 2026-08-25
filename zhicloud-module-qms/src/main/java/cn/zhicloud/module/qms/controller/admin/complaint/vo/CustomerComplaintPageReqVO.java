package cn.zhicloud.module.qms.controller.admin.complaint.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - QMS 客户投诉分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CustomerComplaintPageReqVO extends PageParam {

    @Schema(description = "投诉编号", example = "CP20240101001")
    private String complaintNo;

    @Schema(description = "客户 ID", example = "2048")
    private Long customerId;

    @Schema(description = "产品 ID", example = "1024")
    private Long productId;

    @Schema(description = "处理方式", example = "10")
    private Integer handleType;

    @Schema(description = "状态", example = "10")
    private Integer status;

}