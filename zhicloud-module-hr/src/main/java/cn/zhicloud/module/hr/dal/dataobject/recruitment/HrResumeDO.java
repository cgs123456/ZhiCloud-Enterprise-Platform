package cn.zhicloud.module.hr.dal.dataobject.recruitment;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@TableName("hr_resume")
@KeySequence("hr_resume_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrResumeDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long jobPostingId;
    private String candidateName;
    private String phone;
    private String email;
    private String education;
    private Integer experienceYears;
    private String resumeUrl;
    private Integer status;
    private String remark;

}