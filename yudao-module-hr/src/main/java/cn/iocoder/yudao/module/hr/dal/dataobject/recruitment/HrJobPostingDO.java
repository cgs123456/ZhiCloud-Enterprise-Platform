package cn.iocoder.yudao.module.hr.dal.dataobject.recruitment;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;

@TableName("hr_job_posting")
@KeySequence("hr_job_posting_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrJobPostingDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long positionId;
    private String title;
    private Integer headcount;
    private String salaryRange;
    private String description;
    private String requirement;
    private Integer status;
    private LocalDate publishDate;
    private LocalDate closeDate;

}