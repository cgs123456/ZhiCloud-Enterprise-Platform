package cn.zhicloud.module.hr.dal.dataobject.hrm;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * HRM 员工紧急联系人 DO
 *
 * @author zhicloud
 */
@TableName("hr_employee_contact")
@KeySequence("hr_employee_contact_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmEmployeeContactDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 员工编号
     */
    private Long employeeId;
    /**
     * 联系人姓名
     */
    private String name;
    /**
     * 关系
     */
    private String relation;
    /**
     * 联系人电话
     */
    private String phone;
    /**
     * 联系人工作单位
     */
    private String workUnit;
    /**
     * 联系人职务
     */
    private String postName;
    /**
     * 联系人地址
     */
    private String address;
    /**
     * 排序
     */
    private Integer sort;

}
