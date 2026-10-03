package cn.zhicloud.module.bpm.framework.flowable.config;

import cn.zhicloud.module.bpm.framework.flowable.core.enums.BpmTaskCandidateStrategyEnum;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程定义播种（Seed）配置项
 * <p>
 * 默认关闭：只有显式配置 {@code zhicloud.bpm.process-definition-seed.enabled=true} 且给出租户和流程定义清单，
 * 应用启动时才会补齐流程定义，避免每个环境的每次启动都悄悄部署流程。
 *
 * @author 智云
 */
@ConfigurationProperties(prefix = "zhicloud.bpm.process-definition-seed")
@Data
public class BpmProcessDefinitionSeedProperties {

    /**
     * 是否开启启动时的流程定义播种
     */
    private Boolean enabled = false;

    /**
     * 需要播种的租户编号数组
     */
    private List<Long> tenantIds = new ArrayList<>();

    /**
     * 需要播种的流程定义清单
     */
    private List<Definition> definitions = new ArrayList<>();

    /**
     * 单个流程定义的播种配置
     */
    @Data
    public static class Definition {

        /**
         * 流程定义标识，需要和业务模块发起审批使用的 KEY 保持一致，例如说 oa_reimburse
         */
        private String key;

        /**
         * 流程名称
         */
        private String name;

        /**
         * 审批节点的候选人策略，默认「发起人自己」
         * <p>
         * 枚举 {@link BpmTaskCandidateStrategyEnum}
         */
        private Integer candidateStrategy = BpmTaskCandidateStrategyEnum.START_USER.getStrategy();

        /**
         * 审批节点的候选人策略参数，例如说 USER 策略下的用户编号
         */
        private String candidateParam;

    }

}
