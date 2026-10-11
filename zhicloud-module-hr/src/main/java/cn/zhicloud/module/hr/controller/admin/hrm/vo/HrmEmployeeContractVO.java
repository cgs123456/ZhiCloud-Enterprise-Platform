package cn.zhicloud.module.hr.controller.admin.hrm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - HRM 员工合同 VO（列表 / 新增 / 修改共用）")
@Data
public class HrmEmployeeContractVO {

    @Schema(description = "合同编号")
    private Long id;

    @Schema(description = "员工编号")
    @NotNull(message = "员工编号不能为空")
    private Long employeeId;

    @Schema(description = "合同编码")
    private String no;

    @Schema(description = "合同类型：1劳动合同 2保密协议 3竞业限制 4其他")
    private Integer type;

    @Schema(description = "合同开始日期")
    private LocalDateTime startTime;

    @Schema(description = "合同结束日期")
    private LocalDateTime endTime;

    @Schema(description = "合同期限（月）")
    private Integer term;

    @Schema(description = "合同状态：0未生效 1已生效 2已到期 3已解除")
    private Integer status;

    @Schema(description = "签约公司")
    private String signCompany;

    @Schema(description = "合同签订日期")
    private LocalDateTime signTime;

    @Schema(description = "是否到期提醒")
    private Boolean expireRemind;

    @Schema(description = "附件地址数组")
    private List<String> fileUrls;

    @Schema(description = "备注")
    private String remark;

}
