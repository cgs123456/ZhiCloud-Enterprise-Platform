package cn.zhicloud.module.mes.service.dv.scada;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

/**
 * MES 设备实时数据 Service 接口
 *
 * <p>封装与 {@code zhicloud-module-iot} 的对接逻辑，提供设备实时属性数据的获取与订阅能力。
 * 当前 IoT 模块未启用时，实现侧通过 {@code @Autowired(required=false)} 容错，相关方法返回空值。
 *
 * @author 智云
 */
public interface MesDvDeviceDataService {

    /**
     * 获取设备最新一帧属性数据
     *
     * @param machineryId MES 设备编号
     * @return 最新数据；无数据或 IoT 模块未启用时返回 null
     */
    MesDeviceDataDTO getLatestDeviceData(Long machineryId);

    /**
     * 按时间范围查询设备历史数据
     *
     * @param machineryId MES 设备编号
     * @param start 开始时间（含）
     * @param end 结束时间（含）
     * @return 数据列表，按时间升序；无数据时返回空列表
     */
    List<MesDeviceDataDTO> listDeviceDataByTimeRange(Long machineryId, LocalDateTime start, LocalDateTime end);

    /**
     * 订阅设备实时数据推送
     *
     * <p>当 IoT 模块广播设备消息时，回调会被触发。订阅信息保存在内存中，进程重启后失效。
     *
     * @param machineryId MES 设备编号
     * @param callback 数据回调
     */
    void subscribeDeviceData(Long machineryId, Consumer<MesDeviceDataDTO> callback);

}
