package cn.zhicloud.module.hr.dal.dataobject.recruit;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * HRM 招聘候选人 DO
 *
 * @author zhicloud
 */
@TableName(value = "hr_recruit_candidate", autoResultMap = true)
@KeySequence("hr_recruit_candidate_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmRecruitCandidateDO extends TenantBaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 候选人姓名
     */
    private String name;

    /**
     * 手机号码
     */
    private String mobile;

    /**
     * 性别：1男 2女
     */
    private Integer sex;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 应聘职位编号
     */
    private Long postId;

    /**
     * 用人部门编号
     */
    private Long deptId;

    /**
     * 招聘负责人员工编号
     */
    private Long ownerEmployeeId;

    /**
     * 面试轮次
     */
    private Integer stageNumber;

    /**
     * 工作年限
     */
    private Integer workTime;

    /**
     * 学历
     */
    private Integer education;

    /**
     * 毕业院校
     */
    private String graduateSchool;

    /**
     * 最近工作单位
     */
    private String latestWorkPlace;

    /**
     * 招聘渠道编号
     */
    private Long channelId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 候选人状态：1-8
     */
    private Integer status;

    /**
     * 淘汰原因
     */
    private String eliminate;

    /**
     * 状态更新时间
     */
    private LocalDateTime statusUpdateTime;

    /**
     * 入职时间
     */
    private LocalDateTime entryTime;

    /**
     * 简历附件地址数组
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> resumeUrls;

    /**
     * 当前面试方式
     */
    private Integer interviewType;

    /**
     * 主面试官员工编号
     */
    private Long interviewEmployeeId;

    /**
     * 其他面试官员工编号数组
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> otherInterviewEmployeeIds;

    /**
     * 面试时间
     */
    private LocalDateTime interviewTime;

    /**
     * 面试地址
     */
    private String interviewAddress;

    /**
     * 当前面试结果
     */
    private Integer interviewResult;

    /**
     * 转入的员工编号
     */
    private Long employeeId;

}
