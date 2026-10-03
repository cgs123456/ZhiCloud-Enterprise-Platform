package cn.zhicloud.module.bpm.service.definition;

/**
 * 流程定义播种（Seed）Service 接口
 * <p>
 * 业务模块（例如 OA 报销、HR 请假）通过流程定义 KEY 发起审批，但仓库内不带流程定义。
 * 本接口按「租户 + KEY」补齐一个最小的、可发起的流程定义，无需人工进入管理后台配置。
 *
 * @author 智云
 */
public interface BpmProcessDefinitionSeedService {

    /**
     * 为指定租户播种流程定义，审批节点默认使用「发起人自己」作为候选人策略
     *
     * @param processDefinitionKey 流程定义标识，例如说 oa_reimburse
     * @param name                 流程名称
     * @param tenantId             租户编号
     * @return 该租户下可用的（已存在或新播种的）激活流程定义编号
     */
    String seed(String processDefinitionKey, String name, Long tenantId);

    /**
     * 为指定租户播种流程定义，并指定审批节点的候选人策略
     *
     * @param processDefinitionKey 流程定义标识
     * @param name                 流程名称
     * @param tenantId             租户编号
     * @param candidateStrategy    候选人策略，枚举 {@link cn.zhicloud.module.bpm.framework.flowable.core.enums.BpmTaskCandidateStrategyEnum}
     * @param candidateParam       候选人策略的可选参数，例如说指定用户编号
     * @return 该租户下可用的（已存在或新播种的）激活流程定义编号
     */
    String seed(String processDefinitionKey, String name, Long tenantId,
                Integer candidateStrategy, String candidateParam);

}
