package cn.zhicloud.module.mes.framework.websocket;

import java.util.List;

/**
 * MES 设备遥测实时推送 Service 接口
 *
 * <p>按设备 / 车间维度订阅 SCADA 实时遥测数据，通过 WebSocket 推送给前端看板。
 * 复用现有 {@link MesDashboardWebSocketHandler} 的连接通道。
 *
 * @author 智云
 */
public interface MesDeviceTelemetryPushService {

    /**
     * 订阅指定设备的遥测数据
     *
     * @param sessionId WebSocket 会话 ID
     * @param machineryIds 设备编号列表
     */
    void subscribeByMachinery(String sessionId, List<Long> machineryIds);

    /**
     * 订阅指定车间下所有设备的遥测数据
     *
     * @param sessionId WebSocket 会话 ID
     * @param workshopId 车间编号
     */
    void subscribeByWorkshop(String sessionId, Long workshopId);

    /**
     * 取消订阅
     *
     * @param sessionId WebSocket 会话 ID
     */
    void unsubscribe(String sessionId);

}
