package cn.zhicloud.module.crm.service.receivable;

import cn.hutool.extra.spring.SpringUtil;
import cn.zhicloud.framework.datasource.config.ZhiCloudDataSourceAutoConfiguration;
import cn.zhicloud.framework.integration.service.EventOutboxService;
import cn.zhicloud.framework.mybatis.config.ZhiCloudMybatisAutoConfiguration;
import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import cn.zhicloud.module.bpm.api.task.BpmProcessInstanceApi;
import cn.zhicloud.module.bpm.api.task.BpmProcessInstanceApiImpl;
import cn.zhicloud.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import cn.zhicloud.module.bpm.dal.dataobject.task.BpmProcessInstanceIdempotencyDO;
import cn.zhicloud.module.bpm.dal.mysql.task.BpmProcessInstanceIdempotencyMapper;
import cn.zhicloud.module.bpm.dal.redis.BpmProcessIdRedisDAO;
import cn.zhicloud.module.bpm.enums.definition.BpmModelTypeEnum;
import cn.zhicloud.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateInvoker;
import cn.zhicloud.module.bpm.framework.flowable.core.event.BpmProcessInstanceEventPublisher;
import cn.zhicloud.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.zhicloud.module.bpm.service.message.BpmMessageService;
import cn.zhicloud.module.bpm.service.task.BpmProcessInstanceService;
import cn.zhicloud.module.bpm.service.task.BpmProcessInstanceServiceImpl;
import cn.zhicloud.module.bpm.service.task.BpmTaskService;
import cn.zhicloud.module.crm.dal.dataobject.receivable.CrmReceivableDO;
import cn.zhicloud.module.crm.dal.mysql.receivable.CrmReceivableMapper;
import cn.zhicloud.module.crm.dal.redis.no.CrmNoRedisDAO;
import cn.zhicloud.module.crm.enums.common.CrmAuditStatusEnum;
import cn.zhicloud.module.crm.service.contract.CrmContractService;
import cn.zhicloud.module.crm.service.permission.CrmPermissionService;
import cn.zhicloud.module.system.api.dept.DeptApi;
import cn.zhicloud.module.system.api.user.AdminUserApi;
import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.github.yulichang.autoconfigure.MybatisPlusJoinAutoConfiguration;
import jakarta.annotation.Resource;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CRM 回款提交、真实 BPM 幂等服务、真实 Flowable 和隔离 MySQL 的事务链验证。
 *
 * <p>该测试验证真实 {@link BpmProcessInstanceApiImpl} -> {@link BpmProcessInstanceServiceImpl}
 * -> 幂等 Mapper -> Flowable RuntimeService 的调用链，并在 BPM 返回后通过 Spy
 * 注入 CRM 外层事务故障。它仍是单实例隔离 MySQL 的非生产验证，不代表生产数据源路由、
 * 生产主从复制或生产规模行为。
 *
 * <p>通过环境变量显式启用：
 * ZHICLOUD_CRM_BPM_REAL_CHAIN_IT=true；
 * ZHICLOUD_CRM_BPM_IT_MYSQL_URL / USERNAME / PASSWORD 指向隔离测试库。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = CrmReceivableBpmTransactionMysqlIntegrationTest.TestApplication.class,
        properties = {
                "spring.datasource.url=${ZHICLOUD_CRM_BPM_IT_MYSQL_URL:jdbc:mysql://127.0.0.1:57507/zhicloud_crm_bpm_chain_test?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Singapore}",
                "spring.datasource.username=${ZHICLOUD_CRM_BPM_IT_MYSQL_USERNAME:root}",
                "spring.datasource.password=${ZHICLOUD_CRM_BPM_IT_MYSQL_PASSWORD:}",
                "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
                "spring.sql.init.mode=never",
                "spring.main.lazy-initialization=false",
                "mybatis-plus.configuration.map-underscore-to-camel-case=true",
                "zhicloud.info.base-package=cn.zhicloud",
                "flowable.database-schema-update=true",
                "flowable.db-history-used=true",
                "flowable.check-process-definitions=false",
                "flowable.history-level=audit",
                "flowable.app.enabled=false",
                "flowable.idm.enabled=false",
                "flowable.event-registry.enabled=false"
        })
