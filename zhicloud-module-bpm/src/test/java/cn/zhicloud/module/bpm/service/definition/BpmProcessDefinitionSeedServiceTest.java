package cn.zhicloud.module.bpm.service.definition;

import cn.zhicloud.framework.common.enums.CommonStatusEnum;
import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.framework.tenant.core.util.TenantUtils;
import cn.zhicloud.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.zhicloud.module.bpm.api.task.BpmProcessInstanceApi;
import cn.zhicloud.module.bpm.api.task.BpmProcessInstanceApiImpl;
import cn.zhicloud.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.zhicloud.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import cn.zhicloud.module.bpm.dal.dataobject.task.BpmProcessInstanceIdempotencyDO;
import cn.zhicloud.module.bpm.dal.mysql.task.BpmProcessInstanceIdempotencyMapper;
import cn.zhicloud.module.bpm.dal.redis.BpmProcessIdRedisDAO;
import cn.zhicloud.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.zhicloud.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.zhicloud.module.bpm.framework.flowable.config.BpmFlowableConfiguration;
import cn.zhicloud.module.bpm.framework.flowable.core.candidate.strategy.user.BpmTaskCandidateStartUserStrategy;
import cn.zhicloud.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.zhicloud.module.bpm.framework.flowable.core.listener.BpmProcessInstanceEventListener;
import cn.zhicloud.module.bpm.service.message.BpmMessageService;
import cn.zhicloud.module.bpm.service.task.BpmProcessInstanceCopyService;
import cn.zhicloud.module.bpm.service.task.BpmProcessInstanceServiceImpl;
import cn.zhicloud.module.bpm.service.task.BpmTaskService;
import cn.zhicloud.module.system.api.dept.DeptApi;
import cn.zhicloud.module.system.api.user.AdminUserApi;
import cn.zhicloud.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * 流程定义播种（Seed）的真实链路测试：真实 Flowable 引擎（H2）+ 真实流程定义拓展表，
 * 验证「按租户播种 -> 按 KEY 解析到激活定义 -> 按 KEY 发起 -> 审批通过 -> 发布 APPROVE 状态事件」的闭环。
 * <p>
 * 注意：业务模块发起审批时的解析入口 getActiveProcessDefinition(key) 刻意不 Mock，否则证明不了任何东西；
 * 只有跨模块的 system API（用户、部门）与消息、任务等外围依赖才使用 Mock。
 *
 * @author 智云
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {BaseDbUnitTest.Application.class, BpmProcessDefinitionSeedServiceTest.TestApplication.class},
        properties = {
                // 不配置 MODE=MYSQL：MySQL 兼容模式下，H2 不接受 Flowable 建表脚本里的 identity 数据类型
                "spring.datasource.url=jdbc:h2:mem:bpm-process-definition-seed;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "spring.sql.init.schema-locations=classpath:/sql/bpm_process_definition_seed_schema.sql",
                "flowable.database-schema-update=true",
                "flowable.db-history-used=true",
                "flowable.check-process-definitions=false",
                "flowable.history-level=full",
                "flowable.app.enabled=false"
        })
