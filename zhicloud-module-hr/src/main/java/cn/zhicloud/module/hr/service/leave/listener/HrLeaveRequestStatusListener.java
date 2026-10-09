package cn.zhicloud.module.hr.service.leave.listener;

import cn.zhicloud.framework.common.util.number.NumberUtils;
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
        Long leaveId = NumberUtils.parseLong(event.getBusinessKey());
        if (leaveId == null) {
            log.error("[onEvent][businessKey({}) 不是有效的请假单编号，跳过状态更新]", event.getBusinessKey());
            return;
        }
        leaveRequestService.updateLeaveRequestBpmResult(leaveId,
                event.getStatus(), event.getId());
    }

}
