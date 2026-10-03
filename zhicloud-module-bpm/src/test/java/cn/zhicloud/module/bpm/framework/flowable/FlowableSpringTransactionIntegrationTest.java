package cn.zhicloud.module.bpm.framework.flowable;

import jakarta.annotation.Resource;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {
                DataSourceAutoConfiguration.class,
                DataSourceTransactionManagerAutoConfiguration.class,
                org.flowable.spring.boot.ProcessEngineAutoConfiguration.class,
                org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration.class
        },
        properties = {
                "spring.datasource.url=jdbc:h2:mem:flowable-spring-tx;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.sql.init.mode=never",
                "flowable.database-schema-update=true",
                "flowable.db-history-used=true",
                "flowable.check-process-definitions=false",
                "flowable.history-level=audit",
                "flowable.app.enabled=false"
        })
class FlowableSpringTransactionIntegrationTest {

    private static final String PROCESS_KEY = "flowable-tx-check";

    @Resource
    private RepositoryService repositoryService;
    @Resource
    private RuntimeService runtimeService;
    @Resource
    private HistoryService historyService;
    @Resource
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void deployProcess() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        repositoryService.createDeployment()
                .addString(PROCESS_KEY + ".bpmn20.xml", """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                     xmlns:flowable="http://flowable.org/bpmn"
                                     targetNamespace="zhicloud-test">
                          <process id="flowable-tx-check" name="Flowable transaction check" isExecutable="true">
                            <startEvent id="start"/>
                            <endEvent id="end"/>
                            <sequenceFlow id="flow" sourceRef="start" targetRef="end"/>
                          </process>
                        </definitions>
                        """)
                .deploy();
    }

    @AfterEach
    void cleanDeployment() {
        repositoryService.createDeploymentQuery()
                .processDefinitionKey(PROCESS_KEY)
                .list()
                .forEach(deployment -> repositoryService.deleteDeployment(deployment.getId(), true));
    }

    @Test
    void processStartShouldBeVisibleAfterCommit() {
        String processInstanceId = transactionTemplate.execute(status ->
                runtimeService.startProcessInstanceByKey(PROCESS_KEY, "committed-key").getId());

        HistoricProcessInstance history = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        assertNotNull(history);
    }

    @Test
    void processStartShouldRollBackWithLocalTransaction() {
        assertThrows(IllegalStateException.class, () -> transactionTemplate.execute(status -> {
            runtimeService.startProcessInstanceByKey(PROCESS_KEY, "rolled-back-key");
            throw new IllegalStateException("rollback process start");
        }));

        HistoricProcessInstance history = historyService.createHistoricProcessInstanceQuery()
                .processInstanceBusinessKey("rolled-back-key")
                .singleResult();
        assertNull(history);
    }
}
