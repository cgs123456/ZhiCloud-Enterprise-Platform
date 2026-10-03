package cn.zhicloud.module.bpm.service.message;

import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.framework.test.core.ut.BaseMockitoUnitTest;
import cn.zhicloud.framework.web.config.WebProperties;
import cn.zhicloud.module.bpm.enums.message.BpmMessageEnum;
import cn.zhicloud.module.bpm.service.message.dto.BpmMessageSendWhenProcessInstanceApproveReqDTO;
import cn.zhicloud.module.bpm.service.message.dto.BpmMessageSendWhenTaskCreatedReqDTO;
import cn.zhicloud.module.system.api.sms.SmsSendApi;
import cn.zhicloud.module.system.api.sms.dto.send.SmsSendSingleToUserReqDTO;
import cn.zhicloud.module.system.enums.ErrorCodeConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link BpmMessageServiceImpl} 的单元测试。
 */
public class BpmMessageServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private BpmMessageServiceImpl messageService;

    @Mock
    private SmsSendApi smsSendApi;

    @Mock
    private WebProperties webProperties;

    @BeforeEach
    public void beforeEach() {
        WebProperties.Ui ui = mock(WebProperties.Ui.class);
        doReturn("http://localhost/bpm").when(ui).getUrl();
        doReturn(ui).when(webProperties).getAdminUi();
    }

    /**
     * 收信人没有手机号（或模板/渠道未配置）时 system 侧抛业务异常，审批状态机不能被它带着回滚。
     */
    @Test
    public void testSendMessageWhenApprove_smsFailureIsSwallowed() {
        when(smsSendApi.sendSingleSmsToAdmin(any(SmsSendSingleToUserReqDTO.class)))
                .thenThrow(new ServiceException(ErrorCodeConstants.SMS_SEND_MOBILE_NOT_EXISTS));

        BpmMessageSendWhenProcessInstanceApproveReqDTO reqDTO =
                randomPojo(BpmMessageSendWhenProcessInstanceApproveReqDTO.class);
        assertDoesNotThrow(() -> messageService.sendMessageWhenProcessInstanceApprove(reqDTO));

        verify(smsSendApi).sendSingleSmsToAdmin(any(SmsSendSingleToUserReqDTO.class));
    }

    /**
     * 待办指派同样不能因为短信失败而回滚。
     */
    @Test
    public void testSendMessageWhenTaskAssigned_swallowsSmsFailure() {
        when(smsSendApi.sendSingleSmsToAdmin(any(SmsSendSingleToUserReqDTO.class)))
                .thenThrow(new ServiceException(ErrorCodeConstants.SMS_SEND_MOBILE_NOT_EXISTS));

        assertDoesNotThrow(() -> messageService.sendMessageWhenTaskAssigned(
                randomPojo(BpmMessageSendWhenTaskCreatedReqDTO.class)));
    }

    /**
     * 收口不能把正常通知一起吞掉：收件人与模板编码仍按原约定传给 system。
     */
    @Test
    public void testSendMessageWhenApprove_passesReceiverAndTemplateCode() {
        BpmMessageSendWhenProcessInstanceApproveReqDTO reqDTO =
                randomPojo(BpmMessageSendWhenProcessInstanceApproveReqDTO.class);

        messageService.sendMessageWhenProcessInstanceApprove(reqDTO);

        ArgumentCaptor<SmsSendSingleToUserReqDTO> captor = ArgumentCaptor.forClass(SmsSendSingleToUserReqDTO.class);
        verify(smsSendApi).sendSingleSmsToAdmin(captor.capture());
        assertEquals(reqDTO.getStartUserId(), captor.getValue().getUserId());
        assertEquals(BpmMessageEnum.PROCESS_INSTANCE_APPROVE.getSmsTemplateCode(), captor.getValue().getTemplateCode());
    }

}
