package cn.zhicloud.module.bpm.dal.dataobject.task;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName(value = "bpm_process_instance_idempotency", autoResultMap = true)
@KeySequence("bpm_process_instance_idempotency_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BpmProcessInstanceIdempotencyDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String processDefinitionKey;
    private String businessKey;
    private String processInstanceId;
}
