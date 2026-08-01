package cn.iocoder.yudao.module.mes.framework.websocket;

import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryService;
import cn.iocoder.yudao.module.mes.service.dv.scada.MesDeviceDataDTO;
import cn.iocoder.yudao.module.mes.service.dv.scada.MesDvDeviceDataService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MES 设备遥测实时推送 Service 实现类
 *
 * <p>实现策略：
 * <ul>
 *   <li>订阅注册：内存维护 sessionId → 设备编号集合的映射（{@link #machinerySubscriptions}）；
 *       车间订阅会展开为该车间下的设备编号集合</li>
 *   <li>定时推送：每 5 秒构建一次遥测快照（聚合所有订阅设备的最实时数据），
 *       通过 {@link MesDashboardWebSocketHandler#sendMessageToAll(String)} 广播。
 *       因现有 Handler 仅支持广播，前端按自身订阅过滤消息类型为 {@code device-telemetry} 的负载</li>
 *   <li>无订阅 / 无连接时跳过，减少查询压力</li>
 * </ul>
 *
 * @author 芋道源码
 */
@Service
@Slf4j
public class MesDeviceTelemetryPushServiceImpl implements MesDeviceTelemetryPushService {

    @Resource
    private MesDashboardWebSocketHandler mesDashboardWebSocketHandler;
    @Resource
    private MesDvDeviceDataService deviceDataService;
    @Resource
    private MesDvMachineryService machineryService;
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 每个会话订阅的设备编号集合（key = sessionId）
     */
    private final Map<String, Set<Long>> machinerySubscriptions = new ConcurrentHashMap<>();

    @Override
    public void subscribeByMachinery(String sessionId, List<Long> machineryIds) {
        if (sessionId == null || machineryIds == null || machineryIds.isEmpty()) {
            return;
        }
        machinerySubscriptions.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet()).addAll(machineryIds);
    }

    @Override
    public void subscribeByWorkshop(String sessionId, Long workshopId) {
        if (sessionId == null || workshopId == null) {
            return;
        }
        // 展开车间下所有设备
        List<MesDvMachineryDO> all = machineryService.getMachineryList();
        List<Long> machineryIds = new ArrayList<>();
        for (MesDvMachineryDO m : all) {
            if (workshopId.equals(m.getWorkshopId())) {
                machineryIds.add(m.getId());
            }
        }
        subscribeByMachinery(sessionId, machineryIds);
    }

    @Override
    public void unsubscribe(String sessionId) {
        if (sessionId == null) {
            return;
        }
        machinerySubscriptions.remove(sessionId);
    }

    /**
     * 每 5 秒推送一次遥测数据
     *
     * <p>fixedDelay=5000：上一次推送结束后再等 5 秒，避免堆积
     */
    @Scheduled(fixedDelay = 5000)
    public void pushTelemetry() {
        // 1. 无连接客户端或无订阅时跳过
        if (mesDashboardWebSocketHandler.getOnlineCount() == 0 || machinerySubscriptions.isEmpty()) {
            return;
        }
        // 2. 聚合所有订阅设备编号（去重）
        Set<Long> machineryIdSet = new HashSet<>();
        for (Set<Long> set : machinerySubscriptions.values()) {
            machineryIdSet.addAll(set);
        }
        if (machineryIdSet.isEmpty()) {
            return;
        }
        // 3. 取每台设备的最新遥测
        List<Map<String, Object>> telemetryList = new ArrayList<>();
        for (Long machineryId : machineryIdSet) {
            MesDeviceDataDTO data = deviceDataService.getLatestDeviceData(machineryId);
            if (data == null) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("machineryId", data.getMachineryId());
            item.put("timestamp", data.getTimestamp() != null ? data.getTimestamp().toString() : null);
            item.put("propertyName", data.getPropertyName());
            item.put("propertyValue", data.getPropertyValue());
            item.put("dataType", data.getDataType());
            telemetryList.add(item);
        }
        if (telemetryList.isEmpty()) {
            return;
        }
        // 4. 广播
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "device-telemetry");
        payload.put("timestamp", LocalDateTime.now().toString());
        payload.put("data", telemetryList);
        try {
            mesDashboardWebSocketHandler.sendMessageToAll(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            log.warn("[pushTelemetry][序列化遥测数据失败]", e);
        }
    }

    /**
     * 会话断开时清理订阅（由 WebSocket Handler 在 afterConnectionClosed 中调用）
     */
    public void onSessionClosed(String sessionId) {
        machinerySubscriptions.remove(sessionId);
    }

    /**
     * 当前订阅会话数（供监控/测试使用）
     */
    public int getSubscriptionCount() {
        return machinerySubscriptions.size();
    }

    /**
     * 只读视图：获取某会话的订阅设备编号
     */
    public Set<Long> getSubscribedMachineryIds(String sessionId) {
        return Collections.unmodifiableSet(machinerySubscriptions.getOrDefault(sessionId, Collections.emptySet()));
    }

}
