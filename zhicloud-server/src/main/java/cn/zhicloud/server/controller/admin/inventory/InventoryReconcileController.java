package cn.zhicloud.server.controller.admin.inventory;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.inventory.service.InventoryReconcileReport;
import cn.zhicloud.framework.inventory.service.InventoryReconciliationService;
import cn.zhicloud.server.service.inventory.InventoryReconcileAlertResult;
import cn.zhicloud.server.service.inventory.InventoryReconcileAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 库存日终对账只读视图（§12.146 R-007）
 *
 * <p>背景：{@link InventoryReconciliationService} 早已作为 {@code @Bean} 接好（真值源
 * {@code inventory_item} vs ERP/MES 各 {@code InventoryProjectionReader} 投影），
 * 但除单测外**没有任何调用方**——漂移只在 log.warn 里冒头，运维查不到。本端点把"看得到"
 * 这一步补上：只读、按需触发、返回结构化差异报告（{@code isConsistent} + mismatches 列表），
 * 是 §12.144 R-007"漂移告警/对账差异处置"里能安全落地的最小一块（"告警"那半属需 owner
 * 定的运维设施，本轮不臆造）。对账本身不做任何写。
 *
 * <p>权限串 {@code erp:inventory-reconcile:query} 与 V149 种子同步（凭空造权限串会恒 403）。
 * 放 server 的 {@code controller.admin.*} 包下由框架自动加 /admin-api 前缀。
 *
 * @author 智云库存治理
 */
@Tag(name = "管理后台 - 库存对账")
@RestController
@RequestMapping("/erp/inventory-reconcile")
@Validated
public class InventoryReconcileController {

    @Resource
    private InventoryReconciliationService reconciliationService;

    @Resource
    private InventoryReconcileAlertService alertService;

    @GetMapping("/run")
    @Operation(summary = "执行一次库存对账并返回差异报告（只读）")
    @PreAuthorize("@ss.hasPermission('erp:inventory-reconcile:query')")
    public CommonResult<InventoryReconcileReport> run() {
        return success(reconciliationService.reconcile());
    }

    @PostMapping("/alert")
    @Operation(summary = "手动执行库存对账并向 webhook 推送不一致告警（无自动排程）")
    @PreAuthorize("@ss.hasPermission('erp:inventory-reconcile:query')")
    public CommonResult<InventoryReconcileAlertResult> pushAlert() {
        return success(alertService.pushAlertIfNeeded());
    }

}
