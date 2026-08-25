package cn.zhicloud.module.crm.job.clue;

import cn.zhicloud.framework.quartz.core.handler.JobHandler;
import cn.zhicloud.framework.tenant.core.job.TenantJob;
import cn.zhicloud.module.crm.service.clue.CrmClueService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 线索自动掉入公海 Job
 *
 * @author 智云
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
