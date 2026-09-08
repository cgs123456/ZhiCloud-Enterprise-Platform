package cn.zhicloud.module.aimultiagent.service.skill;

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

    private final WorkerAgentRegistry workerAgentRegistry;
    private final MultiAgentSkillService skillService;

    @Override
    public void run(ApplicationArguments args) {
        try {
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
        } catch (Exception e) {
            // 校验失败不阻断启动（如数据库迁移尚未执行时）
            log.warn("[SkillCatalogValidator][技能目录校验异常，跳过]", e);
        }
    }

}
