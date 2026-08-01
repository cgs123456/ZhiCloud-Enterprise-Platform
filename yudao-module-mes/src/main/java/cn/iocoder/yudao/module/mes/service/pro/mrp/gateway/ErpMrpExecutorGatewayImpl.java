package cn.iocoder.yudao.module.mes.service.pro.mrp.gateway;

import cn.iocoder.yudao.module.erp.dal.dataobject.production.mps.ErpMpsPlanDO;
import cn.iocoder.yudao.module.erp.service.production.mps.gateway.ErpMrpExecutorGateway;
import cn.iocoder.yudao.module.mes.controller.admin.pro.mrp.vo.MesProMrpPlanCreateReqVO;
import cn.iocoder.yudao.module.mes.service.pro.mrp.MesProMrpService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * {@link ErpMrpExecutorGateway} 实现
 *
 * <p>委托给 {@link MesProMrpService}，实现 yudao-module-erp 模块
 * 对 yudao-module-mes 模块 MRP 运算能力的反向调用（SPI 模式）。
 *
 * <p>当 MPS 计划下发 MRP 时，通过此实现类：
 * <ol>
 *     <li>基于 MPS 计划创建 MRP 计划（planNo 由 MPS 计划号派生）</li>
 *     <li>执行 MRP 展算（BOM 递归展开 → 净需求 → 生成计划采购建议）</li>
 * </ol>
 *
 * @author ERP 修复
 */
@Component
@Slf4j
public class ErpMrpExecutorGatewayImpl implements ErpMrpExecutorGateway {

    @Resource
    private MesProMrpService mesProMrpService;

    @Override
    public void executeMrp(ErpMpsPlanDO plan) {
        log.info("[executeMrp][MPS 下发 MRP，planId({}) planNo({}) product({}) period({}) quantity({})]",
                plan.getId(), plan.getPlanNo(), plan.getProductId(), plan.getPlanPeriod(), plan.getPlannedQuantity());
        // 1. 基于 MPS 计划创建 MRP 计划
        MesProMrpPlanCreateReqVO mrpPlanReq = new MesProMrpPlanCreateReqVO();
        mrpPlanReq.setPlanNo("MRP-" + plan.getPlanNo());
        mrpPlanReq.setRemark("由 MPS 计划下发：planNo=" + plan.getPlanNo()
                + "，product=" + plan.getProductName()
                + "，period=" + plan.getPlanPeriod()
                + "，quantity=" + plan.getPlannedQuantity());
        Long mrpPlanId = mesProMrpService.createMrpPlan(mrpPlanReq);
        // 2. 执行 MRP 展算（BOM 展开 → 减库存 → 净需求 → 计划采购订单建议）
        mesProMrpService.calculateMrp(mrpPlanId);
        log.info("[executeMrp][MPS 下发 MRP 完成，mrpPlanId({})]", mrpPlanId);
    }

}
