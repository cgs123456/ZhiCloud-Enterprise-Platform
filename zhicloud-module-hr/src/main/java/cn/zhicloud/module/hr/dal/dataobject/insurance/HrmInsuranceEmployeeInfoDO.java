package cn.zhicloud.module.hr.dal.dataobject.insurance;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * HRM 员工参保信息 DO（一员工一条；社保/公积金账号镜像 hr_employee 两列，单一真相）
 *
 * @author zhicloud
 */
@TableName("hr_insurance_employee_info")
@KeySequence("hr_insurance_employee_info_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmInsuranceEmployeeInfoDO extends TenantBaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 员工编号
     */
    private Long employeeId;

    /**
     * 是否本地首次缴纳社保
     */
    private Boolean firstSocialSecurity;

    /**
     * 是否本地首次缴纳公积金
     */
    private Boolean firstAccumulationFund;

    /**
     * 社保起缴月份（epoch 毫秒口径）
     */
    private java.time.LocalDateTime socialSecurityStartMonth;

    /**
     * 社保方案编号
     */
    private Long schemeId;

}
