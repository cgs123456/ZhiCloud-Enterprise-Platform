package cn.iocoder.yudao.module.mes.job;

import cn.iocoder.yudao.module.mes.service.dv.scada.MesDvDeviceDataCollectorService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * MES 设备数据自动采集 Job
 *
 * <p>每 30 秒触发一次，对所有已启用 SCADA 配置的设备执行一次数据采集。
 *
 * <p>启动时自动开启采集开关；如需关闭可调用 {@code /mes/dv-device-data/stop} 端点。
 *
 * @author 芋道源码
 */
@Component
@Slf4j
public class MesDvDeviceDataCollectJob {

    @Resource
    private MesDvDeviceDataCollectorService collectorService;

    /**
     * 启动时默认开启自动采集
     */
    @PostConstruct
    public void init() {
        collectorService.startAutoCollection();
    }

    /**
     * 每 30 秒触发一次采集
     */
    @Scheduled(cron = "0/30 * * * * ?")
    public void collect() {
        try {
            int count = collectorService.collectAll();
            if (count > 0) {
                log.debug("[collect][本次采集 {} 条设备数据记录]", count);
            }
        } catch (Exception e) {
            log.error("[collect][自动采集异常]", e);
        }
    }

}
