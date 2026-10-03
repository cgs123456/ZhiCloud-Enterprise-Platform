package cn.zhicloud.module.bpm.service.task;

import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.module.bpm.enums.ErrorCodeConstants;
import cn.zhicloud.framework.datasource.config.ZhiCloudDataSourceAutoConfiguration;
import cn.zhicloud.framework.mybatis.core.handler.DefaultDBFieldHandler;
import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import cn.zhicloud.module.bpm.api.task.BpmProcessInstanceApi;
import cn.zhicloud.module.bpm.api.task.BpmProcessInstanceApiImpl;
import cn.zhicloud.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.zhicloud.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import cn.zhicloud.module.bpm.dal.dataobject.task.BpmProcessInstanceIdempotencyDO;
import cn.zhicloud.module.bpm.dal.mysql.task.BpmProcessInstanceIdempotencyMapper;
import cn.zhicloud.module.bpm.dal.redis.BpmProcessIdRedisDAO;
import cn.zhicloud.module.bpm.enums.definition.BpmModelTypeEnum;
import cn.zhicloud.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateInvoker;
import cn.zhicloud.module.bpm.framework.flowable.core.event.BpmProcessInstanceEventPublisher;
import cn.zhicloud.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.zhicloud.module.bpm.service.message.BpmMessageService;
import cn.zhicloud.module.system.api.dept.DeptApi;
import cn.zhicloud.module.system.api.user.AdminUserApi;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusLanguageDriverAutoConfiguration;
import jakarta.annotation.Resource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 真实 Spring Bean、真实 MyBatis Mapper、真实 Flowable 和真实 MySQL 的并发幂等链验证。
 *
 * <p>该测试通过环境变量显式启用，避免普通单元测试在没有隔离 MySQL 沙箱时失败:
 * ZHICLOUD_BPM_REAL_CHAIN_IT=true，并使用
 * ZHICLOUD_BPM_IT_MYSQL_URL / USERNAME / PASSWORD 指向测试库。
 */
@EnabledIfEnvironmentVariable(named = "ZHICLOUD_BPM_REAL_CHAIN_IT", matches = "true")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = BpmProcessInstanceIdempotencyRealChainConcurrencyTest.TestApplication.class,
        properties = {
                "spring.datasource.url=${ZHICLOUD_BPM_IT_MYSQL_URL:jdbc:mysql://127.0.0.1:57507/zhicloud_bpm_chain_test?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Singapore}",
                "spring.datasource.username=${ZHICLOUD_BPM_IT_MYSQL_USERNAME:root}",
                "spring.datasource.password=${ZHICLOUD_BPM_IT_MYSQL_PASSWORD:}",
                "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
                "flowable.database-schema-update=true",
                "flowable.db-history-used=true",
                "flowable.check-process-definitions=false",
                "flowable.history-level=audit",
                "flowable.app.enabled=false",
                "mybatis-plus.configuration.map-underscore-to-camel-case=true"
        })
