package cn.zhicloud.module.crm.service.receivable;

import cn.hutool.extra.spring.SpringUtil;
import cn.zhicloud.framework.datasource.config.ZhiCloudDataSourceAutoConfiguration;
import cn.zhicloud.framework.mybatis.config.ZhiCloudMybatisAutoConfiguration;
import cn.zhicloud.module.bpm.api.task.BpmProcessInstanceApi;
import cn.zhicloud.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.zhicloud.module.crm.dal.dataobject.receivable.CrmReceivableDO;
import cn.zhicloud.module.crm.dal.mysql.receivable.CrmReceivableMapper;
import cn.zhicloud.module.crm.dal.redis.no.CrmNoRedisDAO;
import cn.zhicloud.module.crm.enums.common.CrmAuditStatusEnum;
import cn.zhicloud.module.crm.service.contract.CrmContractService;
import cn.zhicloud.module.crm.service.permission.CrmPermissionService;
import cn.zhicloud.module.system.api.user.AdminUserApi;
import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.github.yulichang.autoconfigure.MybatisPlusJoinAutoConfiguration;
import jakarta.annotation.Resource;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = CrmReceivableBpmTransactionIntegrationTest.TestApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:crm-bpm-tx;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;NON_KEYWORDS=value",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
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
@Sql(scripts = "/sql/crm_bpm_transaction_schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/crm_bpm_transaction_clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class CrmReceivableBpmTransactionIntegrationTest {

    private static final String BPM_PROCESS_DEFINITION_KEY = "crm-receivable-audit";

    @Resource
    private CrmReceivableServiceImpl receivableService;
    @Resource
    private CrmReceivableMapper receivableMapper;
    @Resource
    private RepositoryService repositoryService;
    @Resource
    private HistoryService historyService;
    @Resource
    private TestBpmProcessInstanceApi bpmProcessInstanceApi;

    @MockitoBean
    private CrmContractService contractService;
    @MockitoBean
    private CrmReceivablePlanService receivablePlanService;
    @MockitoBean
    private CrmPermissionService permissionService;
    @MockitoBean
    private AdminUserApi adminUserApi;
    @MockitoBean
    private CrmNoRedisDAO noRedisDAO;

    private String deploymentId;

    @BeforeEach
    void deployProcess() {
        deploymentId = repositoryService.createDeployment()
                .addString(BPM_PROCESS_DEFINITION_KEY + ".bpmn20.xml", """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                     targetNamespace="zhicloud-crm-test">
                          <process id="crm-receivable-audit" name="CRM receivable audit" isExecutable="true">
                            <startEvent id="start"/>
                            <endEvent id="end"/>
                            <sequenceFlow id="flow" sourceRef="start" targetRef="end"/>
                          </process>
                        </definitions>
                        """)
                .deploy()
                .getId();
    }

    @AfterEach
    void deleteProcess() {
        if (deploymentId != null) {
            repositoryService.deleteDeployment(deploymentId, true);
        }
    }

    @Test
    void submitReceivableShouldRollbackCrmStateAndFlowableHistoryTogether() {
        CrmReceivableDO receivable = randomPojo(CrmReceivableDO.class, o -> {
            o.setNo("REC-TX-" + UUID.randomUUID());
            o.setAuditStatus(CrmAuditStatusEnum.DRAFT.getStatus());
            o.setVersion(0);
            o.setPrice(new BigDecimal("10.00"));
            o.setTenantId(1L);
        });
        receivableMapper.insert(receivable);
        String expectedBusinessKey = String.valueOf(receivable.getId());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> receivableService.submitReceivable(receivable.getId(), 1L));

        assertEquals("simulated rollback after BPM process start", exception.getMessage());
        assertEquals(1, bpmProcessInstanceApi.getStartedCount().get());
        assertEquals(CrmAuditStatusEnum.DRAFT.getStatus(), receivableMapper.selectById(receivable.getId()).getAuditStatus());
        assertNull(historyService.createHistoricProcessInstanceQuery()
                .processInstanceBusinessKey(expectedBusinessKey)
                .singleResult());
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
            TestBpmProcessInstanceConfiguration.class
    })
    static class TestApplication {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBpmProcessInstanceConfiguration {

        @Bean
        TestBpmProcessInstanceApi testBpmProcessInstanceApi(RuntimeService runtimeService) {
            return new TestBpmProcessInstanceApi(runtimeService);
        }
    }

    static class TestBpmProcessInstanceApi implements BpmProcessInstanceApi {

        private final RuntimeService runtimeService;
        private final AtomicInteger startedCount = new AtomicInteger();

        TestBpmProcessInstanceApi(RuntimeService runtimeService) {
            this.runtimeService = runtimeService;
        }

        @Override
        public String createProcessInstance(Long userId, BpmProcessInstanceCreateReqDTO reqDTO) {
            runtimeService.startProcessInstanceByKey(reqDTO.getProcessDefinitionKey(), reqDTO.getBusinessKey());
            startedCount.incrementAndGet();
            throw new IllegalStateException("simulated rollback after BPM process start");
        }

        AtomicInteger getStartedCount() {
            return startedCount;
        }
    }
}
