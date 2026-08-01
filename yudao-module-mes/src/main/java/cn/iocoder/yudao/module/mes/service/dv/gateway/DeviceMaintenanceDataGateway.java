package cn.iocoder.yudao.module.mes.service.dv.gateway;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备维护数据 SPI 网关
 *
 * <p>用于解耦 {@code yudao-module-ai} 与 {@code yudao-module-mes} 模块。
 * ai 模块（预测性维护）通过此接口获取 MES 侧的设备维修记录与 KPI 数据，
 * mes 模块提供 {@code MesDeviceMaintenanceDataGatewayImpl} 实现。
 *
 * <p>本接口定义在 MES 模块（数据提供方），避免 ai↔mes 的循环依赖：
 * ai 模块已 optional 依赖 mes（用于 MCP Tool 包装），因此可直接引用本接口；
 * mes 模块实现本接口无需反向依赖 ai。ai 侧通过 {@code @Autowired(required = false)}
 * 注入，未提供实现时预测性维护功能降级为不可用。
 *
 * @author yudao
 */
public interface DeviceMaintenanceDataGateway {

    /**
     * 获取设备最近一次维修完成时间
     *
     * @param deviceId 设备编号
     * @return 最近一次维修完成时间，无记录时返回 null
     */
    LocalDateTime getLastRepairTime(Long deviceId);

    /**
     * 获取设备维修记录总数
     *
     * @param deviceId 设备编号
     * @return 维修记录数
     */
    Long getRepairCount(Long deviceId);

    /**
     * 获取设备历史 KPI（MTBF/MTTR）列表，按周期升序
     *
     * @param deviceId 设备编号
     * @param limit 最大返回条数（取最近 N 个周期）
     * @return KPI 列表，每项包含 period、mtbf、mttr；无数据时返回空列表
     */
    List<KpiSnapshot> getKpiHistory(Long deviceId, int limit);

    /**
     * 获取设备最近一期 KPI 快照
     *
     * @param deviceId 设备编号
     * @return 最近一期 KPI，无数据时返回 null
     */
    KpiSnapshot getLatestKpi(Long deviceId);

    /**
     * KPI 快照
     *
     * @param period 周期（yyyyMM）
     * @param mtbf 平均故障间隔时间（小时）
     * @param mttr 平均修复时间（小时）
     */
    record KpiSnapshot(String period, BigDecimal mtbf, BigDecimal mttr) {
    }

}
