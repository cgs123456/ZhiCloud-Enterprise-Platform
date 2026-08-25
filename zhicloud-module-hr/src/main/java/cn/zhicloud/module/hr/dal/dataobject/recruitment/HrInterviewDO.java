package cn.zhicloud.module.hr.dal.dataobject.recruitment;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

@TableName("hr_interview")
@KeySequence("hr_interview_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrInterviewDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long resumeId;
    private Integer interviewRound;
    private Long interviewerId;
    private LocalDateTime interviewTime;
    private Integer interviewType;
    private Integer result;
    private String comment;
    private Integer status;

}