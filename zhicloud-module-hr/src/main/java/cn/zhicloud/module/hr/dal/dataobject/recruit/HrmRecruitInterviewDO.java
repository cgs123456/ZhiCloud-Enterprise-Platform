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
 * HRM 招聘面试 DO
 *
 * @author zhicloud
 */
@TableName(value = "hr_recruit_interview", autoResultMap = true)
@KeySequence("hr_recruit_interview_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmRecruitInterviewDO extends TenantBaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 候选人编号
     */
    private Long candidateId;

    /**
     * 面试方式：1现场 2电话 3视频
     */
    private Integer type;

    /**
     * 面试轮次
     */
    private Integer stageNumber;

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
    private String address;

    /**
     * 备注
     */
    private String remark;

    /**
     * 面试结果：1未完成 2通过 3未通过 4取消
     */
    private Integer result;

    /**
     * 面试评价
     */
    private String evaluate;

    /**
     * 取消原因
     */
    private String cancelReason;

}
