package cn.iocoder.yudao.module.mes.listener;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * IoT 设备消息事件
 *
 * <p>当 {@code yudao-module-iot} 启用并广播设备数据时，由 IoT 适配层发布此事件。
 * MES 侧通过 {@link MesIotDeviceMessageListener} 监听，更新实时数据缓存。
 *
 * <p>当前 IoT 模块未启用时，本事件不会被发布，监听器处于空转状态。
 *
 * @author 芋道源码
 */
public class MesIotDeviceMessageEvent {

    /**
     * IoT 平台设备 PK（与 {@code MesDvScadaConfigDO#iotDevicePk} 对齐）
     */
    private final String iotDevicePk;
    /**
     * IoT 平台产品编号
     */
    private final Long iotProductId;
    /**
     * 属性数据（key = 属性名，value = 属性值字符串）
     */
    private final Map<String, String> properties;
    /**
     * 数据采集时间戳
     */
    private final LocalDateTime timestamp;

    public MesIotDeviceMessageEvent(String iotDevicePk, Long iotProductId,
                                    Map<String, String> properties, LocalDateTime timestamp) {
        this.iotDevicePk = iotDevicePk;
        this.iotProductId = iotProductId;
        this.properties = properties;
        this.timestamp = timestamp;
    }

    public String getIotDevicePk() {
        return iotDevicePk;
    }

    public Long getIotProductId() {
        return iotProductId;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

}
