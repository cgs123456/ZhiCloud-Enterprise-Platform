package cn.iocoder.yudao.module.mes.service.dv.scada;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * MES 设备实时数据 Service 实现类
 *
 * <p>实现策略：
 * <ul>
 *   <li>实时数据缓存：{@link MesIotDeviceMessageListener} 监听 IoT 模块广播后调用
 *       {@link #onDeviceData(MesDeviceDataDTO)} 更新内存缓存，{@code getLatestDeviceData} 直接读缓存</li>
 *   <li>历史数据查询：当前 IoT 模块未启用，{@code iotApi} 为 null 时返回空列表；
 *       IoT 启用后可通过 {@code iotApi}（可选注入）扩展实现</li>
 *   <li>订阅推送：维护内存订阅表，{@code onDeviceData} 时回调通知</li>
 * </ul>
 *
 * <p>容错：{@code iotApi} 使用 {@code @Autowired(required=false)} 注入，IoT 模块未启用时为 null，
 * 实时数据功能降级为「仅缓存」，不影响 MES 主流程。
 *
 * @author 芋道源码
 */
@Service
@Slf4j
public class MesDvDeviceDataServiceImpl implements MesDvDeviceDataService {

    /**
     * IoT 模块对外 API（可选）
     *
     * <p>当前 {@code yudao-module-iot} 未启用，此处为 null。
     * 类型保持为 {@link Object} 以避免对 IoT 模块具体类的硬依赖；IoT 启用后可通过反射或适配器扩展。
     */
    @Autowired(required = false)
    private Object iotApi;

    /**
     * 每台设备的最新一帧数据（key = machineryId）
     */
    private final Map<Long, MesDeviceDataDTO> latestCache = new ConcurrentHashMap<>();

    /**
     * 每台设备的订阅回调列表（key = machineryId）
     */
    private final Map<Long, List<Consumer<MesDeviceDataDTO>>> subscribers = new ConcurrentHashMap<>();

    @Override
    public MesDeviceDataDTO getLatestDeviceData(Long machineryId) {
        if (machineryId == null) {
            return null;
        }
        return latestCache.get(machineryId);
    }

    @Override
    public List<MesDeviceDataDTO> listDeviceDataByTimeRange(Long machineryId, LocalDateTime start, LocalDateTime end) {
        if (machineryId == null || start == null || end == null) {
            return Collections.emptyList();
        }
        // IoT 模块未启用时无法查询历史时序数据，返回空列表
        if (iotApi == null) {
            log.debug("[getDeviceDataByTimeRange][IoT 模块未启用，返回空列表] machineryId={}", machineryId);
            return Collections.emptyList();
        }
        // TODO IoT 模块启用后，调用 iotApi 查询历史时序数据并转换为 MesDeviceDataDTO
        return Collections.emptyList();
    }

    @Override
    public void subscribeDeviceData(Long machineryId, Consumer<MesDeviceDataDTO> callback) {
        if (machineryId == null || callback == null) {
            return;
        }
        subscribers.computeIfAbsent(machineryId, k -> new CopyOnWriteArrayList<>()).add(callback);
    }

    /**
     * 接收 IoT 模块广播的设备数据（由 {@link MesIotDeviceMessageListener} 调用）
     *
     * <p>更新最新缓存并通知所有订阅者。
     *
     * @param data 设备数据
     */
    public void onDeviceData(MesDeviceDataDTO data) {
        if (data == null || data.getMachineryId() == null) {
            return;
        }
        latestCache.put(data.getMachineryId(), data);
        // 通知订阅者（防御性拷贝，避免回调中再次订阅导致 ConcurrentModification）
        List<Consumer<MesDeviceDataDTO>> callbacks = subscribers.get(data.getMachineryId());
        if (callbacks == null || callbacks.isEmpty()) {
            return;
        }
        for (Consumer<MesDeviceDataDTO> callback : new ArrayList<>(callbacks)) {
            try {
                callback.accept(data);
            } catch (Exception e) {
                log.warn("[onDeviceData][订阅回调异常] machineryId={}", data.getMachineryId(), e);
            }
        }
    }

}
