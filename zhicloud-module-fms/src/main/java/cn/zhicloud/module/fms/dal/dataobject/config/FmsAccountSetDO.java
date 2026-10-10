package cn.zhicloud.module.fms.dal.dataobject.config;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * FMS 账套 DO
 *
 * @author 智云
 */
@TableName("fms_account_set")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FmsAccountSetDO extends TenantBaseDO {

    @TableId
    private Long id;
    /** 公司编码 */
    private String companyCode;
    /** 公司名称 */
    private String companyName;
    /** 公司简介 */
    private String companyProfile;
    /** 所在行业 */
    private String industry;
    /** 所在地 */
    private String location;
    /** 法人代表 */
    private String legalRepresentative;
    /** 法人身份证号 */
    private String legalRepresentativeIdNumber;
    /** 营业执照号 */
    private String businessLicenseNumber;
    /** 组织机构代码 */
    private String organizationCode;
    /** 备注 */
    private String remark;
    /** 联系人 */
    private String contactName;
    /** 办公电话 */
    private String officeTelephone;
    /** 手机号码 */
    private String mobile;
    /** 传真号码 */
    private String faxNumber;
    /** QQ 号码 */
    private String qqNumber;
    /** 邮箱 */
    private String email;
    /** 其他联系方式 */
    private String otherContact;
    /** 详细地址 */
    private String address;
    /** 本位币编号，关联 {@link FmsCurrencyDO#getId()}，初始化后回填 */
    private Long currencyId;
    /** 启用期间，格式 yyyyMM，如 202601 */
    private Integer startTime;
    /** 会计制度 */
    private Integer standard;
    /** 是否已初始化 */
    private Boolean initialized;
    /** 是否默认账套 */
    private Boolean defaultStatus;

}
