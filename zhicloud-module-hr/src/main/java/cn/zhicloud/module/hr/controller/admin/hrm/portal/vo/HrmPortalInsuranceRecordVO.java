package cn.zhicloud.module.hr.controller.admin.hrm.portal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 员工自助端 - 社保参保记录 VO
 *
 * 对齐前端 InsuranceRecordVO 契约。
 *
 * @author zhicloud
 */
@Schema(description = "员工自助端 - 社保参保记录 VO")
@Data
public class HrmPortalInsuranceRecordVO {

    @Schema(description = "员工月度社保记录编号")
    private Long id;

    @Schema(description = "月度社保表编号")
    private Long monthRecordId;

    @Schema(description = "员工编号")
    private Long employeeId;

    @Schema(description = "社保方案编号")
    private Long schemeId;

    @Schema(description = "社保方案名称")
    private String schemeName;

    @Schema(description = "社保方案类型：1比例 2固定金额")
    private Integer schemeType;

    @Schema(description = "参保城市")
    private String schemeCity;

    @Schema(description = "年份")
    private Integer year;

    @Schema(description = "月份")
    private Integer month;

    @Schema(description = "个人社保金额")
    private BigDecimal personalInsuranceAmount;

    @Schema(description = "企业社保金额")
    private BigDecimal corporateInsuranceAmount;

    @Schema(description = "个人公积金金额")
    private BigDecimal personalProvidentFundAmount;

    @Schema(description = "企业公积金金额")
    private BigDecimal corporateProvidentFundAmount;

    @Schema(description = "参保状态")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "社保项目列表（仅详情返回）")
    private List<HrmPortalInsuranceProjectVO> projectList;

}
