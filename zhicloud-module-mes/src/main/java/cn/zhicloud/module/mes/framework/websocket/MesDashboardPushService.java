package cn.zhicloud.module.mes.framework.websocket;

import cn.zhicloud.module.mes.controller.admin.home.vo.MesHomeProductionTrendRespVO;
import cn.zhicloud.module.mes.controller.admin.home.vo.MesHomeSummaryRespVO;
import cn.zhicloud.module.mes.controller.admin.home.vo.MesHomeWorkOrderStatusRespVO;
import cn.zhicloud.module.mes.service.home.MesHomeStatisticsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MES 生产看板定时推送 Service
 *
 * 每 5 秒推送一次看板数据（汇总 + 工单状态分布 + 生产趋势 + 安灯报警）给所有 WS 连接的客户端。
 * 调用现有 {@link MesHomeStatisticsService} 聚合数据，与首页统计接口同源。
 *
 * @author 智云
 */
@Service
@Slf4j
public class MesDashboardPushService {

    @Resource
    private MesDashboardWebSocketHandler mesDashboardWebSocketHandler;

    @Resource
    private MesHomeStatisticsService homeStatisticsService;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 每 5 秒推送一次看板数据
     *
     * fixedDelay=5000：上一次推送结束后再等 5 秒执行下一次，避免推送堆积
     */
    @Scheduled(fixedDelay = 5000)
    public void pushDashboard() {
        // 无连接客户端时跳过，减少统计查询压力
        if (mesDashboardWebSocketHandler.getOnlineCount() == 0) {
            return;
        }
        try {
            Map<String, Object> snapshot = buildSnapshot();
            String payload = objectMapper.writeValueAsString(snapshot);
            mesDashboardWebSocketHandler.sendMessageToAll(payload);
        } catch (Exception e) {
            log.warn("[mesDashboard][推送看板数据失败]", e);
        }
    }

    /**
     * 构建看板快照（同时供同步接口 /dashboard/snapshot 使用）
     *
     * @return 看板数据 Map（会被序列化为 JSON 推送）
     */
    public Map<String, Object> buildSnapshot() {
        MesHomeSummaryRespVO summary = homeStatisticsService.getHomeSummary();
        List<MesHomeWorkOrderStatusRespVO> workOrderStatus = homeStatisticsService.getWorkOrderStatusDistribution();
        List<MesHomeProductionTrendRespVO> productionTrend = homeStatisticsService.getProductionTrend(7);

        Map<String, Object> payload = new HashMap<>();
        payload.put("timestamp", LocalDateTime.now().toString());
        payload.put("summary", summary);
        payload.put("workOrderStatus", workOrderStatus);
        payload.put("productionTrend", productionTrend);
        // andon-alert 数据：summary.andonActiveCount 即未处置安灯报警数
        payload.put("andonActiveCount", summary != null ? summary.getAndonActiveCount() : null);
        return payload;
    }

    /**
     * 将快照序列化为 JSON 字符串（供 WS 推送）
     */
    public String buildSnapshotJson() throws JsonProcessingException {
        return objectMapper.writeValueAsString(buildSnapshot());
    }

}