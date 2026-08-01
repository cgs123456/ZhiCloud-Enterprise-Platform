package cn.iocoder.yudao.module.mes.controller.app.pro;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.MesProFeedbackRespVO;
import cn.iocoder.yudao.module.mes.controller.app.pro.vo.MesPdaFeedbackSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.app.pro.vo.MesPdaMachineryRespVO;
import cn.iocoder.yudao.module.mes.controller.app.pro.vo.MesPdaScanMachineryReqVO;
import cn.iocoder.yudao.module.mes.controller.app.pro.vo.MesPdaScanWorkOrderReqVO;
import cn.iocoder.yudao.module.mes.controller.app.pro.vo.MesPdaWorkOrderRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.task.MesProTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteService;
import cn.iocoder.yudao.module.mes.service.pro.task.MesProTaskService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PDA_FEEDBACK_MACHINERY_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PDA_FEEDBACK_WORK_ORDER_NOT_EXISTS;

/**
 * MES PDA / 工位机报工接口
 *
 * <p>面向 PDA 工位机的简化报工流程，跳过审批直接完成；接口加 {@code @PermitAll}，
 * 由 App 端 token 认证。
 *
 * @author 芋道源码
 */
@Tag(name = "用户 APP - MES PDA 报工")
@RestController
@RequestMapping("/mes-api/pro/feedback")
@Validated
public class MesProPdaFeedbackController {

    @Resource
    private MesProFeedbackService feedbackService;
    @Resource
    private MesProWorkOrderService workOrderService;
    @Resource
    private MesProTaskService taskService;
    @Resource
    private MesProRouteProcessService routeProcessService;
    @Resource
    private MesProRouteService routeService;
    @Resource
    private MesMdItemService itemService;
    @Resource
    private MesDvMachineryMapper machineryMapper;

    @PostMapping("/scan-work-order")
    @Operation(summary = "扫描工单二维码，返回工单信息+当前工序列表")
    @PermitAll
    public CommonResult<MesPdaWorkOrderRespVO> scanWorkOrder(@Valid @RequestBody MesPdaScanWorkOrderReqVO reqVO) {
        // 1. 查询工单
        MesProWorkOrderDO workOrder = workOrderService.getWorkOrder(reqVO.getWorkOrderNo());
        if (workOrder == null) {
            throw exception(PRO_PDA_FEEDBACK_WORK_ORDER_NOT_EXISTS, reqVO.getWorkOrderNo());
        }
        // 2. 查询工单关联任务，定位工艺路线
        List<MesProTaskDO> tasks = taskService.getTaskListByWorkOrderIds(Collections.singletonList(workOrder.getId()));
        Long routeId = tasks.stream().map(MesProTaskDO::getRouteId).findFirst().orElse(null);
        // 3. 查询工艺路线的工序列表
        List<MesProRouteProcessDO> routeProcesses = routeId == null
                ? Collections.emptyList()
                : routeProcessService.getRouteProcessListByRouteId(routeId);

        // 4. 组装 RespVO
        MesPdaWorkOrderRespVO respVO = new MesPdaWorkOrderRespVO()
                .setId(workOrder.getId())
                .setWorkOrderNo(workOrder.getCode())
                .setWorkOrderName(workOrder.getName())
                .setProductId(workOrder.getProductId())
                .setQuantity(workOrder.getQuantity())
                .setQuantityProduced(workOrder.getQuantityProduced())
                .setStatus(workOrder.getStatus())
                .setStatusName(getWorkOrderStatusName(workOrder.getStatus()))
                .setRequestDate(workOrder.getRequestDate())
                .setRouteId(routeId)
                .setScanTime(LocalDateTime.now());
        // 工艺路线信息
        if (routeId != null) {
            MesProRouteDO route = routeService.getRoute(routeId);
            if (route != null) {
                respVO.setRouteCode(route.getCode()).setRouteName(route.getName());
            }
        }
        // 产品信息
        if (workOrder.getProductId() != null) {
            MesMdItemDO item = itemService.getItem(workOrder.getProductId());
            if (item != null) {
                respVO.setProductCode(item.getCode()).setProductName(item.getName());
            }
        }
        // 工序列表
        List<MesPdaWorkOrderRespVO.ProcessInfo> processInfos = new ArrayList<>(routeProcesses.size());
        for (MesProRouteProcessDO rp : routeProcesses) {
            MesPdaWorkOrderRespVO.ProcessInfo info = new MesPdaWorkOrderRespVO.ProcessInfo()
                    .setProcessId(rp.getProcessId())
                    .setOrderNum(rp.getSort())
                    .setKeyFlag(rp.getKeyFlag())
                    .setCheckFlag(rp.getCheckFlag());
            // 工序编码/名称通过 itemService 暂不查（ProcessService 不提供批量查工序详情），由 PDA 自行加载
            processInfos.add(info);
        }
        respVO.setProcesses(processInfos);
        return success(respVO);
    }

    @PostMapping("/submit-feedback")
    @Operation(summary = "PDA 端提交报工（跳过审批直接完成）")
    @PermitAll
    public CommonResult<Long> submitFeedback(@Valid @RequestBody MesPdaFeedbackSubmitReqVO reqVO) {
        return success(feedbackService.pdaSubmitFeedback(reqVO));
    }

    @GetMapping("/my-feedback-list")
    @Operation(summary = "查询当前用户的报工记录")
    @Parameter(name = "feedbackUserId", description = "报工用户编号", required = true, example = "1")
    @PermitAll
    public CommonResult<List<MesProFeedbackRespVO>> myFeedbackList(@RequestParam("feedbackUserId") Long feedbackUserId) {
        List<MesProFeedbackDO> list = feedbackService.getFeedbackListByUser(feedbackUserId);
        return success(BeanUtils.toBean(list, MesProFeedbackRespVO.class));
    }

    @PostMapping("/scan-machinery")
    @Operation(summary = "扫描设备二维码，返回设备状态+当前工单")
    @PermitAll
    public CommonResult<MesPdaMachineryRespVO> scanMachinery(@Valid @RequestBody MesPdaScanMachineryReqVO reqVO) {
        // 1. 查询设备
        MesDvMachineryDO machinery = machineryMapper.selectByCode(reqVO.getMachineryCode());
        if (machinery == null) {
            throw exception(PRO_PDA_FEEDBACK_MACHINERY_NOT_EXISTS, reqVO.getMachineryCode());
        }
        // 2. 组装 RespVO
        MesPdaMachineryRespVO respVO = new MesPdaMachineryRespVO()
                .setId(machinery.getId())
                .setMachineryCode(machinery.getCode())
                .setMachineryName(machinery.getName())
                .setSpecification(machinery.getSpecification())
                .setStatus(machinery.getStatus())
                .setScanTime(LocalDateTime.now());
        // 3. 当前工单：暂未维护 workstation ↔ machinery 关系，留空；待 workstation 表新增 machineryId 后补全
        respVO.setStatusName("正常");
        return success(respVO);
    }

    private String getWorkOrderStatusName(Integer status) {
        if (status == null) {
            return null;
        }
        for (MesProWorkOrderStatusEnum e : MesProWorkOrderStatusEnum.values()) {
            if (e.getStatus().equals(status)) {
                return e.getName();
            }
        }
        return null;
    }

}
