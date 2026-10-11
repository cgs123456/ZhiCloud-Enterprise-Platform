package cn.zhicloud.module.hr.dal.dataobject.salary2;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;


/**
 * HRM 薪资项 DO（树形）
 *
 * @author zhicloud
 */
@TableName(value = "hr_salary_option", autoResultMap = false)
@KeySequence("hr_salary_option_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmSalaryOptionDO extends TenantBaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 薪资项编码
     */
    private Integer code;

    /**
     * 父薪资项编码（0=根）
     */
    private Integer parentCode;

    /**
     * 薪资项名称
     */
    private String name;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否系统默认项（不可删）
     */
    private Boolean systemFlag;

    /**
     * 薪资项类型
     */
    private Integer type;

    /**
     * 是否计税
     */
    private Boolean taxEnabled;

    /**
     * 是否显示
     */
    private Boolean visible;

    /**
     * 是否参与计算
     */
    private Boolean calculateEnabled;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 标准薪资项目录编号
     */
    private Long templateId;

    /**
     * 排序
     */
    private Integer sort;

}
