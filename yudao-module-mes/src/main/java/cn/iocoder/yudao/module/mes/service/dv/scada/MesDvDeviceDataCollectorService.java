package cn.iocoder.yudao.module.mes.service.dv.scada;

import cn.iocoder.yudao.module.mes.dal.dataobject.dv.scada.MesDvDeviceDataRecordDO;

import java.util.List;

/**
 * MES 设备数据采集器 Service 接口
 *
 * <p>负责按 {@code MesDvScadaConfigDO} 配置触发实际的数据采集，结果落库到
 * {@link MesDvDeviceDataRecordDO}。
 *
 * <p>支持 MQTT / Modbus TCP / OPC-UA 三种协议；当对应协议依赖未启用时降级为 stub
 * 返回模拟数据，保证主流程不中断。
 *
 * @author 芋道源码
 */
public interface MesDvDeviceDataCollectorService {

    /**
     * 触发一次设备数据采集
     *
     * @param machineryId MES 设备编号
     * @return 采集到的数据记录列表；采集失败或未配置时返回空列表
     */
    List<MesDvDeviceDataRecordDO> collectDeviceData(Long machineryId);

    /**
     * 启动自动采集
     *
     * <p>开启后由后台 Job 周期性触发采集所有已启用的 SCADA 配置。
     */
    void startAutoCollection();

    /**
     * 停止自动采集
     */
    void stopAutoCollection();

    /**
     * 执行一次全量采集（所有已启用的 SCADA 配置）
     *
     * @return 本次采集的记录总数
     */
    int collectAll();

}