@Sql(scripts = "/sql/bpm_process_instance_idempotency_mysql.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class BpmProcessInstanceIdempotencyRealChainConcurrencyTest {

    private static final Long TENANT_ID = 7L;
    private static final Long USER_ID = 1001L;
    private static final String PROCESS_KEY = "bpm-idempotency-real-chain";
    private static final int CONCURRENCY = 10;

    @Resource
    private BpmProcessInstanceApi processInstanceApi;
    @Resource
    private BpmProcessInstanceService processInstanceService;
    @Resource
    private BpmProcessInstanceIdempotencyMapper idempotencyMapper;
    @Resource
    private RepositoryService repositoryService;
    @Resource
    private HistoryService historyService;
    @Resource
    private RuntimeService runtimeService;
    @Resource
    private DataSource dataSource;

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

    private String businessKey;
    private ProcessDefinition definition;
    private final CountDownLatch winnerInsideDefinitionLookup = new CountDownLatch(1);
    private final CountDownLatch releaseWinner = new CountDownLatch(1);
    private final AtomicInteger activeDefinitionLookups = new AtomicInteger();

    @BeforeEach
    void setUp() {
        assertTrue(AopUtils.isAopProxy(processInstanceService), "BpmProcessInstanceService 必须经过事务代理，当前测试容器未启用事务管理");
        cleanupBusinessRows();
        businessKey = "bpm-real-chain-" + UUID.randomUUID();
        definition = deployProcess();
        BpmProcessDefinitionInfoDO definitionInfo = new BpmProcessDefinitionInfoDO()
                .setProcessDefinitionId(definition.getId())
                .setModelType(BpmModelTypeEnum.BPMN.getType())
                .setVisible(true);

        when(processDefinitionService.getActiveProcessDefinition(PROCESS_KEY)).thenAnswer(invocation -> {
            if (activeDefinitionLookups.incrementAndGet() == 1) {
                winnerInsideDefinitionLookup.countDown();
                assertTrue(releaseWinner.await(20, TimeUnit.SECONDS), "等待释放并发屏障超时");
            }
            return definition;
        });
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
    }

    @AfterEach
    void tearDown() {
        releaseWinner.countDown();
        if (definition != null) {
            repositoryService.deleteDeployment(definition.getDeploymentId(), true);
        }
        cleanupBusinessRows();
    }

    @Test
    void concurrentCallsThroughRealSpringAndFlowableReturnOneInstance() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENCY);
        CountDownLatch ready = new CountDownLatch(CONCURRENCY);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<CallResult>> futures = new ArrayList<>(CONCURRENCY);
        try {
            for (int i = 0; i < CONCURRENCY; i++) {
                final int index = i;
                Callable<CallResult> task = () -> {
                    TenantContextHolder.setTenantId(TENANT_ID);
                    ready.countDown();
                    try {
                        if (!start.await(10, TimeUnit.SECONDS)) {
                            return CallResult.failure(index, new IllegalStateException("等待并发开始超时"));
                        }
                        BpmProcessInstanceCreateReqDTO reqDTO = new BpmProcessInstanceCreateReqDTO()
                                .setProcessDefinitionKey(PROCESS_KEY)
                                .setBusinessKey(businessKey);
                        return CallResult.success(index, processInstanceApi.createProcessInstance(USER_ID, reqDTO));
                    } catch (Throwable ex) {
                        return CallResult.failure(index, ex);
                    } finally {
                        TenantContextHolder.clear();
                    }
                };
                futures.add(executor.submit(task));
            }

            assertTrue(ready.await(10, TimeUnit.SECONDS), "并发线程未全部就绪");
            start.countDown();
            if (!winnerInsideDefinitionLookup.await(10, TimeUnit.SECONDS)) {
                releaseWinner.countDown();
                List<CallResult> earlyResults = new ArrayList<>(CONCURRENCY);
                for (int i = 0; i < futures.size(); i++) {
                    try {
                        earlyResults.add(futures.get(i).get(5, TimeUnit.SECONDS));
                    } catch (Exception ex) {
                        earlyResults.add(CallResult.failure(i, ex));
                    }
                }
                assertTrue(false, () -> "没有线程进入真实流程定义查询阶段: " + render(earlyResults));
            }
            // winner 持有幂等唯一键但尚未提交。给其他线程足够时间完成首次一致性读并阻塞在唯一键插入上。
            Thread.sleep(3000);
            releaseWinner.countDown();

            List<CallResult> results = new ArrayList<>(CONCURRENCY);
            for (Future<CallResult> future : futures) {
                results.add(future.get(20, TimeUnit.SECONDS));
            }

            List<CallResult> failures = results.stream().filter(result -> result.error != null).toList();
            Set<String> instanceIds = results.stream()
                    .map(CallResult::instanceId)
                    .filter(Objects::nonNull)
                    .collect(java.util.stream.Collectors.toSet());

            BpmProcessInstanceIdempotencyDO persisted = idempotencyMapper.selectByUniqueKey(
                    TENANT_ID, PROCESS_KEY, businessKey);
            List<HistoricProcessInstance> histories = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceBusinessKey(businessKey)
                    .list();

            assertTrue(failures.isEmpty(), () -> "真实并发调用存在失败: " + render(results));
            assertEquals(1, instanceIds.size(), () -> "并发调用必须返回同一流程实例: " + render(results));
            assertNotNull(persisted, "幂等登记行必须存在且回填实例编号");
            assertEquals(instanceIds.iterator().next(), persisted.getProcessInstanceId(),
                    "幂等登记行必须指向唯一流程实例");
            assertEquals(1, histories.size(), "Flowable 历史必须只有一条对应 businessKey 的流程实例");
            assertEquals(persisted.getProcessInstanceId(), histories.get(0).getId(),
                    "幂等登记行必须与 Flowable 历史一致");
        } finally {
            releaseWinner.countDown();
            start.countDown();
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    @Test
    void committedBlankRegistrationFailsClosedWithoutCreatingProcessInstance() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            insertBlankRegistration(connection, businessKey);
            connection.commit();
        }

        TenantContextHolder.setTenantId(TENANT_ID);
        ServiceException exception;
        try {
            exception = assertThrows(ServiceException.class,
                    () -> processInstanceApi.createProcessInstance(USER_ID, createRequest(businessKey)));
        } finally {
            TenantContextHolder.clear();
        }

        assertEquals(ErrorCodeConstants.PROCESS_INSTANCE_IDEMPOTENCY_PENDING.getCode(), exception.getCode());
        BpmProcessInstanceIdempotencyDO persisted = idempotencyMapper.selectByUniqueKey(
                TENANT_ID, PROCESS_KEY, businessKey);
        assertNotNull(persisted, "预置的空白登记必须仍存在");
        assertNull(persisted.getProcessInstanceId(), "失败关闭不得回填实例编号");
        assertEquals(0, countHistories(businessKey), "空白登记已存在时不得创建 Flowable 实例");
    }

    @Test
    void blankRegistrationCommittedAfterFastPathFailsClosed() throws Exception {
        // 本用例自行制造“快路径看不到、随后才提交”的登记；关闭现有并发用例的 winner 屏障。
        releaseWinner.countDown();
        try (Connection claimant = dataSource.getConnection()) {
            claimant.setAutoCommit(false);
            insertBlankRegistration(claimant, businessKey);

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<CallResult> resultFuture = executor.submit(() -> {
                TenantContextHolder.setTenantId(TENANT_ID);
                try {
                    return CallResult.success(0,
                            processInstanceApi.createProcessInstance(USER_ID, createRequest(businessKey)));
                } catch (Throwable ex) {
                    return CallResult.failure(0, ex);
                } finally {
                    TenantContextHolder.clear();
                }
            });
            try {
                assertTrue(waitForIdempotencyUniqueKeyLockWait(10_000),
                        "调用方未在幂等唯一键上形成可观测锁等待，无法证明两事务交错路径");
                claimant.commit();

                CallResult result = resultFuture.get(20, TimeUnit.SECONDS);
                assertNotNull(result.error(),
                        () -> "空白登记在快路径之后提交，调用方必须失败关闭: " + result.description());
                assertTrue(result.error() instanceof ServiceException,
                        () -> "应抛出 ServiceException，实际: " + result.description());
                assertEquals(ErrorCodeConstants.PROCESS_INSTANCE_IDEMPOTENCY_PENDING.getCode(),
                        ((ServiceException) result.error()).getCode(),
                        () -> "应返回 PENDING，实际: " + result.description());
                assertEquals(0, countHistories(businessKey),
                        "空白登记由另一事务提交后不得创建 Flowable 实例");

                BpmProcessInstanceIdempotencyDO persisted = idempotencyMapper.selectByUniqueKey(
                        TENANT_ID, PROCESS_KEY, businessKey);
                assertNotNull(persisted, "空白登记必须仍存在");
                assertNull(persisted.getProcessInstanceId(), "失败关闭不得回填实例编号");
            } finally {
                claimant.rollback();
                executor.shutdownNow();
                executor.awaitTermination(10, TimeUnit.SECONDS);
            }
        }
    }

    private BpmProcessInstanceCreateReqDTO createRequest(String businessKey) {
        return new BpmProcessInstanceCreateReqDTO()
                .setProcessDefinitionKey(PROCESS_KEY)
                .setBusinessKey(businessKey);
    }

    private void insertBlankRegistration(Connection connection, String businessKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO bpm_process_instance_idempotency
                    (tenant_id, process_definition_key, business_key, process_instance_id)
                VALUES (?, ?, ?, NULL)
                """)) {
            statement.setLong(1, TENANT_ID);
            statement.setString(2, PROCESS_KEY);
            statement.setString(3, businessKey);
            assertEquals(1, statement.executeUpdate(), "预置空白登记必须成功");
        }
    }

    private boolean waitForIdempotencyUniqueKeyLockWait(long timeoutMillis) throws SQLException, InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        while (System.nanoTime() < deadline) {
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(
                         "SELECT COUNT(*) FROM performance_schema.data_lock_waits waits " +
                         "JOIN performance_schema.data_locks locks " +
                         "ON locks.ENGINE_LOCK_ID = waits.REQUESTING_ENGINE_LOCK_ID " +
                         "WHERE locks.OBJECT_SCHEMA = DATABASE() " +
                         "AND locks.OBJECT_NAME = 'bpm_process_instance_idempotency' " +
                         "AND locks.LOCK_STATUS = 'WAITING'")) {
                if (resultSet.next() && resultSet.getInt(1) > 0) {
                    return true;
                }
            }
            Thread.sleep(50);
        }
        return false;
    }

    private long countHistories(String businessKey) {
        return historyService.createHistoricProcessInstanceQuery()
                .processInstanceBusinessKey(businessKey)
                .count();
    }
    private ProcessDefinition deployProcess() {
        String bpmn = """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:flowable="http://flowable.org/bpmn"
                             targetNamespace="zhicloud-test">
                  <process id="%s" name="BPM idempotency real chain" isExecutable="true">
                    <startEvent id="start"/>
                    <endEvent id="end"/>
                    <sequenceFlow id="flow" sourceRef="start" targetRef="end"/>
                  </process>
                </definitions>
                """.formatted(PROCESS_KEY);
        String deploymentId = repositoryService.createDeployment()
                .name(PROCESS_KEY + "-deployment")
                .addString(PROCESS_KEY + ".bpmn20.xml", bpmn)
                .deploy()
                .getId();
        return repositoryService.createProcessDefinitionQuery()
                .deploymentId(deploymentId)
                .singleResult();
    }

    private void cleanupBusinessRows() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM bpm_process_instance_idempotency WHERE process_definition_key = ?")) {
            statement.setString(1, PROCESS_KEY);
            statement.executeUpdate();
        } catch (SQLException ex) {
            // 第一次启动时表可能尚不存在；@Sql 会随后创建。避免掩盖测试主体的真实失败。
            if (!isMissingTable(ex)) {
                throw new IllegalStateException("清理幂等测试数据失败", ex);
            }
        }
    }

    private static boolean isMissingTable(SQLException ex) {
        return ex.getErrorCode() == 1146 || (ex.getMessage() != null && ex.getMessage().contains("doesn't exist"));
    }

    private String render(List<CallResult> results) {
        return results.stream().map(CallResult::description).toList().toString();
    }

    private record CallResult(int index, String instanceId, Throwable error) {

        static CallResult success(int index, String instanceId) {
            return new CallResult(index, instanceId, null);
        }

        static CallResult failure(int index, Throwable error) {
            return new CallResult(index, null, error);
        }

        String description() {
            if (error == null) {
                return "#" + index + "=success(" + instanceId + ")";
            }
            return "#" + index + "=" + error.getClass().getSimpleName() + "(" + error.getMessage() + ")";
        }
    }

    @SpringBootConfiguration
    @ImportAutoConfiguration({
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            ZhiCloudDataSourceAutoConfiguration.class,
            SqlInitializationAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class,
            MybatisPlusLanguageDriverAutoConfiguration.class,
            org.flowable.spring.boot.ProcessEngineAutoConfiguration.class,
            org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration.class
    })
    @Import({BpmProcessInstanceApiImpl.class, BpmProcessInstanceServiceImpl.class})
    static class TestApplication {

        @Bean
        DefaultDBFieldHandler defaultDBFieldHandler() {
            return new DefaultDBFieldHandler();
        }

        @Bean
        MapperFactoryBean<BpmProcessInstanceIdempotencyMapper> bpmProcessInstanceIdempotencyMapper(
                SqlSessionFactory sqlSessionFactory) {
            MapperFactoryBean<BpmProcessInstanceIdempotencyMapper> factory =
                    new MapperFactoryBean<>(BpmProcessInstanceIdempotencyMapper.class);
            factory.setSqlSessionFactory(sqlSessionFactory);
            return factory;
        }
    }
}
