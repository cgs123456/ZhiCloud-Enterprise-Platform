package cn.zhicloud.module.fms.controller.admin.config.vo.accountset;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - FMS 账套创建/修改 Request VO")
@Data
public class FmsAccountSetSaveReqVO {

    @Schema(description = "账套编号")
    private Long id;

    @Schema(description = "公司编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "ZC001")
    @NotBlank(message = "公司编码不能为空")
    private String companyCode;

    @Schema(description = "公司名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "智云科技有限公司")
    @NotBlank(message = "公司名称不能为空")
    private String companyName;

    @Schema(description = "公司简介")
    private String companyProfile;

    @Schema(description = "所在行业")
    private String industry;

    @Schema(description = "所在地")
    private String location;

    @Schema(description = "法人代表")
    private String legalRepresentative;

    @Schema(description = "法人身份证号")
    private String legalRepresentativeIdNumber;

    @Schema(description = "营业执照号")
    private String businessLicenseNumber;

    @Schema(description = "组织机构代码")
    private String organizationCode;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "办公电话")
    private String officeTelephone;

    @Schema(description = "手机号码")
    private String mobile;

    @Schema(description = "传真号码")
    private String faxNumber;

    @Schema(description = "QQ 号码")
    private String qqNumber;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "其他联系方式")
    private String otherContact;

    @Schema(description = "详细地址")
    private String address;

}
