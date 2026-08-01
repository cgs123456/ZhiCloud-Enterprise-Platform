package cn.iocoder.yudao.module.mes.controller.admin.home;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.framework.websocket.MesDashboardPushService;
import cn.iocoder.yudao.module.mes.framework.websocket.MesDashboardWebSocketHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * MES 生产看板 Controller
 *
 * 提供看板 WebSocket 连接信息与同步快照接口（兼容无 WS 场景）。
 *
 * 前端连接示例：ws://host:48080/ws/mes/dashboard
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - MES 生产看板")
@RestController
@RequestMapping("/mes/dashboard")
public class MesDashboardController {

    @Resource
    private MesDashboardWebSocketHandler mesDashboardWebSocketHandler;
    @Resource
    private MesDashboardPushService mesDashboardPushService;

    @GetMapping("/connect")
    @Operation(summary = "获得看板 WebSocket 连接信息")
    @PreAuthorize("@ss.hasPermission('mes:home:query')")
    public CommonResult<Map<String, Object>> connect() {
        // 前端连接示例：ws://host:48080/ws/mes/dashboard
        Map<String, Object> info = new HashMap<>();
        info.put("endpoint", "/ws/mes/dashboard");
        info.put("protocol", "ws");
        info.put("onlineCount", mesDashboardWebSocketHandler.getOnlineCount());
        return success(info);
    }

    @GetMapping("/snapshot")
    @Operation(summary = "获得看板同步快照（兼容无 WS 场景）")
    @PreAuthorize("@ss.hasPermission('mes:home:query')")
    public CommonResult<Map<String, Object>> snapshot() {
        return success(mesDashboardPushService.buildSnapshot());
    }

}