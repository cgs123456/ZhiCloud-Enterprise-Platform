package cn.zhicloud.module.bpm.dal.mysql.task;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.module.bpm.dal.dataobject.task.BpmProcessInstanceIdempotencyDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BpmProcessInstanceIdempotencyMapper extends BaseMapperX<BpmProcessInstanceIdempotencyDO> {

    /**
     * 以唯一键为串行化点抢占幂等登记。
     *
     * <p>普通插入在多个事务同时冲突时会形成共享锁，再由后续 `FOR UPDATE` 升级为排他锁，
     * 在 MySQL REPEATABLE-READ 下容易死锁。`INSERT ... ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id)`
     * 在一条语句内完成唯一键检查并以排他锁等待已存在的未提交登记，避免锁升级循环。
     *
     * <p>新行写入本次调用的 claim_token；重复命中只保留已有 token，绝不覆盖，
     * 因此后续必须用当前读返回的 claim_token 判断本事务是否真正拥有创建权。
     */
    @Insert("""
            INSERT INTO bpm_process_instance_idempotency
                (tenant_id, process_definition_key, business_key, claim_token)
            VALUES (#{tenantId}, #{processDefinitionKey}, #{businessKey}, #{claimToken})
            ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id)
            """)
    void insertOrLockUniqueKey(BpmProcessInstanceIdempotencyDO idempotency);
    default BpmProcessInstanceIdempotencyDO selectByUniqueKey(Long tenantId, String processDefinitionKey,
                                                              String businessKey) {
        return selectOne(BpmProcessInstanceIdempotencyDO::getTenantId, tenantId,
                BpmProcessInstanceIdempotencyDO::getProcessDefinitionKey, processDefinitionKey,
                BpmProcessInstanceIdempotencyDO::getBusinessKey, businessKey);
    }

    /**
     * 在并发抢占唯一键失败后使用当前读获取登记行。
     *
     * <p>MySQL 的 REPEATABLE-READ 下，普通一致性读可能仍看不到其他事务刚提交的行；
     * {@code SELECT ... FOR UPDATE} 会读取最新已提交版本，并在 winner 尚未提交时等待。
     */
    default BpmProcessInstanceIdempotencyDO selectByUniqueKeyForUpdate(Long tenantId, String processDefinitionKey,
                                                                       String businessKey) {
        return selectOneForUpdate(BpmProcessInstanceIdempotencyDO::getTenantId, tenantId,
                BpmProcessInstanceIdempotencyDO::getProcessDefinitionKey, processDefinitionKey,
                BpmProcessInstanceIdempotencyDO::getBusinessKey, businessKey);
    }
}
