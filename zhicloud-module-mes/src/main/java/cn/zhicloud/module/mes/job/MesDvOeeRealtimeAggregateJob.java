package cn.zhicloud.module.mes.job;

import cn.zhicloud.framework.common.enums.CommonStatusEnum;
import cn.zhicloud.module.mes.dal.dataobject.dv.oeerecord.MesDvOeeRecordDO;
import cn.zhicloud.module.mes.dal.dataobject.dv.scada.MesDvScadaConfigDO;
import cn.zhicloud.module.mes.dal.mysql.dv.oeerecord.MesDvOeeRecordMapper;
import cn.zhicloud.module.mes.service.dv.scada.MesDeviceDataDTO;
import cn.zhicloud.module.mes.service.dv.scada.MesDvDeviceDataService;
import cn.zhicloud.module.mes.service.dv.scada.MesDvScadaConfigService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * MES OEE 实时聚合 Job
 *
 * <p>每分钟把 SCADA 实时数据聚合写入 {@code mes_dv_oeerecord}，作为 OEE 实时计算的数据源。
 *
 * <p>聚合规则（每分钟每台设备一行）：
 * <ul>
 *   <li>{@code recordDate}：当前分钟（截断到分钟）</li>
 *   <li>{@code runTime}：设备运行状态为 true 时记 1 分钟，否则 0</li>
 *   <li>{@code totalProduced / goodProduced}：从计数类属性取值（counter / goodCount）</li>
 *   <li>{@code plannedProductionTime}：默认 1 分钟（计划内全部可用）</li>
 *   <li>其余 OEE 指标字段留空，由 {@code MesDvOeeServiceImpl#calculateOee} 在区间聚合时计算</li>
 * </ul>
 *
 * <p>当前 IoT 模块未启用时，{@code getLatestDeviceData} 返回 null，本 Job 不写入任何记录。
 *
 * @author 智云
 */
@Component
@Slf4j
public class MesDvOeeRealtimeAggregateJob {

    @Resource
    private MesDvScadaConfigService scadaConfigService;
    @Resource
    private MesDvDeviceDataService deviceDataService;
    @Resource
    private MesDvOeeRecordMapper oeeRecordMapper;

    /**
     * 每分钟执行一次（cron：每分钟第 0 秒）
     */
    @Scheduled(cron = "0 * * * * ?")
    public void aggregate() {
        // 1. 查询所有启用的 SCADA 配置（分页全量）
        cn.zhicloud.module.mes.controller.admin.dv.scada.vo.MesDvScadaConfigPageReqVO pageReqVO =
                new cn.zhicloud.module.mes.controller.admin.dv.scada.vo.MesDvScadaConfigPageReqVO();
        pageReqVO.setEnabled(CommonStatusEnum.ENABLE.getStatus());
        pageReqVO.setPageSize(cn.zhicloud.framework.common.pojo.PageParam.PAGE_SIZE_NONE);
        var configs = scadaConfigService.getScadaConfigPage(pageReqVO).getList();
        if (configs.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        // 2. 逐台设备聚合
        for (MesDvScadaConfigDO config : configs) {
            try {
                aggregateOne(config, now);
            } catch (Exception e) {
                log.warn("[aggregate][设备 {} 实时聚合失败]", config.getMachineryId(), e);
            }
        }
    }

    private void aggregateOne(MesDvScadaConfigDO config, LocalDateTime recordDate) {
        MesDeviceDataDTO latest = deviceDataService.getLatestDeviceData(config.getMachineryId());
        // 无实时数据时跳过（IoT 未启用或设备未上报）
        if (latest == null) {
            return;
        }
        // 1. 解析运行状态与计数
        boolean running = parseRunningStatus(latest);
        BigDecimal totalProduced = parseCounter(latest, "counter", "count", "qty");
        BigDecimal goodProduced = parseCounter(latest, "goodcount", "good", "qualified");

        // 2. 构建分钟级 OEE 记录
        MesDvOeeRecordDO record = MesDvOeeRecordDO.builder()
                .machineryId(config.getMachineryId())
                .recordDate(recordDate)
                .plannedProductionTime(BigDecimal.ONE) // 计划 1 分钟
                .runTime(running ? BigDecimal.ONE : BigDecimal.ZERO)
                .totalProduced(totalProduced)
                .goodProduced(goodProduced)
                .build();
        oeeRecordMapper.insert(record);
    }

    /**
     * 解析运行状态：属性名含 status/running/flag 且值为 true/1/running 视为运行中
     */
    private boolean parseRunningStatus(MesDeviceDataDTO data) {
        String name = data.getPropertyName();
        String value = data.getPropertyValue();
        if (name == null || value == null) {
            return false;
        }
        String lowerName = name.toLowerCase();
        if (!(lowerName.contains("status") || lowerName.contains("running") || lowerName.contains("flag"))) {
            return false;
        }
        String lowerValue = value.toLowerCase();
        return "true".equals(lowerValue) || "1".equals(lowerValue) || "running".equals(lowerValue);
    }

    /**
     * 解析计数类属性值
     */
    private BigDecimal parseCounter(MesDeviceDataDTO data, String... keywords) {
        if (data == null || data.getPropertyName() == null || data.getPropertyValue() == null) {
            return BigDecimal.ZERO;
        }
        String lowerName = data.getPropertyName().toLowerCase();
        for (String kw : keywords) {
            if (lowerName.contains(kw)) {
                try {
                    return new BigDecimal(data.getPropertyValue());
                } catch (NumberFormatException e) {
                    return BigDecimal.ZERO;
                }
            }
        }
        return BigDecimal.ZERO;
    }

}