@Sql(scripts = {
        "/sql/crm_bpm_transaction_schema_mysql.sql",
        "/sql/crm_bpm_transaction_clean_mysql.sql"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/crm_bpm_transaction_clean_mysql.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@EnabledIfEnvironmentVariable(named = "ZHICLOUD_CRM_BPM_REAL_CHAIN_IT", matches = "true",
        disabledReason = "需要隔离 MySQL 沙箱")
class CrmReceivableBpmTransactionMysqlIntegrationTest {

    private static final Long TENANT_ID = 11L;
    private static final Long USER_ID = 1101L;
    private static final String BPM_PROCESS_DEFINITION_KEY = CrmReceivableServiceImpl.BPM_PROCESS_DEFINITION_KEY;

    @Resource
    private CrmReceivableServiceImpl receivableService;
    @Resource
    private CrmReceivableMapper receivableMapper;
    @Resource
    private BpmProcessInstanceService processInstanceService;
    @Resource
    private BpmProcessInstanceIdempotencyMapper idempotencyMapper;
    @Resource
    private RepositoryService repositoryService;
    @Resource
    private RuntimeService runtimeService;
    @Resource
    private HistoryService historyService;

    @MockitoSpyBean
    private BpmProcessInstanceApi bpmProcessInstanceApi;

    @MockitoBean
    private CrmContractService contractService;
    @MockitoBean
    private CrmReceivablePlanService receivablePlanService;
    @MockitoBean
    private CrmPermissionService permissionService;
    @MockitoBean
    private CrmNoRedisDAO noRedisDAO;
    @MockitoBean
    private BpmProcessDefinitionService processDefinitionService;
    @MockitoBean
    private BpmTaskService taskService;
    @MockitoBean
    private BpmMessageService messageService;
    @MockitoBean
    private AdminUserApi adminUserApi;
    @MockitoBean
    private DeptApi deptApi;
    @MockitoBean
    private BpmProcessInstanceEventPublisher processInstanceEventPublisher;
    @MockitoBean
    private BpmTaskCandidateInvoker taskCandidateInvoker;
    @MockitoBean
    private BpmProcessIdRedisDAO processIdRedisDAO;
    @MockitoBean
    private EventOutboxService eventOutboxService;

    private final AtomicBoolean failAfterRealBpmCall = new AtomicBoolean(false);
    private final AtomicReference<String> startedProcessInstanceId = new AtomicReference<>();

    private String deploymentId;
    private ProcessDefinition definition;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        failAfterRealBpmCall.set(false);
        startedProcessInstanceId.set(null);

        assertTrue(AopUtils.isAopProxy(receivableService),
                "CrmReceivableService 必须经过事务代理，当前测试容器未启用事务管理");
        assertTrue(AopUtils.isAopProxy(processInstanceService),
                "BpmProcessInstanceService 必须经过事务代理，当前测试容器未启用事务管理");

        deploymentId = repositoryService.createDeployment()
                .name(BPM_PROCESS_DEFINITION_KEY + "-crm-chain-test")
                .addString(BPM_PROCESS_DEFINITION_KEY + ".bpmn20.xml", """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                     targetNamespace="zhicloud-crm-real-chain-test">
                          <process id="crm-receivable-audit" name="CRM receivable audit" isExecutable="true">
                            <startEvent id="start"/>
                            <endEvent id="end"/>
                            <sequenceFlow id="flow" sourceRef="start" targetRef="end"/>
                          </process>
                        </definitions>
                        """)
                .deploy()
                .getId();
        definition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deploymentId)
                .singleResult();
        assertNotNull(definition, "Flowable 流程定义必须部署成功");

        BpmProcessDefinitionInfoDO definitionInfo = new BpmProcessDefinitionInfoDO()
                .setProcessDefinitionId(definition.getId())
                .setModelType(BpmModelTypeEnum.BPMN.getType())
                .setVisible(true);
        when(processDefinitionService.getActiveProcessDefinition(BPM_PROCESS_DEFINITION_KEY))
                .thenReturn(definition);
        when(processDefinitionService.getProcessDefinition(definition.getId())).thenReturn(definition);
        when(processDefinitionService.getProcessDefinitionInfo(definition.getId())).thenReturn(definitionInfo);
        when(processDefinitionService.canUserStartProcessDefinition(definitionInfo, USER_ID)).thenReturn(true);
        when(processDefinitionService.getProcessDefinitionBpmnModel(definition.getId()))
                .thenAnswer(invocation -> repositoryService.getBpmnModel(definition.getId()));
        when(taskService.getTodoTask(anyLong(), any(), any())).thenReturn(null);
        when(taskCandidateInvoker.calculateUsersByActivity(any(), anyString(), anyLong(), anyString(), anyMap()))
                .thenReturn(Collections.emptySet());
        when(adminUserApi.getUserMap(any())).thenReturn(Collections.emptyMap());
        when(deptApi.getDeptMap(any())).thenReturn(Collections.emptyMap());

        doAnswer(invocation -> {
            String processInstanceId = (String) invocation.callRealMethod();
            if (failAfterRealBpmCall.get()) {
                startedProcessInstanceId.set(processInstanceId);
                throw new IllegalStateException("simulated rollback after real BPM process start");
            }
            return processInstanceId;
        }).when(bpmProcessInstanceApi).createProcessInstance(anyLong(), any());
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        if (deploymentId != null) {
            repositoryService.deleteDeployment(deploymentId, true);
        }
    }

    @Test
    void submitReceivableShouldRollbackCrmBpmRegistrationAndFlowableTogether() {
        CrmReceivableDO receivable = insertDraftReceivable();
        String businessKey = String.valueOf(receivable.getId());
        failAfterRealBpmCall.set(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> receivableService.submitReceivable(receivable.getId(), USER_ID));

        assertEquals("simulated rollback after real BPM process start", exception.getMessage());
        assertNotNull(startedProcessInstanceId.get(), "真实 BPM 调用必须已返回流程实例编号后再触发回滚");
        verify(bpmProcessInstanceApi).createProcessInstance(anyLong(), any());

        CrmReceivableDO rolledBack = receivableMapper.selectById(receivable.getId());
        assertNotNull(rolledBack, "CRM 回款行本身不应被删除");
        assertEquals(CrmAuditStatusEnum.DRAFT.getStatus(), rolledBack.getAuditStatus(),
                "CRM 状态必须随外层事务回滚到 DRAFT");
        assertNull(rolledBack.getProcessInstanceId(), "CRM 流程实例编号必须随事务回滚");
        assertEquals(0, rolledBack.getVersion(), "CRM 乐观锁版本必须随事务回滚");

        assertNull(idempotencyMapper.selectByUniqueKey(TENANT_ID, BPM_PROCESS_DEFINITION_KEY, businessKey),
                "BPM 幂等登记行必须随同一事务回滚");
        assertNull(runtimeService.createProcessInstanceQuery()
                        .processInstanceBusinessKey(businessKey)
                        .singleResult(),
                "Flowable 运行实例必须随同一事务回滚");
        assertEquals(0, historyService.createHistoricProcessInstanceQuery()
                        .processInstanceBusinessKey(businessKey)
                        .count(),
                "Flowable 历史实例必须随同一事务回滚");
    }

    @Test
    void submitReceivableShouldCommitCrmBpmRegistrationAndFlowableTogether() {
        CrmReceivableDO receivable = insertDraftReceivable();
        String businessKey = String.valueOf(receivable.getId());

        receivableService.submitReceivable(receivable.getId(), USER_ID);

        CrmReceivableDO committed = receivableMapper.selectById(receivable.getId());
        assertNotNull(committed);
        assertEquals(CrmAuditStatusEnum.PROCESS.getStatus(), committed.getAuditStatus());
        assertNotNull(committed.getProcessInstanceId(), "CRM 必须回填真实 Flowable 实例编号");
        assertEquals(2, committed.getVersion(), "CAS 抢占与流程编号回写必须各推进一次版本");

        BpmProcessInstanceIdempotencyDO registration = idempotencyMapper.selectByUniqueKey(
                TENANT_ID, BPM_PROCESS_DEFINITION_KEY, businessKey);
        assertNotNull(registration, "BPM 幂等登记行必须提交");
        assertEquals(committed.getProcessInstanceId(), registration.getProcessInstanceId(),
                "CRM 与 BPM 幂等登记必须指向同一流程实例");
        assertEquals(1, historyService.createHistoricProcessInstanceQuery()
                        .processInstanceBusinessKey(businessKey)
                        .count(),
                "Flowable 历史必须恰好有一条对应 businessKey 的实例");
        HistoricProcessInstance history = historyService.createHistoricProcessInstanceQuery()
                .processInstanceBusinessKey(businessKey)
                .singleResult();
        assertNotNull(history);
        assertEquals(committed.getProcessInstanceId(), history.getId(),
                "BPM 幂等登记必须与 Flowable 历史一致");
    }

    private CrmReceivableDO insertDraftReceivable() {
        CrmReceivableDO receivable = randomPojo(CrmReceivableDO.class, o -> {
            o.setNo("REC-TX-" + UUID.randomUUID());
            o.setAuditStatus(CrmAuditStatusEnum.DRAFT.getStatus());
            o.setVersion(0);
            o.setProcessInstanceId(null);
            o.setPrice(new BigDecimal("10.00"));
            o.setTenantId(TENANT_ID);
        });
        assertEquals(1, receivableMapper.insert(receivable));
        assertNotNull(receivable.getId(), "MySQL 自增主键必须回填");
        return receivable;
    }

    @Import({
            ZhiCloudDataSourceAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            DruidDataSourceAutoConfigure.class,
            ZhiCloudMybatisAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class,
            MybatisPlusJoinAutoConfiguration.class,
            SpringUtil.class,
            org.flowable.spring.boot.ProcessEngineAutoConfiguration.class,
            org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration.class,
            CrmReceivableServiceImpl.class,
            BpmProcessInstanceApiImpl.class,
            BpmProcessInstanceServiceImpl.class
    })
    static class TestApplication {
    }
}

