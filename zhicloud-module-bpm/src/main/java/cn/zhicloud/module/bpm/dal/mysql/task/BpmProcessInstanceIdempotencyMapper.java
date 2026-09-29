package cn.zhicloud.module.bpm.dal.mysql.task;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.module.bpm.dal.dataobject.task.BpmProcessInstanceIdempotencyDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BpmProcessInstanceIdempotencyMapper extends BaseMapperX<BpmProcessInstanceIdempotencyDO> {

    default BpmProcessInstanceIdempotencyDO selectByUniqueKey(Long tenantId, String processDefinitionKey,
                                                              String businessKey) {
        return selectOne(BpmProcessInstanceIdempotencyDO::getTenantId, tenantId,
                BpmProcessInstanceIdempotencyDO::getProcessDefinitionKey, processDefinitionKey,
                BpmProcessInstanceIdempotencyDO::getBusinessKey, businessKey);
    }
}
