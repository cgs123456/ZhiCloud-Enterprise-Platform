package cn.zhicloud.module.hr.service.contract;

import cn.zhicloud.framework.tenant.core.job.TenantJob;
import cn.zhicloud.module.hr.dal.dataobject.contract.HrContractDO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class HrContractExpireJob {

    @Resource
    private HrContractService contractService;

    @Scheduled(cron = "0 0 1 * * ?")
    @TenantJob // 多租户逐个执行，否则定时线程无租户上下文会导致 NPE
    public void execute() {
        List<HrContractDO> expiringContracts = contractService.getExpiringContracts(30);
        log.info("[execute][扫描到 {} 条即将到期合同]", expiringContracts.size());
        for (HrContractDO contract : expiringContracts) {
            log.info("[execute][合同即将到期 contractNo={} employeeId={} endDate={}]",
                    contract.getContractNo(), contract.getEmployeeId(), contract.getEndDate());
            contractService.markExpiring(contract.getId());
        }
    }

}