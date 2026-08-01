package cn.iocoder.yudao.module.hr.dal.dataobject.position;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.hr.enums.position.HrPositionLevelEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * HR 职位 DO
 *
 * @author yudao
 */
@TableName("hr_position")
@KeySequence("hr_position_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrPositionDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 职位编码
     */
    private String code;
    /**
     * 职位名称
     */
    private String name;
    /**
     * 所属部门 ID
     */
    private Long deptId;
    /**
     * 职级
     *
     * 枚举 {@link HrPositionLevelEnum}
     */
    private Integer level;
    /**
     * 基本工资
     */
    private BigDecimal baseSalary;
    /**
     * 备注
     */
    private String remark;
    /**
     * 排序
     */
    private Integer sort;

}