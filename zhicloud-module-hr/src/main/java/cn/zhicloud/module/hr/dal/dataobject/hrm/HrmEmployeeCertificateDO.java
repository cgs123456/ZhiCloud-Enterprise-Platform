package cn.zhicloud.module.hr.dal.dataobject.hrm;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * HRM 员工证书 DO
 *
 * @author zhicloud
 */
@TableName("hr_employee_certificate")
@KeySequence("hr_employee_certificate_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmEmployeeCertificateDO extends TenantBaseDO {

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
     * 证书名称
     */
    private String name;
    /**
     * 证书级别
     */
    private String level;
    /**
     * 证书编码
     */
    private String no;
    /**
     * 有效开始日期
     */
    private LocalDateTime startTime;
    /**
     * 有效结束日期
     */
    private LocalDateTime endTime;
    /**
     * 发证机构
     */
    private String issuingAuthority;
    /**
     * 发证日期
     */
    private LocalDateTime issuingTime;
    /**
     * 备注
     */
    private String remark;
    /**
     * 排序
     */
    private Integer sort;

}
