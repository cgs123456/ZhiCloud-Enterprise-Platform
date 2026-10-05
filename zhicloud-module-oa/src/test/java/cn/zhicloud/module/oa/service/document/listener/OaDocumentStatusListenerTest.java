package cn.zhicloud.module.oa.service.document.listener;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.zhicloud.module.bpm.api.task.BpmProcessInstanceApi;
import cn.zhicloud.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.zhicloud.module.oa.dal.dataobject.document.OaDocumentDO;
import cn.zhicloud.module.oa.dal.mysql.document.OaDocumentMapper;
import cn.zhicloud.module.oa.service.document.OaDocumentServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link OaDocumentStatusListener} 的单元测试：BPM 审批结果能否真正回写到公文。
 *
 * <p>用真实 H2 + 真实 Mapper，只把 BPM 的发起接口 mock 掉：
 * 这个用例要证明的是「事件到库」这一段，而不是 BPM 内部。
 *
 * @author 智云
 */
@Import({OaDocumentServiceImpl.class, OaDocumentStatusListener.class})
public class OaDocumentStatusListenerTest extends BaseDbUnitTest {

    @Resource
    private OaDocumentStatusListener listener;
    @Resource
    private OaDocumentMapper documentMapper;

    @MockitoBean
    private BpmProcessInstanceApi processInstanceApi;

    private OaDocumentDO insertDocument(Long id, int status) {
        OaDocumentDO document = randomPojo(OaDocumentDO.class, o -> o.setId(id).setStatus(status));
        documentMapper.insert(document);
        return document;
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
    public void test_onEvent_approve_writesPendingSignStatus() {
        insertDocument(2001L, 20); // 20 审核中（submitDocument 后的状态）

        listener.onApplicationEvent(event(OaDocumentServiceImpl.PROCESS_KEY, "2001",
                BpmProcessInstanceStatusEnum.APPROVE.getStatus()));

        OaDocumentDO db = documentMapper.selectById(2001L);
        assertEquals(25, db.getStatus()); // 25 待签发，与手工核稿通过同档
        assertEquals("pi-2001", db.getProcessInstanceId());
    }

    @Test
    public void test_onEvent_reject_writesDraftStatus() {
        insertDocument(2002L, 20);

        listener.onApplicationEvent(event(OaDocumentServiceImpl.PROCESS_KEY, "2002",
                BpmProcessInstanceStatusEnum.REJECT.getStatus()));

        assertEquals(10, documentMapper.selectById(2002L).getStatus()); // 10 草稿，可修改后重新提交
    }

    @Test
    public void test_onEvent_cancel_writesDraftStatus() {
        insertDocument(2003L, 20);

        listener.onApplicationEvent(event(OaDocumentServiceImpl.PROCESS_KEY, "2003",
                BpmProcessInstanceStatusEnum.CANCEL.getStatus()));

        assertEquals(10, documentMapper.selectById(2003L).getStatus());
    }

    @Test
    public void test_onEvent_running_keepsReviewingStatus() {
        insertDocument(2004L, 20);

        listener.onApplicationEvent(event(OaDocumentServiceImpl.PROCESS_KEY, "2004",
                BpmProcessInstanceStatusEnum.RUNNING.getStatus()));

        // 中间态不能把公文状态倒写：只有终态才落库
        assertEquals(20, documentMapper.selectById(2004L).getStatus());
    }

    @Test
    public void test_onEvent_redelivery_doesNotRegressSignedDocument() {
        insertDocument(2005L, 30); // 已签发发布（30）：结果事件重复投递时不能倒退回待签发

        listener.onApplicationEvent(event(OaDocumentServiceImpl.PROCESS_KEY, "2005",
                BpmProcessInstanceStatusEnum.APPROVE.getStatus()));

        assertEquals(30, documentMapper.selectById(2005L).getStatus());
    }

    @Test
    public void test_onEvent_otherProcessKey_isIgnored() {
        insertDocument(2006L, 20);

        listener.onApplicationEvent(event("oa_reimburse", "2006",
                BpmProcessInstanceStatusEnum.APPROVE.getStatus()));

        // 别的流程定义的结果必须原样不动，否则一个监听器会把别人的单据改掉
        assertEquals(20, documentMapper.selectById(2006L).getStatus());
    }

}
