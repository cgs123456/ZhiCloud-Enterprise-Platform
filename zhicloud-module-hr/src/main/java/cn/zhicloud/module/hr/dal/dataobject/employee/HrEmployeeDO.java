package cn.zhicloud.module.hr.dal.dataobject.employee;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import cn.zhicloud.module.hr.enums.employee.HrEmployeeStatusEnum;
import cn.zhicloud.module.hr.enums.employee.HrEmploymentTypeEnum;
import cn.zhicloud.module.hr.enums.employee.HrGenderEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;

/**
 * HR 员工档案 DO
 *
 * @author zhicloud
 */
@TableName("hr_employee")
@KeySequence("hr_employee_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrEmployeeDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 工号
     */
    private String empNo;
    /**
     * 姓名
     */
    private String name;
    /**
     * 性别
     *
     * 枚举 {@link HrGenderEnum}
     */
    private Integer gender;
    /**
     * 出生日期
     */
    private LocalDate birthDate;
    /**
     * 身份证号
     */
    private String idCard;
    /**
     * 联系电话
     */
    private String phone;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 部门 ID
     */
    private Long deptId;
    /**
     * 职位 ID
     */
    private Long positionId;
    /**
     * 入职日期
     */
    private LocalDate hireDate;
    /**
     * 离职日期
     */
    private LocalDate leaveDate;
    /**
     * 状态
     *
     * 枚举 {@link HrEmployeeStatusEnum}
     */
    private Integer status;
    /**
     * 用工类型
     *
     * 枚举 {@link HrEmploymentTypeEnum}
     */
    private Integer employmentType;
    /**
     * 备注
     */
    private String remark;
    /**
     * 排序
     */
    private Integer sort;

}