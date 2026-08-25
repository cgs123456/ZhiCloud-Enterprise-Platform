package cn.zhicloud.module.wms.job.inventory;

import cn.zhicloud.framework.quartz.core.handler.JobHandler;
import cn.zhicloud.module.wms.service.inventory.batch.WmsBatchExpiryAlertService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * WMS 批次效期预警 Job
 *
 * <p>每天 8:00 扫描所有批次效期，更新批次状态（正常/临期预警/已过期）并生成效期预警记录。
 * 建议调度 cron：0 0 8 * * ?（每天 8:00 执行）
 *
 * @author 智云
 */
@Component
@Slf4j
public class WmsBatchExpiryAlertJob implements JobHandler {

    @Resource
    private WmsBatchExpiryAlertService batchExpiryAlertService;

    @Override
    public String execute(String param) {
        int count = batchExpiryAlertService.scanExpiryAlerts();
        log.info("[execute][批次效期预警扫描完成，共生成 {} 条预警]", count);
        return String.format("批次效期预警扫描完成，共生成 %s 条预警", count);
    }

}
