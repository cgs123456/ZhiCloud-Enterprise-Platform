package cn.zhicloud.module.mes.controller.app.pro;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.mes.controller.admin.pro.feedback.vo.MesProFeedbackRespVO;
import cn.zhicloud.module.mes.controller.app.pro.vo.MesPdaFeedbackSubmitReqVO;
import cn.zhicloud.module.mes.controller.app.pro.vo.MesPdaMachineryRespVO;
import cn.zhicloud.module.mes.controller.app.pro.vo.MesPdaScanMachineryReqVO;
import cn.zhicloud.module.mes.controller.app.pro.vo.MesPdaScanWorkOrderReqVO;
import cn.zhicloud.module.mes.controller.app.pro.vo.MesPdaWorkOrderRespVO;
import cn.zhicloud.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.zhicloud.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.task.MesProTaskDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.zhicloud.module.mes.dal.mysql.dv.machinery.MesDvMachineryMapper;
import cn.zhicloud.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import cn.zhicloud.module.mes.service.md.item.MesMdItemService;
import cn.zhicloud.module.mes.service.pro.feedback.MesProFeedbackService;
import cn.zhicloud.module.mes.service.pro.route.MesProRouteProcessService;
import cn.zhicloud.module.mes.service.pro.route.MesProRouteService;
import cn.zhicloud.module.mes.service.pro.task.MesProTaskService;
import cn.zhicloud.module.mes.service.pro.workorder.MesProWorkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.zhicloud.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.framework.common.pojo.CommonResult.success;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_PDA_FEEDBACK_MACHINERY_NOT_EXISTS;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_PDA_FEEDBACK_WORK_ORDER_NOT_EXISTS;

/**
 * MES PDA / 工位机报工接口
 *
 * <p>面向 PDA 工位机的简化报工流程，跳过审批直接完成。
 *
 * <p><b>鉴权说明</b>：本类接口此前统一标注 {@code @PermitAll}，注释声称「由 App 端 token 认证」——
 * 但 {@code @PermitAll} 的语义恰恰是<b>跳过</b>登录态校验，注释与实现相反，
 * 实际效果是报工、扫工单、扫设备三个接口对未认证请求完全开放：
 * 攻击者可匿名灌入伪造报工数据污染产量/工时统计，或枚举全部工单号与工艺路线。
 * 现已全部改为 {@code @PreAuthorize} 权限校验（权限码见 V76 迁移脚本）。
 *
 * @author 智云
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
    @PreAuthorize("@ss.hasPermission('mes:pda:scan')")
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
    @PreAuthorize("@ss.hasPermission('mes:pda:feedback')")
    public CommonResult<Long> submitFeedback(@Valid @RequestBody MesPdaFeedbackSubmitReqVO reqVO) {
        return success(feedbackService.pdaSubmitFeedback(reqVO));
    }

    /**
     * 查询「当前登录用户」的报工记录。
     *
     * <p>此前该接口是 {@code @PermitAll} + 从请求参数读取 {@code feedbackUserId}，
     * 等于匿名越权（IDOR）：任何人遍历自增 ID 即可读取全厂人员的报工明细
     * （含工时、产量、不良数，可反推绩效与产能）。
     * 现改为从登录态推导用户身份，请求参数不再参与查询。
     */
    @GetMapping("/my-feedback-list")
    @Operation(summary = "查询当前用户的报工记录")
    @PreAuthorize("@ss.hasPermission('mes:pda:feedback')")
    public CommonResult<List<MesProFeedbackRespVO>> myFeedbackList() {
        List<MesProFeedbackDO> list = feedbackService.getFeedbackListByUser(getLoginUserId());
        return success(BeanUtils.toBean(list, MesProFeedbackRespVO.class));
    }

    @PostMapping("/scan-machinery")
    @Operation(summary = "扫描设备二维码，返回设备状态+当前工单")
    @PreAuthorize("@ss.hasPermission('mes:pda:scan')")
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
