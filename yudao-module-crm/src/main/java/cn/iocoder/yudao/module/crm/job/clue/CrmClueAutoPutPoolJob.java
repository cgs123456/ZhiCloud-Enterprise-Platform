package cn.iocoder.yudao.module.crm.job.clue;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.crm.service.clue.CrmClueService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 线索自动掉入公海 Job
 *
 * @author 芋道源码
 */
@Component
public class CrmClueAutoPutPoolJob implements JobHandler {

    @Resource
    private CrmClueService clueService;

    @Override
    @TenantJob
    public String execute(String param) {
        int count = clueService.autoPutCluePool();
        return String.format("掉入公海线索 %s 个", count);
    }

}
