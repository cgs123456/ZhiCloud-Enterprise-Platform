package cn.zhicloud.module.hr.dal.dataobject.insurance;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * HRM 标准参保类型 DO（如深圳 城镇职工）
 *
 * @author zhicloud
 */
@TableName("hr_insurance_standard_type")
@KeySequence("hr_insurance_standard_type_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmInsuranceStandardTypeDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 参保地区编号（system_area.id）
     */
    private Long areaId;
    /**
     * 参保方案编码（URBAN/FLEXIBLE/RURAL）
     */
    private String code;
    /**
     * 参保方案名称（如 城镇职工）
     */
    private String name;
    /**
     * 备注
     */
    private String remark;

}
