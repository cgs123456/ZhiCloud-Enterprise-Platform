package cn.zhicloud.module.bpm.dal.mysql.task;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.bpm.dal.dataobject.task.BpmProcessInstanceIdempotencyDO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BpmProcessInstanceIdempotencyMapperTest extends BaseDbUnitTest {

    @Resource
    private BpmProcessInstanceIdempotencyMapper mapper;

    @Test
    void uniqueKeyBlocksDuplicateBusinessKey() {
        mapper.insert(newRow(7L, "crm-receivable-audit", "1", null));

        BpmProcessInstanceIdempotencyDO existing = mapper.selectByUniqueKey(7L, "crm-receivable-audit", "1");
        assertNotNull(existing);
        assertNull(existing.getProcessInstanceId());
        assertThrows(DuplicateKeyException.class, () ->
                mapper.insert(newRow(7L, "crm-receivable-audit", "1", "flow-1")));
    }

    @Test
    void selectByUniqueKeyScopesByTenantAndBusinessKey() {
        mapper.insert(newRow(7L, "crm-receivable-audit", "1", "flow-tenant-7"));
        mapper.insert(newRow(8L, "crm-receivable-audit", "1", "flow-tenant-8"));
        mapper.insert(newRow(7L, "crm-invoice-audit", "1", "flow-invoice"));

        assertEquals("flow-tenant-7", mapper.selectByUniqueKey(7L, "crm-receivable-audit", "1").getProcessInstanceId());
        assertEquals("flow-tenant-8", mapper.selectByUniqueKey(8L, "crm-receivable-audit", "1").getProcessInstanceId());
        assertEquals("flow-invoice", mapper.selectByUniqueKey(7L, "crm-invoice-audit", "1").getProcessInstanceId());
        assertNull(mapper.selectByUniqueKey(7L, "crm-receivable-audit", "2"));
    }

    private static BpmProcessInstanceIdempotencyDO newRow(Long tenantId, String processDefinitionKey,
                                                           String businessKey, String processInstanceId) {
        BpmProcessInstanceIdempotencyDO row = new BpmProcessInstanceIdempotencyDO();
        row.setTenantId(tenantId);
        row.setProcessDefinitionKey(processDefinitionKey);
        row.setBusinessKey(businessKey);
        row.setProcessInstanceId(processInstanceId);
        return row;
    }
}
