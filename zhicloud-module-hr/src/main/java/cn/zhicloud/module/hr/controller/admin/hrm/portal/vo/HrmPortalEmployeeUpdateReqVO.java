package cn.zhicloud.module.hr.controller.admin.hrm.portal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 员工自助端 - 员工档案自助修改 Request VO
 *
 * 仅允许修改可自助编辑的字段子集，对齐前端 HrmPortalEmployeeUpdateReqVO。
 *
 * @author zhicloud
 */
@Schema(description = "员工自助端 - 员工档案自助修改 Request VO")
@Data
public class HrmPortalEmployeeUpdateReqVO {

    @Schema(description = "员工姓名")
    private String name;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "国家或地区")
    private String country;

    @Schema(description = "民族")
    private String nation;

    @Schema(description = "证件类型：1身份证 2港澳通行证 3台湾通行证 4护照 5其他")
    private Integer idType;

    @Schema(description = "证件号码")
    private String idNumber;

    @Schema(description = "性别：1男 2女")
    private Integer sex;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "籍贯")
    private String nativePlace;

    @Schema(description = "出生时间")
    private LocalDateTime birthday;

    @Schema(description = "户籍地址")
    private String address;

    @Schema(description = "最高学历（1-12）")
    private Integer highestEducation;

}
