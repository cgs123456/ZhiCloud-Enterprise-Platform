package cn.zhicloud.module.hr.dal.dataobject.insurance;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * HRM 标准参保项目 DO（如城镇职工 养老保险）
 *
 * @author zhicloud
 */
@TableName("hr_insurance_standard_project")
@KeySequence("hr_insurance_standard_project_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmInsuranceStandardProjectDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 参保类型编号
     */
    private Long typeId;
    /**
     * 参保类型编码（冗余，便于不关联 type 表查询）
     */
    private String typeCode;
    /**
     * 参保地区编号
     */
    private Long areaId;
    /**
     * 项目类型（hrm_insurance_project_type 字典 1-11）
     */
    private Integer type;
    /**
     * 项目名称（如 养老保险）
     */
    private String name;
    /**
     * 缴纳基数（元）
     */
    private BigDecimal baseAmount;
    /**
     * 公司缴纳比例（小数）
     */
    private BigDecimal corporateRate;
    /**
     * 个人缴纳比例（小数）
     */
    private BigDecimal personalRate;
    /**
     * 备注
     */
    private String remark;

}
