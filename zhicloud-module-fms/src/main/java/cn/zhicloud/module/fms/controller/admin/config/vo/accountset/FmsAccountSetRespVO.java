package cn.zhicloud.module.fms.controller.admin.config.vo.accountset;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - FMS 账套 Response VO")
@Data
public class FmsAccountSetRespVO {

    @Schema(description = "账套编号")
    private Long id;

    @Schema(description = "公司编码")
    private String companyCode;

    @Schema(description = "公司名称")
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

    @Schema(description = "本位币编号")
    private Long currencyId;

    @Schema(description = "启用期间，格式 yyyyMM")
    private Integer startTime;

    @Schema(description = "会计制度")
    private Integer standard;

    @Schema(description = "是否已初始化")
    private Boolean initialized;

    @Schema(description = "是否默认账套")
    private Boolean defaultStatus;

    @Schema(description = "是否账套创建人")
    private Boolean founder;

    @Schema(description = "当前用户的成员权限级别")
    private Integer level;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

}
