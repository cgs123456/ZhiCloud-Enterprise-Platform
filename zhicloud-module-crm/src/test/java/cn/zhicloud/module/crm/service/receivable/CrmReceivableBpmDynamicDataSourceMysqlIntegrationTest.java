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
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusLanguageDriverAutoConfiguration;
import com.github.yulichang.autoconfigure.MybatisPlusJoinAutoConfiguration;
import jakarta.annotation.Resource;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
 * CRM 回款提交、真实 BPM 幂等服务、真实 Flowable、动态数据源路由和隔离 MySQL 的事务链验证。
 *
 * <p>该测试验证：
 * <ul>
 *   <li>Spring 注入的主 {@link DataSource} 是 {@link DynamicRoutingDataSource}；</li>
 *   <li>路由中存在 master 和 slave，且分别连接独立测试库；</li>
 *   <li>Spring 事务管理器绑定动态数据源；</li>
 *   <li>CRM、BPM 幂等 Mapper、Flowable 在同一外层事务中提交或回滚；</li>
 *   <li>写事务使用 master 默认路由，slave 测试库不出现业务写入。</li>
 * </ul>
 *
 * <p>该测试仍是隔离 MySQL 的非生产验证，不代表生产主从复制、复制延迟、读写分离、
 * 网络故障、进程中断、补偿、对账或生产规模并发已经验证。连接身份通过 Spring 事务同步器
 * 绑定的物理连接 ID 验证；Flowable 通过同一动态数据源配置和提交/回滚原子性验证，
 * 不等同于网络抓包或 MySQL 全量审计日志级别的逐条 SQL 连接证明。
 *
 * <p>通过环境变量显式启用：
 * ZHICLOUD_CRM_BPM_DS_IT=true；
 * ZHICLOUD_CRM_BPM_DS_IT_MASTER_URL / SLAVE_URL / MYSQL_USERNAME / MYSQL_PASSWORD
 * 指向隔离测试库。
 */
