package cn.zhicloud.module.bpm.api.task;

import cn.zhicloud.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.zhicloud.module.bpm.controller.admin.task.vo.instance.BpmProcessInstanceCancelReqVO;
import cn.zhicloud.module.bpm.service.task.BpmProcessInstanceService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * Flowable 流程实例 Api 实现类
 *
 * @author 智云
 * @author jason
 */
@Service
@Validated
public class BpmProcessInstanceApiImpl implements BpmProcessInstanceApi {

    @Resource
    private BpmProcessInstanceService processInstanceService;

    @Override
    public String createProcessInstance(Long userId, @Valid BpmProcessInstanceCreateReqDTO reqDTO) {
        return processInstanceService.createProcessInstance(userId, reqDTO);
    }

    @Override
    public void cancelProcessInstance(Long userId, String processInstanceId, String reason) {
        BpmProcessInstanceCancelReqVO cancelReqVO = new BpmProcessInstanceCancelReqVO();
        cancelReqVO.setId(processInstanceId);
        cancelReqVO.setReason(reason);
        processInstanceService.cancelProcessInstanceByStartUser(userId, cancelReqVO);
    }

}
