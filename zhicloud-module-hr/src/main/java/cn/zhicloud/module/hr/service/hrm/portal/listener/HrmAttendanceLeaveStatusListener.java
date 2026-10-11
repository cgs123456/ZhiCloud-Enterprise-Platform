package cn.zhicloud.module.hr.service.hrm.portal.listener;

import cn.zhicloud.framework.common.util.number.NumberUtils;
import cn.zhicloud.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.zhicloud.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.zhicloud.module.hr.service.hrm.portal.HrmPortalService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * HRM 请假审批结果监听器：BPM 流程实例状态回写 HRM 考勤请假单。
 *
 * 流程定义 KEY 为 hrm_leave_request（与旧 /hr 请假域区分）；businessKey 为 hr_attendance_leave.id。
 *
 * @author zhicloud
 */
@Component
@Slf4j
public class HrmAttendanceLeaveStatusListener extends BpmProcessInstanceStatusEventListener {

    @Resource
    private HrmPortalService portalService;

    @Override
    protected String getProcessDefinitionKey() {
        return "hrm_leave_request";
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        Long leaveId = NumberUtils.parseLong(event.getBusinessKey());
        if (leaveId == null) {
            log.error("[onEvent][businessKey({}) 不是有效的请假单编号，跳过状态更新]", event.getBusinessKey());
            return;
        }
        portalService.updateLeaveBpmResult(leaveId, event.getStatus(), event.getId());
    }

}