@EnabledIfEnvironmentVariable(named = "ZHICLOUD_CRM_BPM_DS_IT", matches = "true",
        disabledReason = "需要隔离 MySQL 沙箱和 master/slave 测试库")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = CrmReceivableBpmDynamicDataSourceMysqlIntegrationTest.TestApplication.class,
        properties = {
                "spring.datasource.dynamic.enabled=true",
                "spring.datasource.dynamic.primary=master",
                "spring.datasource.dynamic.strict=true",
                "spring.datasource.dynamic.datasource.master.url=${ZHICLOUD_CRM_BPM_DS_IT_MASTER_URL:jdbc:mysql://127.0.0.1:57507/zhicloud_crm_bpm_ds_master_test?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Singapore}",
                "spring.datasource.dynamic.datasource.master.username=${ZHICLOUD_CRM_BPM_DS_IT_MYSQL_USERNAME:root}",
                "spring.datasource.dynamic.datasource.master.password=${ZHICLOUD_CRM_BPM_DS_IT_MYSQL_PASSWORD:}",
                "spring.datasource.dynamic.datasource.master.driver-class-name=com.mysql.cj.jdbc.Driver",
                "spring.datasource.dynamic.datasource.slave.url=${ZHICLOUD_CRM_BPM_DS_IT_SLAVE_URL:jdbc:mysql://127.0.0.1:57507/zhicloud_crm_bpm_ds_slave_test?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Singapore}",
                "spring.datasource.dynamic.datasource.slave.username=${ZHICLOUD_CRM_BPM_DS_IT_MYSQL_USERNAME:root}",
                "spring.datasource.dynamic.datasource.slave.password=${ZHICLOUD_CRM_BPM_DS_IT_MYSQL_PASSWORD:}",
                "spring.datasource.dynamic.datasource.slave.driver-class-name=com.mysql.cj.jdbc.Driver",
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
class CrmReceivableBpmDynamicDataSourceMysqlIntegrationTest {

    private static final Long TENANT_ID = 11L;
    private static final Long USER_ID = 1101L;
    private static final String BPM_PROCESS_DEFINITION_KEY = CrmReceivableServiceImpl.BPM_PROCESS_DEFINITION_KEY;
    private static final String MASTER_DATABASE = "zhicloud_crm_bpm_ds_master_test";
    private static final String SLAVE_DATABASE = "zhicloud_crm_bpm_ds_slave_test";

    @Resource
    private DataSource dataSource;
    @Resource(name = "transactionManager")
    private PlatformTransactionManager transactionManager;
    @Resource
    private ProcessEngine processEngine;
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
    private final AtomicReference<Long> connectionIdBeforeBpm = new AtomicReference<>();
    private final AtomicReference<Long> connectionIdAfterBpm = new AtomicReference<>();

    private String deploymentId;
    private ProcessDefinition definition;

    @BeforeEach
    void setUp() throws SQLException {
        TenantContextHolder.setTenantId(TENANT_ID);
        failAfterRealBpmCall.set(false);
        startedProcessInstanceId.set(null);
        connectionIdBeforeBpm.set(null);
        connectionIdAfterBpm.set(null);

        assertInstanceOf(DynamicRoutingDataSource.class, dataSource,
                "测试容器必须注入 DynamicRoutingDataSource，而不是退化为单数据源");
        initializeSlaveSchemaAndClean();

        assertTrue(AopUtils.isAopProxy(receivableService),
                "CrmReceivableService 必须经过事务代理，当前测试容器未启用事务管理");
        assertTrue(AopUtils.isAopProxy(processInstanceService),
                "BpmProcessInstanceService 必须经过事务代理，当前测试容器未启用事务管理");

        deploymentId = repositoryService.createDeployment()
                .name(BPM_PROCESS_DEFINITION_KEY + "-crm-dynamic-ds-test")
                .addString(BPM_PROCESS_DEFINITION_KEY + ".bpmn20.xml", """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                     targetNamespace="zhicloud-crm-dynamic-ds-test">
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
            connectionIdBeforeBpm.set(currentConnectionIdentity().connectionId());
            String processInstanceId = (String) invocation.callRealMethod();
            connectionIdAfterBpm.set(currentConnectionIdentity().connectionId());
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
    void dynamicRoutingShouldExposeMasterAndSlaveDatabases() throws SQLException {
        DynamicRoutingDataSource routing = routingDataSource();
        assertEquals(Set.of("master", "slave"), routing.getDataSources().keySet(),
                "测试容器必须只暴露 master 和 slave 两个显式路由");
        assertDatabase(routing.getDataSource("master"), MASTER_DATABASE);
        assertDatabase(routing.getDataSource("slave"), SLAVE_DATABASE);

        assertInstanceOf(DataSourceTransactionManager.class, transactionManager,
                "事务管理器必须是 DataSourceTransactionManager 或其子类");
        assertSame(dataSource, ((DataSourceTransactionManager) transactionManager).getDataSource(),
                "事务管理器必须绑定动态数据源本身，而不是绕过路由的单数据源");
        DataSource flowableDataSource = processEngine.getProcessEngineConfiguration().getDataSource();
        assertInstanceOf(TransactionAwareDataSourceProxy.class, flowableDataSource,
                "Flowable 必须通过事务感知代理访问动态数据源");
        assertSame(dataSource, ((TransactionAwareDataSourceProxy) flowableDataSource).getTargetDataSource(),
                "Flowable 事务感知代理必须指向同一个 DynamicRoutingDataSource");
    }

    @Test
    void submitReceivableShouldCommitCrmBpmAndFlowableOnMasterInOneTransaction() throws SQLException {
        CrmReceivableDO draft = insertDraftReceivable();
        String businessKey = String.valueOf(draft.getId());
        AtomicReference<Long> outerConnectionId = new AtomicReference<>();

        transactionTemplate().executeWithoutResult(status -> {
            long connectionId = currentConnectionIdentity().connectionId();
            outerConnectionId.set(connectionId);
            assertEquals(MASTER_DATABASE, currentConnectionIdentity().database(),
                    "外层事务默认必须路由到 master 测试库");

            receivableService.submitReceivable(draft.getId(), USER_ID);

            assertEquals(connectionId, currentConnectionIdentity().connectionId(),
                    "CRM 与 BPM 调用期间必须复用同一 Spring 事务绑定连接");
            assertEquals(connectionId, connectionIdBeforeBpm.get(),
                    "真实 BPM 调用开始前必须仍在外层事务绑定的物理连接上");
            assertEquals(connectionId, connectionIdAfterBpm.get(),
                    "真实 BPM 调用返回后必须仍在外层事务绑定的物理连接上");

            CrmReceivableDO inTransaction = receivableMapper.selectById(draft.getId());
            assertNotNull(inTransaction);
            assertEquals(CrmAuditStatusEnum.PROCESS.getStatus(), inTransaction.getAuditStatus());
            assertNotNull(inTransaction.getProcessInstanceId());
            BpmProcessInstanceIdempotencyDO registration = idempotencyMapper.selectByUniqueKey(
                    TENANT_ID, BPM_PROCESS_DEFINITION_KEY, businessKey);
            assertNotNull(registration, "事务内必须能看到 BPM 幂等登记");
            assertEquals(inTransaction.getProcessInstanceId(), registration.getProcessInstanceId());
            assertEquals(1, historyService.createHistoricProcessInstanceQuery()
                            .processInstanceBusinessKey(businessKey)
                            .count(),
                    "事务内必须能看到一条 Flowable 历史实例");
        });

        CrmReceivableDO committed = receivableMapper.selectById(draft.getId());
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

        assertEquals(0, countOnSlave("SELECT COUNT(*) FROM crm_receivable"),
                "写事务不得在 slave 测试库产生 CRM 行");
        assertEquals(0, countOnSlave("SELECT COUNT(*) FROM bpm_process_instance_idempotency"),
                "写事务不得在 slave 测试库产生 BPM 幂等登记行");
    }

    @Test
    void submitReceivableShouldRollbackCrmBpmAndFlowableOnMasterInOneTransaction() throws SQLException {
        CrmReceivableDO draft = insertDraftReceivable();
        String businessKey = String.valueOf(draft.getId());
        AtomicReference<Long> outerConnectionId = new AtomicReference<>();
        failAfterRealBpmCall.set(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> transactionTemplate().executeWithoutResult(status -> {
                    long connectionId = currentConnectionIdentity().connectionId();
                    outerConnectionId.set(connectionId);
                    assertEquals(MASTER_DATABASE, currentConnectionIdentity().database(),
                            "回滚事务默认必须路由到 master 测试库");
                    receivableService.submitReceivable(draft.getId(), USER_ID);
                }));

        assertEquals("simulated rollback after real BPM process start", exception.getMessage());
        assertNotNull(startedProcessInstanceId.get(), "真实 BPM 调用必须已返回流程实例编号后再触发回滚");
        assertEquals(outerConnectionId.get(), connectionIdBeforeBpm.get(),
                "回滚事务中真实 BPM 调用前必须复用外层绑定连接");
        assertEquals(outerConnectionId.get(), connectionIdAfterBpm.get(),
                "回滚事务中真实 BPM 调用返回后必须仍复用外层绑定连接");
        verify(bpmProcessInstanceApi).createProcessInstance(anyLong(), any());

        CrmReceivableDO rolledBack = receivableMapper.selectById(draft.getId());
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

        assertEquals(0, countOnSlave("SELECT COUNT(*) FROM crm_receivable"),
                "回滚后 slave 测试库不得出现 CRM 行");
        assertEquals(0, countOnSlave("SELECT COUNT(*) FROM bpm_process_instance_idempotency"),
                "回滚后 slave 测试库不得出现 BPM 幂等登记行");
    }

    private CrmReceivableDO insertDraftReceivable() {
        CrmReceivableDO receivable = randomPojo(CrmReceivableDO.class, o -> {
            o.setNo("REC-DS-TX-" + UUID.randomUUID());
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

    private TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(transactionManager);
    }

    private DynamicRoutingDataSource routingDataSource() {
        return (DynamicRoutingDataSource) dataSource;
    }

    private ConnectionIdentity currentConnectionIdentity() {
        Connection connection = null;
        try {
            connection = DataSourceUtils.getConnection(dataSource);
            return new ConnectionIdentity(queryConnectionId(connection), queryDatabase(connection));
        } catch (SQLException ex) {
            throw new IllegalStateException("读取当前 Spring 事务绑定连接身份失败", ex);
        } finally {
            if (connection != null) {
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
        }
    }

    private void initializeSlaveSchemaAndClean() throws SQLException {
        DataSource slave = routingDataSource().getDataSource("slave");
        try (Connection connection = slave.getConnection()) {
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("sql/crm_bpm_transaction_schema_mysql.sql"));
        }
        executeUpdate(slave, "DELETE FROM crm_receivable");
        executeUpdate(slave, "DELETE FROM bpm_process_instance_idempotency WHERE process_definition_key = ?",
                BPM_PROCESS_DEFINITION_KEY);
    }

    private long countOnSlave(String sql) throws SQLException {
        DataSource slave = routingDataSource().getDataSource("slave");
        try (Connection connection = slave.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertTrue(resultSet.next(), "COUNT 查询必须返回一行");
            return resultSet.getLong(1);
        }
    }

    private void assertDatabase(DataSource dataSource, String expectedDatabase) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT DATABASE()")) {
            assertTrue(resultSet.next(), "SELECT DATABASE() 必须返回一行");
            assertEquals(expectedDatabase, resultSet.getString(1));
        }
    }

    private static void executeUpdate(DataSource dataSource, String sql, Object... args) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                statement.setObject(i + 1, args[i]);
            }
            statement.executeUpdate();
        }
    }

    private static long queryConnectionId(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT CONNECTION_ID()")) {
            assertTrue(resultSet.next(), "SELECT CONNECTION_ID() 必须返回一行");
            return resultSet.getLong(1);
        }
    }

    private static String queryDatabase(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT DATABASE()")) {
            assertTrue(resultSet.next(), "SELECT DATABASE() 必须返回一行");
            return resultSet.getString(1);
        }
    }

    private record ConnectionIdentity(long connectionId, String database) {
    }

    @SpringBootConfiguration
    @ImportAutoConfiguration({
            DynamicDataSourceAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            ZhiCloudDataSourceAutoConfiguration.class,
            SqlInitializationAutoConfiguration.class,
            ZhiCloudMybatisAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class,
            MybatisPlusLanguageDriverAutoConfiguration.class,
            MybatisPlusJoinAutoConfiguration.class,
            org.flowable.spring.boot.ProcessEngineAutoConfiguration.class,
            org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration.class
    })
    @Import({SpringUtil.class, CrmReceivableServiceImpl.class, BpmProcessInstanceApiImpl.class,
            BpmProcessInstanceServiceImpl.class})
    static class TestApplication {
    }
}
