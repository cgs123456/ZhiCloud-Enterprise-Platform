package cn.zhicloud.module.bpm.service.oa.listener;

import cn.zhicloud.framework.common.util.number.NumberUtils;
import cn.zhicloud.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.zhicloud.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.zhicloud.module.bpm.service.oa.BpmOALeaveService;
import cn.zhicloud.module.bpm.service.oa.BpmOALeaveServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * OA 请假单的结果的监听器实现类
 *
 * @author 智云
 */
@Slf4j
@Component
public class BpmOALeaveStatusListener extends BpmProcessInstanceStatusEventListener {

    @Resource
    private BpmOALeaveService leaveService;

    @Override
    protected String getProcessDefinitionKey() {
        return BpmOALeaveServiceImpl.PROCESS_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        Long leaveId = NumberUtils.parseLong(event.getBusinessKey());
        if (leaveId == null) {
            log.error("[onEvent][businessKey({}) 不是有效的请假单编号，跳过状态更新]", event.getBusinessKey());
            return;
        }
        leaveService.updateLeaveStatus(leaveId, event.getStatus());
    }

}
