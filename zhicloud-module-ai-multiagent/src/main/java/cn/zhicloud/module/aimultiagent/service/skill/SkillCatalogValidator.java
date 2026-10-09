package cn.zhicloud.module.aimultiagent.service.skill;

import cn.zhicloud.framework.tenant.core.util.TenantUtils;
import cn.zhicloud.module.aimultiagent.dal.dataobject.MultiAgentSkillDO;
import cn.zhicloud.module.aimultiagent.service.agent.AbstractWorkerAgent;
import cn.zhicloud.module.aimultiagent.service.agent.WorkerAgentRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 技能目录一致性校验器
 *
 * <p>应用启动后，比对各 Worker {@code getSupportedTools()} 声明的工具与
 * aimultiagent_skill 目录（Level-2）的绑定关系：
 * <ul>
 *   <li>工具有声明但目录缺失 → WARN（提醒补录，不阻断启动）</li>
 *   <li>目录绑定了未知工具 → WARN（提醒修正，不阻断启动）</li>
 *   <li>技能/分组被禁用 → WARN（提醒该工具调用时将无目录背书）</li>
 * </ul>
 *
 * @author zhicloud
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Integer.MAX_VALUE)
public class SkillCatalogValidator implements ApplicationRunner {

    /**
     * 技能目录所属租户：平台全局租户 0。
     *
     * <p>依据 V87__aimultiagent_skill.sql，aimultiagent_skill / aimultiagent_skill_group
     * 的种子行 tenant_id 全部为 0（技能目录是平台级目录，不按业务租户切分）。
     */
    private static final long PLATFORM_TENANT_ID = 0L;

    private final WorkerAgentRegistry workerAgentRegistry;
    private final MultiAgentSkillService skillService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            // 本 Runner 在启动线程执行，TenantContextHolder 为空；而技能目录是租户 0 的平台目录，
            // 不显式切换租户就会被租户拦截器抛「不存在租户编号」，异常被下方 catch 吞掉 →
            // 校验形同死代码（本项目已有「电子签名切面从未被触发」的先例，故这里必须真跑起来）。
            // 与 ErpAccountingEventServiceImpl 读取平台科目同一写法：TenantUtils.execute(0L, ...)。
            TenantUtils.execute(PLATFORM_TENANT_ID, this::validateCatalog);
        } catch (Exception e) {
            // 校验失败不阻断启动（如数据库迁移尚未执行时）
            log.warn("[SkillCatalogValidator][技能目录校验异常，跳过]", e);
        }
    }

    private void validateCatalog() {
        List<AbstractWorkerAgent> workers = workerAgentRegistry.listWorkers();
        if (workers == null || workers.isEmpty()) {
            log.info("[SkillCatalogValidator][无已注册 Worker，跳过技能目录校验]");
            return;
        }
        int missing = 0;
        for (AbstractWorkerAgent worker : workers) {
            List<String> tools = worker.getSupportedTools();
            if (tools == null) {
                continue;
            }
            for (String tool : tools) {
                MultiAgentSkillDO skill = skillService.getSkillByToolName(tool);
                if (skill == null) {
                    missing++;
                    log.warn("[SkillCatalogValidator][Worker={} 声明的工具 {} 在技能目录中缺失，请补录]",
                            worker.getName(), tool);
                } else if (Integer.valueOf(1).equals(skill.getStatus())) {
                    log.warn("[SkillCatalogValidator][Worker={} 的工具 {} 对应技能({})已被禁用]",
                            worker.getName(), tool, skill.getCode());
                }
            }
        }
        log.info("[SkillCatalogValidator][技能目录校验完成，Worker 数={}，缺失目录的工具数={}]",
                workers.size(), missing);
    }

}