@ActiveProfiles("unit-test")
@Sql(scripts = "/sql/bpm_process_definition_seed_clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class BpmProcessDefinitionSeedServiceTest {

    private static final Long TENANT_ID = 101L;
    private static final Long OTHER_TENANT_ID = 102L;
    private static final Long USER_ID = 201L;

    /**
     * OA 报销的流程定义 KEY，见 OaReimburseServiceImpl 的 PROCESS_KEY
     */
    private static final String OA_PROCESS_KEY = "oa_reimburse";
    /**
     * HRM 请假的流程定义 KEY，见 HrmPortalServiceImpl 的 LEAVE_PROCESS_KEY
     */
    private static final String HR_PROCESS_KEY = "hrm_leave_request";

    @Resource
    private BpmProcessDefinitionSeedService seedService;
    @Resource
    private BpmProcessDefinitionService processDefinitionService;
    @Resource
    private BpmProcessInstanceApi processInstanceApi;
    @Resource
    private RepositoryService repositoryService;
    @Resource
    private TaskService taskService;
    @Resource
    private HistoryService historyService;
    @Resource
    private StatusEventCollector statusEventCollector;

    @MockitoBean
    private AdminUserApi adminUserApi;
    @MockitoBean
    private DeptApi deptApi;
    @MockitoBean
    private BpmTaskService bpmTaskService;
    @MockitoBean
    private BpmMessageService bpmMessageService;
    @MockitoBean
    private BpmProcessInstanceCopyService bpmProcessInstanceCopyService;
    @MockitoBean
    private BpmFormService bpmFormService;
    @MockitoBean
    private BpmProcessIdRedisDAO bpmProcessIdRedisDAO;
    @MockitoBean
    private BpmProcessInstanceIdempotencyMapper processInstanceIdempotencyMapper;

    /**
     * 模拟「抢占幂等登记」的返回行，供后续当前读使用
     */
    private final AtomicReference<BpmProcessInstanceIdempotencyDO> claimed = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        statusEventCollector.getEvents().clear();
        // 候选人策略「发起人自己」会校验发起人账号是否可用，所以给一个启用状态的用户即可
        AdminUserRespDTO user = new AdminUserRespDTO().setId(USER_ID).setNickname("播种测试用户")
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
        when(adminUserApi.getUserMap(any())).thenReturn(Collections.singletonMap(USER_ID, user));
        // 幂等登记使用 MySQL 方言（INSERT ... ON DUPLICATE KEY UPDATE），且与「按 KEY 解析流程定义」无关，
        // 所以 Mock 成「无登记 -> 当前抢占成功」，让发起链路继续真实执行
        when(processInstanceIdempotencyMapper.selectByUniqueKey(any(), any(), any())).thenReturn(null);
        doAnswer(invocation -> {
            claimed.set(invocation.getArgument(0));
            return null;
        }).when(processInstanceIdempotencyMapper).insertOrLockUniqueKey(any());
        when(processInstanceIdempotencyMapper.selectByUniqueKeyForUpdate(any(), any(), any()))
                .thenAnswer(invocation -> claimed.get());
    }

    @AfterEach
    void tearDown() {
        // 删除部署会级联删除运行中 + 历史数据，避免用例之间互相影响
        repositoryService.createDeploymentQuery().list().forEach(
                deployment -> repositoryService.deleteDeployment(deployment.getId(), true));
    }

    @Test
    void seedOaReimburseThenApprovePublishesApproveEvent() {
        // 1. 播种：为该租户生成一个可发起的最小流程定义
        String definitionId = seedService.seed(OA_PROCESS_KEY, "员工报销审批", TENANT_ID);
        assertNotNull(definitionId);

        // 2. 真实解析：业务模块按 KEY 发起时走的查询，必须命中该租户的激活定义
        ProcessDefinition definition = TenantUtils.execute(TENANT_ID,
                () -> processDefinitionService.getActiveProcessDefinition(OA_PROCESS_KEY));
        assertNotNull(definition, "播种后应能按 租户 + KEY 解析到激活流程定义");
        assertEquals(definitionId, definition.getId());
        assertEquals(String.valueOf(TENANT_ID), definition.getTenantId(), "部署必须带租户，否则按租户查询不到");
        // 2.1 拓展表数据也必须写入，否则发起流程时同样会报「流程定义不存在」
        BpmProcessDefinitionInfoDO definitionInfo = processDefinitionService.getProcessDefinitionInfo(definitionId);
        assertNotNull(definitionInfo);
        assertEquals(definitionId, definitionInfo.getProcessDefinitionId());

        // 3. 幂等：重复播种不再产生第二份激活定义
        assertEquals(definitionId, seedService.seed(OA_PROCESS_KEY, "员工报销审批", TENANT_ID));
        assertEquals(1L, countActiveDefinitions(TENANT_ID, OA_PROCESS_KEY));
        // 3.1 其它租户不受影响，仍然解析不到
        assertNull(TenantUtils.execute(OTHER_TENANT_ID,
                () -> processDefinitionService.getActiveProcessDefinition(OA_PROCESS_KEY)));

        // 4. 按 KEY 发起流程，businessKey 为业务单编号，审批结果事件靠它回写
        String businessKey = "9527";
        String processInstanceId = TenantUtils.execute(TENANT_ID,
                () -> processInstanceApi.createProcessInstance(USER_ID, new BpmProcessInstanceCreateReqDTO()
                        .setProcessDefinitionKey(OA_PROCESS_KEY).setBusinessKey(businessKey)));
        assertNotNull(processInstanceId);

        // 4.1 任务分配给发起人本人（候选人策略：发起人自己），无需额外的角色 / 部门 / 用户组数据
        Task task = TenantUtils.execute(TENANT_ID, () -> taskService.createTaskQuery()
                .processInstanceId(processInstanceId).singleResult());
        assertNotNull(task, "流程发起后应产生一个待办任务");
        assertEquals(String.valueOf(USER_ID), task.getAssignee());

        // 5. 审批通过：与 BpmTaskServiceImpl#approveTask 的落库动作一致，先写任务状态再完成任务。
        //    流程走到结束事件后，由 BpmProcessInstanceEventListener 把 RUNNING 改成 APPROVE 并发布事件
        TenantUtils.execute(TENANT_ID, () -> {
            taskService.setVariableLocal(task.getId(), BpmnVariableConstants.TASK_VARIABLE_STATUS,
                    BpmTaskStatusEnum.APPROVE.getStatus());
            taskService.complete(task.getId());
        });

        // 6. 断言业务监听器（OaReimburseStatusListener、HrmAttendanceLeaveStatusListener）收到的事件内容
        List<BpmProcessInstanceStatusEvent> events = statusEventCollector.getEvents();
        assertEquals(1, events.size(), "流程结束后应发布一个状态事件");
        BpmProcessInstanceStatusEvent event = events.get(0);
        assertEquals(BpmProcessInstanceStatusEnum.APPROVE.getStatus(), event.getStatus());
        assertEquals(OA_PROCESS_KEY, event.getProcessDefinitionKey());
        assertEquals(processInstanceId, event.getId());
        assertEquals(businessKey, event.getBusinessKey());

        // 6.1 流程实例本身也结束，且状态变量为 APPROVE
        HistoricProcessInstance historicProcessInstance = TenantUtils.execute(TENANT_ID,
                () -> historyService.createHistoricProcessInstanceQuery()
                        .processInstanceId(processInstanceId).includeProcessVariables().singleResult());
        assertNotNull(historicProcessInstance);
        assertNotNull(historicProcessInstance.getEndTime());
        assertEquals(BpmProcessInstanceStatusEnum.APPROVE.getStatus(),
                historicProcessInstance.getProcessVariables().get(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_STATUS));
    }

    @Test
    void seedHrLeaveRequestForEveryTenantSeparately() {
        // 每个租户各自播种，互不共享流程定义
        String tenantOneDefinitionId = seedService.seed(HR_PROCESS_KEY, "员工请假申请", TENANT_ID);
        String tenantTwoDefinitionId = seedService.seed(HR_PROCESS_KEY, "员工请假申请", OTHER_TENANT_ID);

        assertEquals(1L, countActiveDefinitions(TENANT_ID, HR_PROCESS_KEY));
        assertEquals(1L, countActiveDefinitions(OTHER_TENANT_ID, HR_PROCESS_KEY));
        ProcessDefinition tenantOneDefinition = TenantUtils.execute(TENANT_ID,
                () -> processDefinitionService.getActiveProcessDefinition(HR_PROCESS_KEY));
        ProcessDefinition tenantTwoDefinition = TenantUtils.execute(OTHER_TENANT_ID,
                () -> processDefinitionService.getActiveProcessDefinition(HR_PROCESS_KEY));
        assertNotNull(tenantOneDefinition);
        assertNotNull(tenantTwoDefinition);
        assertEquals(tenantOneDefinitionId, tenantOneDefinition.getId());
        assertEquals(tenantTwoDefinitionId, tenantTwoDefinition.getId());
    }

    private long countActiveDefinitions(Long tenantId, String processDefinitionKey) {
        return TenantUtils.execute(tenantId, () -> repositoryService.createProcessDefinitionQuery()
                .processDefinitionTenantId(String.valueOf(tenantId))
                .processDefinitionKey(processDefinitionKey).active().count());
    }

    /**
     * 真实链路的 Bean：流程定义播种 + 模型创建部署 + Flowable 引擎配置 + 流程实例服务
     */
    @Import({BpmProcessDefinitionSeedServiceImpl.class, BpmProcessDefinitionServiceImpl.class,
            BpmModelServiceImpl.class, BpmProcessInstanceServiceImpl.class, BpmProcessInstanceApiImpl.class,
            BpmFlowableConfiguration.class, BpmProcessInstanceEventListener.class,
            BpmTaskCandidateStartUserStrategy.class, StatusEventCollector.class})
    @ImportAutoConfiguration({org.flowable.spring.boot.ProcessEngineAutoConfiguration.class,
            org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration.class})
    static class TestApplication {
    }

    /**
     * 收集 {@link BpmProcessInstanceStatusEvent}，用于断言业务监听器将收到什么
     */
    static class StatusEventCollector implements ApplicationListener<BpmProcessInstanceStatusEvent> {

        private final List<BpmProcessInstanceStatusEvent> events = Collections.synchronizedList(new ArrayList<>());

        public List<BpmProcessInstanceStatusEvent> getEvents() {
            return events;
        }

        @Override
        public void onApplicationEvent(BpmProcessInstanceStatusEvent event) {
            events.add(event);
        }

    }

}
