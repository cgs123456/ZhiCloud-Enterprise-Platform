package cn.zhicloud.module.aimultiagent.service.skill;

import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import cn.zhicloud.module.aimultiagent.dal.dataobject.MultiAgentSkillDO;
import cn.zhicloud.module.aimultiagent.service.agent.AbstractWorkerAgent;
import cn.zhicloud.module.aimultiagent.service.agent.WorkerAgentRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SkillCatalogValidator} 单元测试。
 *
 * <p>回归目标：该 Runner 原先在无租户上下文的启动线程里直接查 aimultiagent_skill，
 * 被租户拦截器抛「不存在租户编号」后整段被 catch 吞掉，校验从未真正生效（死代码）。
 * 现要求在租户 0（平台全局目录，见 V87__aimultiagent_skill.sql）上下文内执行。
 *
 * @author zhicloud
 */
class SkillCatalogValidatorTest {

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void run_queriesCatalogUnderPlatformTenantZero_andRestoresContext() {
        WorkerAgentRegistry registry = mock(WorkerAgentRegistry.class);
        MultiAgentSkillService skillService = mock(MultiAgentSkillService.class);
        AbstractWorkerAgent worker = mock(AbstractWorkerAgent.class);
        when(worker.getName()).thenReturn("wmsWorker");
        when(worker.getSupportedTools()).thenReturn(Collections.singletonList("wms_receipt_order_list"));
        when(registry.listWorkers()).thenReturn(Collections.singletonList(worker));

        AtomicReference<Long> seenTenantId = new AtomicReference<>();
        when(skillService.getSkillByToolName(anyString())).thenAnswer(invocation -> {
            seenTenantId.set(TenantContextHolder.getTenantId());
            MultiAgentSkillDO skill = new MultiAgentSkillDO();
            skill.setCode("wms:receipt_order_list");
            skill.setStatus(0);
            return skill;
        });

        // 启动线程没有租户上下文
        TenantContextHolder.clear();
        new SkillCatalogValidator(registry, skillService).run(null);

        // 查询确实发生在租户 0（而不是抛异常被吞掉）
        assertEquals(0L, seenTenantId.get());
        verify(skillService).getSkillByToolName("wms_receipt_order_list");
        // 执行后租户上下文被还原
        assertNull(TenantContextHolder.getTenantId());
    }

    @Test
    void run_restoresPreviousTenantContext() {
        WorkerAgentRegistry registry = mock(WorkerAgentRegistry.class);
        MultiAgentSkillService skillService = mock(MultiAgentSkillService.class);
        when(registry.listWorkers()).thenReturn(Collections.emptyList());

        TenantContextHolder.setTenantId(1L);
        new SkillCatalogValidator(registry, skillService).run(null);

        assertEquals(1L, TenantContextHolder.getTenantId());
    }

    @Test
    void run_countsMissingCatalogEntryWithoutThrowing() {
        WorkerAgentRegistry registry = mock(WorkerAgentRegistry.class);
        MultiAgentSkillService skillService = mock(MultiAgentSkillService.class);
        AbstractWorkerAgent worker = mock(AbstractWorkerAgent.class);
        when(worker.getName()).thenReturn("wmsWorker");
        when(worker.getSupportedTools()).thenReturn(Collections.singletonList("wms_unknown_tool"));
        when(registry.listWorkers()).thenReturn(Collections.singletonList(worker));
        when(skillService.getSkillByToolName("wms_unknown_tool")).thenReturn(null);

        TenantContextHolder.clear();
        new SkillCatalogValidator(registry, skillService).run(null);

        verify(skillService).getSkillByToolName("wms_unknown_tool");
        assertNull(TenantContextHolder.getTenantId());
    }
}
