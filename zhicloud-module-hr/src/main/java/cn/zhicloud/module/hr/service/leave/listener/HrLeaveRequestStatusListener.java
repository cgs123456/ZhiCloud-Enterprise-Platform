package cn.zhicloud.module.hr.service.leave.listener;

import cn.zhicloud.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.zhicloud.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.zhicloud.module.hr.service.leave.HrLeaveRequestService;
import cn.zhicloud.module.hr.service.leave.HrLeaveRequestServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 请假审批结果监听器：BPM 流程实例状态回写请假单。
 *
 * @author 智云
 */
@Component
@Slf4j
public class HrLeaveRequestStatusListener extends BpmProcessInstanceStatusEventListener {

    @Resource
    private HrLeaveRequestService leaveRequestService;

    @Override
    public String getProcessDefinitionKey() {
        return HrLeaveRequestServiceImpl.PROCESS_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        leaveRequestService.updateLeaveRequestBpmResult(Long.parseLong(event.getBusinessKey()),
                event.getStatus(), event.getId());
    }

}
