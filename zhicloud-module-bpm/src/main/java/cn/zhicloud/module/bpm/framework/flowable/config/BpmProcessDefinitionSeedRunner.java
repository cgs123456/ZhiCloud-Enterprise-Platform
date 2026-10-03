package cn.zhicloud.module.bpm.framework.flowable.config;

import cn.hutool.core.collection.CollUtil;
import cn.zhicloud.module.bpm.service.definition.BpmProcessDefinitionSeedService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 启动时的流程定义播种 Runner
 * <p>
 * 它是「显式开启」的入口：默认不存在 {@code zhicloud.bpm.process-definition-seed.enabled} 配置，即不会执行。
 * 需要为某些租户补齐流程定义时（例如新环境初始化、集成测试环境），显式配置租户和流程定义清单即可，
 * 重复执行安全（同租户同 KEY 已有激活定义时跳过）。
 *
 * @author 智云
 */
@Component
@ConditionalOnProperty(prefix = "zhicloud.bpm.process-definition-seed", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(BpmProcessDefinitionSeedProperties.class)
@Slf4j
public class BpmProcessDefinitionSeedRunner implements ApplicationRunner {

    @Resource
    private BpmProcessDefinitionSeedProperties seedProperties;

    @Resource
    private BpmProcessDefinitionSeedService seedService;

    @Override
    public void run(ApplicationArguments args) {
        if (CollUtil.isEmpty(seedProperties.getTenantIds()) || CollUtil.isEmpty(seedProperties.getDefinitions())) {
            log.warn("[run][已开启流程定义播种，但租户或流程定义清单为空，跳过播种]");
            return;
        }
        seedProperties.getDefinitions().forEach(definition ->
                seedProperties.getTenantIds().forEach(tenantId -> {
                    try {
                        seedService.seed(definition.getKey(), definition.getName(), tenantId,
                                definition.getCandidateStrategy(), definition.getCandidateParam());
                    } catch (Exception ex) {
                        // 播种失败不阻断应用启动：它可幂等重复执行，修正配置后重启即可补齐
                        log.error("[run][租户({}) 播种流程定义({}) 失败]", tenantId, definition.getKey(), ex);
                    }
                }));
    }

}
