package cn.zhicloud.module.oa.service.reimburse.listener;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.zhicloud.module.bpm.api.task.BpmProcessInstanceApi;
import cn.zhicloud.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.zhicloud.module.oa.dal.dataobject.reimburse.OaReimburseDO;
import cn.zhicloud.module.oa.dal.mysql.reimburse.OaReimburseMapper;
import cn.zhicloud.module.oa.service.reimburse.OaReimburseServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link OaReimburseStatusListener} 的单元测试：BPM 审批结果能否真正回写到报销单。
 *
 * <p>用真实 H2 + 真实 Mapper，只把 BPM 的发起接口 mock 掉：
 * 这个用例要证明的是「事件到库」这一段，而不是 BPM 内部。
 *
 * @author 智云
 */
@Import({OaReimburseServiceImpl.class, OaReimburseStatusListener.class})
public class OaReimburseStatusListenerTest extends BaseDbUnitTest {

    @Resource
    private OaReimburseStatusListener listener;
    @Resource
    private OaReimburseMapper reimburseMapper;

    @MockitoBean
    private BpmProcessInstanceApi processInstanceApi;

    private OaReimburseDO insertReimburse(Long id) {
        OaReimburseDO reimburse = randomPojo(OaReimburseDO.class, o -> o.setId(id)
                .setStatus(20)); // 20 审批中（submitReimburse 后的状态）
        reimburseMapper.insert(reimburse);
        return reimburse;
    }

    private BpmProcessInstanceStatusEvent event(String processDefinitionKey, String businessKey, Integer status) {
        BpmProcessInstanceStatusEvent event = new BpmProcessInstanceStatusEvent(this);
        event.setId("pi-" + businessKey);
        event.setProcessDefinitionKey(processDefinitionKey);
        event.setBusinessKey(businessKey);
        event.setStatus(status);
        return event;
    }

    @Test
    public void test_onEvent_approve_writesApprovedStatus() {
        insertReimburse(1001L);

        listener.onApplicationEvent(event(OaReimburseServiceImpl.PROCESS_KEY, "1001",
                BpmProcessInstanceStatusEnum.APPROVE.getStatus()));

        OaReimburseDO db = reimburseMapper.selectById(1001L);
        assertEquals(30, db.getStatus()); // 30 已通过
        assertEquals("pi-1001", db.getProcessInstanceId());
    }

    @Test
    public void test_onEvent_reject_writesRejectedStatus() {
        insertReimburse(1002L);

        listener.onApplicationEvent(event(OaReimburseServiceImpl.PROCESS_KEY, "1002",
                BpmProcessInstanceStatusEnum.REJECT.getStatus()));

        assertEquals(40, reimburseMapper.selectById(1002L).getStatus()); // 40 已驳回
    }

    @Test
    public void test_onEvent_cancel_writesRevokedStatus() {
        insertReimburse(1003L);

        listener.onApplicationEvent(event(OaReimburseServiceImpl.PROCESS_KEY, "1003",
                BpmProcessInstanceStatusEnum.CANCEL.getStatus()));

        assertEquals(50, reimburseMapper.selectById(1003L).getStatus()); // 50 已撤销
    }

    @Test
    public void test_onEvent_running_keepsApprovingStatus() {
        insertReimburse(1004L);

        listener.onApplicationEvent(event(OaReimburseServiceImpl.PROCESS_KEY, "1004",
                BpmProcessInstanceStatusEnum.RUNNING.getStatus()));

        // 中间态不能把单据状态倒写：只有终态才落库
        assertEquals(20, reimburseMapper.selectById(1004L).getStatus());
    }

    @Test
    public void test_onEvent_otherProcessKey_isIgnored() {
        insertReimburse(1005L);

        listener.onApplicationEvent(event("hr_leave_request", "1005",
                BpmProcessInstanceStatusEnum.APPROVE.getStatus()));

        // 别的流程定义的结果必须原样不动，否则一个监听器会把别人的单据改掉
        assertEquals(20, reimburseMapper.selectById(1005L).getStatus());
    }

}
