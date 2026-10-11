package cn.zhicloud.module.hr.dal.dataobject.recruit;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * HRM 招聘职位 DO
 *
 * @author zhicloud
 */
@TableName(value = "hr_recruit_post", autoResultMap = true)
@KeySequence("hr_recruit_post_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmRecruitPostDO extends TenantBaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 职位名称
     */
    private String postName;

    /**
     * 用人部门编号
     */
    private Long deptId;

    /**
     * 工作性质：1全职 2实习 3兼职
     */
    private Integer jobNature;

    /**
     * 工作城市地区编号
     */
    private Long areaId;

    /**
     * 招聘人数
     */
    private Integer recruitNum;

    /**
     * 招聘原因
     */
    private String reason;

    /**
     * 工作经验要求
     */
    private Integer workTime;

    /**
     * 学历要求
     */
    private Integer educationRequire;

    /**
     * 最低薪资（-1 面议）
     */
    private BigDecimal minSalary;

    /**
     * 最高薪资（-1 面议）
     */
    private BigDecimal maxSalary;

    /**
     * 薪资单位：1元/月 2元/年（-1 面议）
     */
    private Integer salaryUnit;

    /**
     * 最小年龄（-1 不限）
     */
    private Integer minAge;

    /**
     * 最大年龄（-1 不限）
     */
    private Integer maxAge;

    /**
     * 最迟到岗时间
     */
    private LocalDateTime latestEntryTime;

    /**
     * 招聘负责人员工编号
     */
    private Long ownerEmployeeId;

    /**
     * 面试官员工编号数组
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> interviewEmployeeIds;

    /**
     * 职位描述
     */
    private String description;

    /**
     * 紧急程度：1紧急 2一般
     */
    private Integer emergencyLevel;

    /**
     * 职位类型编号
     */
    private Long postTypeId;

    /**
     * 职位状态：0停止招聘 1招聘中
     */
    private Integer status;

    /**
     * 停止原因
     */
    private String stopReason;

}
