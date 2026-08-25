package cn.zhicloud.module.ai.service.workflow;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import cn.zhicloud.module.ai.controller.admin.workflow.vo.AiWorkflowNodeTypeRespVO;
import cn.zhicloud.module.ai.controller.admin.workflow.vo.AiWorkflowPageReqVO;
import cn.zhicloud.module.ai.controller.admin.workflow.vo.AiWorkflowSaveReqVO;
import cn.zhicloud.module.ai.controller.admin.workflow.vo.AiWorkflowTemplateRespVO;
import cn.zhicloud.module.ai.controller.admin.workflow.vo.AiWorkflowTestReqVO;
import cn.zhicloud.module.ai.controller.admin.workflow.vo.AiWorkflowValidateReqVO;
import cn.zhicloud.module.ai.controller.admin.workflow.vo.AiWorkflowValidateRespVO;
import cn.zhicloud.module.ai.dal.dataobject.model.AiToolDO;
import cn.zhicloud.module.ai.dal.dataobject.workflow.AiWorkflowDO;
import cn.zhicloud.module.ai.dal.mysql.workflow.AiWorkflowMapper;
import cn.zhicloud.module.ai.service.knowledge.AiKnowledgeSegmentService;
import cn.zhicloud.module.ai.service.knowledge.bo.AiKnowledgeSegmentSearchReqBO;
import cn.zhicloud.module.ai.service.knowledge.bo.AiKnowledgeSegmentSearchRespBO;
import cn.zhicloud.module.ai.service.model.AiModelService;
import cn.zhicloud.module.ai.service.model.AiToolService;
import cn.zhicloud.module.ai.framework.ai.security.AiCallGuard;
import cn.zhicloud.module.ai.framework.ai.security.AiContextPropagator;
import cn.zhicloud.module.ai.framework.ai.security.SsrfGuard;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import dev.tinyflow.core.Tinyflow;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.ai.enums.ErrorCodeConstants.WORKFLOW_CODE_EXISTS;
import static cn.zhicloud.module.ai.enums.ErrorCodeConstants.WORKFLOW_NOT_EXISTS;

/**
 * AI 工作流 Service 实现类
 *
 * <h3>工作流执行引擎兼容性说明（SubTask 10.4）</h3>
 * <pre>
 * 1. TinyFlow 1.2.6 在 JDK 21 虚拟线程下的行为：
 *    - TinyFlow 内部通过 {@code tinyflow.toChain().executeForResult(variables)} 同步执行工作流链，
 *      不自带异步线程池，执行线程由调用方（即当前请求线程）决定。
 *    - 本项目已在 application.yaml 开启 {@code spring.threads.virtual.enabled=true}，Controller
 *      层请求会运行在虚拟线程上，因此 TinyFlow 的整条链式执行也会跑在虚拟线程中。
 *    - 虚拟线程适用于 IO 密集型（LLM 调用、向量检索、HTTP 工具）场景，可大幅提升并发；
 *      但若节点内存在 synchronized 长时间持有或 CPU 密集计算，应使用 {@code Thread.ofPlatform()}
 *      切换到平台线程，避免 pinning（钉住）问题。
 *
 * 2. Spring AI 1.1.8 下的兼容性：
 *    - TinyFlow 默认使用 agents-flex 的 Llm 接口（如 QwenLlm / OllamaLlm），
 *      与 Spring AI 的 ChatModel 是两套独立体系，本服务通过 {@link AiModelService#getLLmProvider4Tinyflow}
 *      将 zhicloud 的模型配置转换为 agents-flex Llm Provider 注入 TinyFlow。
 *    - 后续如需统一到 Spring AI ChatClient，需实现一个 SpringAiLlm 适配器（agents-flex Llm 接口）
 *      包裹 Spring AI ChatModel，再通过 {@code tinyflow.setLlmProvider} 注入。
 *
 * 3. 潜在的 ThreadLocal 传递问题：
 *    - 多租户上下文（TenantContext）、用户上下文（SecurityContext）依赖 ThreadLocal 传递。
 *    - TinyFlow 的 Llm Provider 以 lambda 形式注册，实际调用发生在同一请求线程内，ThreadLocal 可正常传递。
 *    - 若未来引入异步执行（CompletableFuture / 响应式），必须使用
 *      {@code TransmittableThreadLocal} 或手动在异步任务前后复制 ThreadLocal 上下文，
 *      否则租户隔离、权限校验将失效。
 *    - RAG 检索节点（调用 VectorStore）、Tool 调用节点（调用 @Tool 方法）若涉及多租户数据，
 *      同样需要确保 TenantContext 在调用链路中正确传递。
 * </pre>
 *
 * @author lesan
 */
@Service
@Slf4j
public class AiWorkflowServiceImpl implements AiWorkflowService {

    @Resource
    private AiWorkflowMapper workflowMapper;

    @Resource
    private AiModelService apiModelService;

    // ========== C8 修复：ragNode/toolNode/multiAgentNode 三节点依赖注入 ==========

    @Resource
    private AiKnowledgeSegmentService knowledgeSegmentService;

    @Resource
    private AiToolService toolService;

    @Resource
    private ToolCallbackResolver toolCallbackResolver;

    /**
     * Multi-Agent 执行器 SPI 网关
     *
     * <p>通过 SPI 模式解耦：zhicloud-module-ai 定义接口，zhicloud-module-ai-multiagent 提供实现。
     * 当 zhicloud-module-ai-multiagent 模块未启用时，此 Bean 不存在，multiAgentNode 节点降级为日志告警。
     */
    @Autowired(required = false)
    private MultiAgentExecutorGateway multiAgentExecutorGateway;

    // ========== P1 AI Workflow 保护组件注入 ==========

    @Resource
    private AiCallGuard aiCallGuard;
    @Resource
    private AiContextPropagator aiContextPropagator;
    @Resource
    private SsrfGuard ssrfGuard;

    @Override
    public Long createWorkflow(AiWorkflowSaveReqVO createReqVO) {
        // 1. 参数校验
        validateCodeUnique(null, createReqVO.getCode());

        // 2. 插入工作流配置
        AiWorkflowDO workflow = BeanUtils.toBean(createReqVO, AiWorkflowDO.class);
        workflowMapper.insert(workflow);
        return workflow.getId();
    }

    @Override
    public void updateWorkflow(AiWorkflowSaveReqVO updateReqVO) {
        // 1. 参数校验
        validateWorkflowExists(updateReqVO.getId());
        validateCodeUnique(updateReqVO.getId(), updateReqVO.getCode());

        // 2. 更新工作流配置
        AiWorkflowDO workflow = BeanUtils.toBean(updateReqVO, AiWorkflowDO.class);
        workflowMapper.updateById(workflow);
    }

    @Override
    public void deleteWorkflow(Long id) {
        // 1. 校验存在
        validateWorkflowExists(id);

        // 2. 删除工作流配置
        workflowMapper.deleteById(id);
    }

    private AiWorkflowDO validateWorkflowExists(Long id) {
        if (ObjUtil.isNull(id)) {
            throw exception(WORKFLOW_NOT_EXISTS);
        }
        AiWorkflowDO workflow = workflowMapper.selectById(id);
        if (ObjUtil.isNull(workflow)) {
            throw exception(WORKFLOW_NOT_EXISTS);
        }
        return workflow;
    }

    private void validateCodeUnique(Long id, String code) {
        if (StrUtil.isBlank(code)) {
            return;
        }
        AiWorkflowDO workflow = workflowMapper.selectByCode(code);
        if (ObjUtil.isNull(workflow)) {
            return;
        }
        if (ObjUtil.isNull(id)) {
            throw exception(WORKFLOW_CODE_EXISTS);
        }
        if (ObjUtil.notEqual(workflow.getId(), id)) {
            throw exception(WORKFLOW_CODE_EXISTS);
        }
    }

    @Override
    public AiWorkflowDO getWorkflow(Long id) {
        return workflowMapper.selectById(id);
    }

    @Override
    public PageResult<AiWorkflowDO> getWorkflowPage(AiWorkflowPageReqVO pageReqVO) {
        return workflowMapper.selectPage(pageReqVO);
    }

    @Override
    public Object testWorkflow(AiWorkflowTestReqVO testReqVO) {
        // 加载 graph
        String graph = testReqVO.getGraph() != null ? testReqVO.getGraph()
                : validateWorkflowExists(testReqVO.getId()).getGraph();

        // 提取 variables：先初始化，确保 parseFlowParam 可将预计算结果合并进来
        Map<String, Object> variables = testReqVO.getParams();
        if (variables == null) {
            variables = new HashMap<>();
        }

        // 构建 TinyFlow 执行链，并预执行 ragNode/toolNode/multiAgentNode（C8 修复）
        Tinyflow tinyflow = parseFlowParam(graph, variables);

        // 执行
        // 说明：executeForResult 同步执行，线程为当前请求线程（虚拟线程下为 virtual thread）。
        return tinyflow.toChain().executeForResult(variables);
    }

    /**
     * 解析工作流 graph，配置 Tinyflow，并预执行 ragNode/toolNode/multiAgentNode 三类节点。
     *
     * <h3>预执行 + 变量注入模式（C8 修复）</h3>
     * <p>TinyFlow 内置不包含 RAG / Spring AI Tool / Multi-Agent 节点处理器，因此采用
     * "预执行 + 变量注入" 模式：在 TinyFlow 执行前，先同步执行这三类节点的实际能力，
     * 将结果以 nodeId 为 key 存入 precomputedResults，最终合并到 variables 中。
     * 下游节点（如 llmNode 的 prompt 模板）可通过变量引用使用这些预计算结果。
     *
     * <p>异常处理：三类节点的预执行均用 try-catch 包裹，异常时降级为 log.warn，不阻断主流程。
     *
     * @param graph     工作流 graph JSON
     * @param variables 执行变量（会被 precomputedResults 合并更新）
     * @return 配置好的 Tinyflow 实例
     */
    private Tinyflow parseFlowParam(String graph, Map<String, Object> variables) {
        // TODO @lesan：可以使用 jackson 哇？
        JSONObject json = JSONObject.parseObject(graph);
        JSONArray nodeArr = json.getJSONArray("nodes");
        Tinyflow tinyflow = new Tinyflow(json.toJSONString());
        // C8 修复：预计算结果 map，key=节点 id，value=该节点产出（如 context/result/documents）
        Map<String, Object> precomputedResults = new HashMap<>();
        for (int i = 0; i < nodeArr.size(); i++) {
            JSONObject node = nodeArr.getJSONObject(i);
            String nodeType = node.getString("type");
            JSONObject data = node.getJSONObject("data");
            String nodeId = node.getString("id");
            switch (nodeType) {
                case "llmNode":
                    // LLM 调用节点：根据 llmId 注入对应的 agents-flex Llm Provider
                    apiModelService.getLLmProvider4Tinyflow(tinyflow, data.getLong("llmId"));
                    break;
                case "ragNode":
                    // RAG 检索节点：调用 AiKnowledgeSegmentService 进行相似度检索（C8 修复）
                    log.info("[parseFlowParam][ragNode 节点 knowledgeId={}]", data.getLong("knowledgeId"));
                    try {
                        Long knowledgeId = data.getLong("knowledgeId");
                        if (knowledgeId != null) {
                            // 构造 RAG 检索请求
                            AiKnowledgeSegmentSearchReqBO reqBO = new AiKnowledgeSegmentSearchReqBO();
                            reqBO.setKnowledgeId(knowledgeId);
                            // query 优先取节点配置，未配置时取 variables 中的 message（startNode 输出）
                            String query = data.getString("query");
                            if (StrUtil.isBlank(query)) {
                                query = variables != null ? (String) variables.getOrDefault("message", "") : "";
                            }
                            reqBO.setContent(query);
                            if (data.getInteger("topK") != null) {
                                reqBO.setTopK(data.getInteger("topK"));
                            }
                            if (data.getDouble("similarityThreshold") != null) {
                                reqBO.setSimilarityThreshold(data.getDouble("similarityThreshold"));
                            }
                            // P1: wrapped with AiCallGuard for timeout and circuit breaker protection
                            Map<String, Object> ragResult = aiCallGuard.callWithGuard(() -> {
                                List<AiKnowledgeSegmentSearchRespBO> segments = knowledgeSegmentService.searchKnowledgeSegment(reqBO);
                                String context = segments.stream()
                                        .map(AiKnowledgeSegmentSearchRespBO::getContent)
                                        .collect(Collectors.joining("\n\n"));
                                Map<String, Object> result = new HashMap<>();
                                result.put("documents", segments);
                                result.put("context", context);
                                log.info("[parseFlowParam][ragNode search returned {} segments]", segments.size());
                                return result;
                            });
                            precomputedResults.put(nodeId, ragResult);
                        }
                    } catch (Exception ex) {
                        log.warn("[parseFlowParam][ragNode 检索失败 knowledgeId={} err={}]",
                                data.getLong("knowledgeId"), ex.toString());
                    }
                    break;
                case "toolNode":
                    // Tool 调用节点：通过 ToolCallbackResolver 调用 Spring AI @Tool 方法（C8 修复）
                    log.info("[parseFlowParam][toolNode 节点 toolId={}]", data.getLong("toolId"));
                    try {
                        Long toolId = data.getLong("toolId");
                        if (toolId != null) {
                            AiToolDO tool = toolService.getTool(toolId);
                            if (tool != null) {
                                ToolCallback callback = toolCallbackResolver.resolve(tool.getName());
                                if (callback != null) {
                                    // params JSON 序列化为字符串
                                    String toolInput = data.getJSONObject("params") != null
                                            ? data.getJSONObject("params").toJSONString() : "{}";
                                    // P1: wrapped with AiCallGuard for timeout and circuit breaker protection
                                    String result = aiCallGuard.callWithGuard(() -> callback.call(toolInput));
                                    Map<String, Object> toolResult = new HashMap<>();
                                    toolResult.put("result", result);
                                    precomputedResults.put(nodeId, toolResult);
                                    log.info("[parseFlowParam][toolNode 工具调用成功 tool={} result={}]",
                                            tool.getName(), result);
                                }
                            }
                        }
                    } catch (Exception ex) {
                        log.warn("[parseFlowParam][toolNode 调用失败 toolId={} err={}]",
                                data.getLong("toolId"), ex.toString());
                    }
                    break;
                case "multiAgentNode":
                    // Multi-Agent 子流程节点：通过 SPI 网关调用多 Agent 编排（C8 修复）
                    log.info("[parseFlowParam][multiAgentNode 节点 agentConfig={}]", data.getString("agentConfig"));
                    try {
                        if (multiAgentExecutorGateway != null) {
                            // 从 agentConfig 解析 topologyId（agentConfig 为 JSON 字符串）
                            String agentConfigStr = data.getString("agentConfig");
                            Long topologyId = null;
                            if (StrUtil.isNotBlank(agentConfigStr)) {
                                JSONObject agentConfig = JSONObject.parseObject(agentConfigStr);
                                topologyId = agentConfig.getLong("topologyId");
                            }
                            if (topologyId != null) {
                                // task 优先取节点配置，未配置时取 variables 中的 message
                                String task = data.getString("task");
                                if (StrUtil.isBlank(task)) {
                                    task = variables != null ? (String) variables.getOrDefault("message", "") : "";
                                }
                                // P1: wrapped with AiContextPropagator (tenant context) and AiCallGuard (timeout/circuit breaker)
                                final Long finalTopologyId = topologyId;
                                final String finalTask = task;
                                String result = aiCallGuard.callWithGuard(
                                    aiContextPropagator.wrap(() -> {
                                        Long tenantId = TenantContextHolder.getTenantId();
                                        return multiAgentExecutorGateway.execute(finalTopologyId, finalTask, tenantId);
                                    })
                                );
                                Map<String, Object> agentResult = new HashMap<>();
                                agentResult.put("result", result);
                                precomputedResults.put(nodeId, agentResult);
                                log.info("[parseFlowParam][multiAgentNode 执行成功 topologyId={}]", topologyId);
                            }
                        } else {
                            log.warn("[parseFlowParam][multiAgentNode MultiAgentExecutorGateway 未注入，跳过执行]");
                        }
                    } catch (Exception ex) {
                        log.warn("[parseFlowParam][multiAgentNode 执行失败 err={}]", ex.toString());
                    }
                    break;
                case "internalNode":
                    break;
                default:
                    break;
            }
        }
        // C8 修复：将预计算结果合并到 variables，供 TinyFlow 执行时下游节点通过变量引用
        if (variables != null) {
            variables.putAll(precomputedResults);
        }
        return tinyflow;
    }

    @Override
    public List<AiWorkflowNodeTypeRespVO> getNodeTypeList() {
        return buildNodeTypes();
    }

    @Override
    public AiWorkflowNodeTypeRespVO getNodeConfigSchema(String nodeType) {
        return buildNodeTypes().stream()
                .filter(item -> ObjUtil.equal(item.getNodeType(), nodeType))
                .findFirst()
                .orElse(null);
    }

    @Override
    public AiWorkflowValidateRespVO validateWorkflow(AiWorkflowValidateReqVO reqVO) {
        AiWorkflowValidateRespVO respVO = new AiWorkflowValidateRespVO();
        List<String> errors = new ArrayList<>();
        try {
            JSONObject graph = JSONObject.parseObject(reqVO.getGraph());
            JSONArray nodes = graph.getJSONArray("nodes");
            if (nodes == null || nodes.isEmpty()) {
                errors.add("工作流节点列表（nodes）不能为空");
            } else {
                List<String> validTypes = buildNodeTypes().stream()
                        .map(AiWorkflowNodeTypeRespVO::getNodeType).toList();
                for (int i = 0; i < nodes.size(); i++) {
                    JSONObject node = nodes.getJSONObject(i);
                    String type = node.getString("type");
                    String id = node.getString("id");
                    // 校验节点类型合法性
                    if (!validTypes.contains(type)) {
                        errors.add(StrUtil.format("节点 [{}] 的类型 [{}] 不支持", id, type));
                        continue;
                    }
                    // 校验节点配置必填项
                    JSONObject data = node.getJSONObject("data");
                    if (data == null) {
                        data = new JSONObject();
                    }
                    validateNodeConfig(type, id, data, errors);
                }
            }
            // 校验连线（edges）
            JSONArray edges = graph.getJSONArray("edges");
            if (edges != null && !edges.isEmpty() && nodes != null && !nodes.isEmpty()) {
                List<String> nodeIds = new ArrayList<>();
                for (int i = 0; i < nodes.size(); i++) {
                    nodeIds.add(nodes.getJSONObject(i).getString("id"));
                }
                for (int i = 0; i < edges.size(); i++) {
                    JSONObject edge = edges.getJSONObject(i);
                    String source = edge.getString("source");
                    String target = edge.getString("target");
                    if (!nodeIds.contains(source)) {
                        errors.add(StrUtil.format("连线 [{}] 的 source 节点 [{}] 不存在", i, source));
                    }
                    if (!nodeIds.contains(target)) {
                        errors.add(StrUtil.format("连线 [{}] 的 target 节点 [{}] 不存在", i, target));
                    }
                }
            }
        } catch (Exception e) {
            errors.add("工作流模型 JSON 解析失败：" + e.getMessage());
        }
        respVO.setValid(errors.isEmpty());
        respVO.setErrors(errors);
        return respVO;
    }

    /**
     * 校验单个节点的配置必填项
     */
    private void validateNodeConfig(String type, String id, JSONObject data, List<String> errors) {
        switch (type) {
            case "llmNode":
                if (data.getLong("llmId") == null) {
                    errors.add(StrUtil.format("LLM 节点 [{}] 缺少 llmId 配置", id));
                }
                break;
            case "ragNode":
                if (data.getLong("knowledgeId") == null) {
                    errors.add(StrUtil.format("RAG 检索节点 [{}] 缺少 knowledgeId 配置", id));
                }
                break;
            case "toolNode":
                if (data.getLong("toolId") == null) {
                    errors.add(StrUtil.format("Tool 调用节点 [{}] 缺少 toolId 配置", id));
                }
                break;
            case "multiAgentNode":
                if (StrUtil.isBlank(data.getString("agentConfig"))) {
                    errors.add(StrUtil.format("Multi-Agent 节点 [{}] 缺少 agentConfig 配置", id));
                }
                break;
            default:
                break;
        }
    }

    @Override
    public List<AiWorkflowTemplateRespVO> getWorkflowTemplateList() {
        return buildWorkflowTemplates();
    }

    // ========== 节点类型定义（前端可视化编排用） ==========

    /**
     * 构建所有可用节点类型列表
     * <p>
     * 分类说明：
     * - flow：流程控制节点（开始、结束、条件、循环）
     * - ai：AI 能力节点（LLM 调用、RAG 检索、Tool 调用、Multi-Agent 子流程）
     * - tool：工具节点（代码执行、HTTP 请求、内部处理）
     */
    private List<AiWorkflowNodeTypeRespVO> buildNodeTypes() {
        List<AiWorkflowNodeTypeRespVO> list = new ArrayList<>();

        // ========== flow 流程控制 ==========
        list.add(buildNodeType("startNode", "开始", "flow", "工作流入口节点，接收初始输入参数",
                buildSchema(new String[][]{{"message", "string", "用户输入消息", "false"}}),
                Collections.emptyList(),
                Arrays.asList(buildPort("message", "string", "初始输入消息"))));
        list.add(buildNodeType("endNode", "结束", "flow", "工作流出口节点，输出最终结果",
                buildSchema(new String[][]{{"result", "string", "最终输出结果", "false"}}),
                Arrays.asList(buildPort("input", "object", "上游节点结果")),
                Arrays.asList(buildPort("result", "string", "最终输出结果"))));
        list.add(buildNodeType("conditionNode", "条件分支", "flow", "根据条件表达式进行分支路由",
                buildSchema(new String[][]{{"expression", "string", "条件表达式", "true"}}),
                Arrays.asList(buildPort("input", "object", "条件判断输入")),
                Arrays.asList(buildPort("true", "object", "条件成立分支"), buildPort("false", "object", "条件不成立分支"))));
        list.add(buildNodeType("loopNode", "循环", "flow", "对列表数据进行循环处理",
                buildSchema(new String[][]{{"iterable", "string", "可迭代变量名", "true"}, {"maxIterations", "number", "最大循环次数", "false"}}),
                Arrays.asList(buildPort("iterable", "array", "可迭代数据")),
                Arrays.asList(buildPort("item", "object", "当前迭代项"), buildPort("index", "number", "当前索引"))));

        // ========== ai AI 能力 ==========
        list.add(buildNodeType("llmNode", "LLM 调用", "ai", "调用大语言模型生成回复，支持选择模型、设置 temperature/maxTokens",
                buildSchema(new String[][]{
                        {"llmId", "number", "AI 模型编号", "true"},
                        {"prompt", "string", "提示词模板（支持 ${var} 变量）", "true"},
                        {"temperature", "number", "温度参数（0-2）", "false"},
                        {"maxTokens", "number", "最大 Token 数", "false"}
                }),
                Arrays.asList(buildPort("message", "string", "用户消息"), buildPort("context", "object", "上下文变量")),
                Arrays.asList(buildPort("result", "string", "LLM 生成结果"))));
        list.add(buildNodeType("ragNode", "RAG 检索", "ai", "调用 VectorStore 进行相似度检索，召回知识库相关文档片段",
                buildSchema(new String[][]{
                        {"knowledgeId", "number", "知识库编号", "true"},
                        {"query", "string", "检索查询语句（支持 ${var} 变量）", "true"},
                        {"topK", "number", "召回数量", "false"},
                        {"similarityThreshold", "number", "相似度阈值（0-1）", "false"}
                }),
                Arrays.asList(buildPort("query", "string", "检索查询语句")),
                Arrays.asList(buildPort("documents", "array", "召回的文档片段列表"), buildPort("context", "string", "拼接后的上下文文本"))));
        list.add(buildNodeType("toolNode", "Tool 调用", "ai", "调用 @Tool 注解的方法，执行业务工具（如库存查询、运费计算等）",
                buildSchema(new String[][]{
                        {"toolId", "number", "AI 工具编号", "true"},
                        {"params", "object", "工具调用参数（JSON）", "false"}
                }),
                Arrays.asList(buildPort("params", "object", "工具调用参数")),
                Arrays.asList(buildPort("result", "object", "工具返回结果"))));
        list.add(buildNodeType("multiAgentNode", "Multi-Agent 子流程", "ai", "调用多 Agent 编排（与 zhicloud-module-ai-multiagent 集成），由 Supervisor 分发任务给 Worker Agent",
                buildSchema(new String[][]{
                        {"agentConfig", "string", "Agent 编排配置 JSON（拓扑、工具绑定）", "true"},
                        {"task", "string", "子流程任务描述（支持 ${var} 变量）", "true"},
                        {"maxDepth", "number", "调用深度上限", "false"},
                        {"tokenBudget", "number", "Token 预算上限", "false"}
                }),
                Arrays.asList(buildPort("task", "string", "子流程任务描述")),
                Arrays.asList(buildPort("result", "string", "Agent 编排最终结果"))));

        // ========== tool 工具 ==========
        list.add(buildNodeType("codeNode", "代码执行", "tool", "执行自定义代码片段（JavaScript/Groovy）",
                buildSchema(new String[][]{{"code", "string", "代码内容", "true"}, {"language", "string", "语言（javascript/groovy）", "false"}}),
                Arrays.asList(buildPort("input", "object", "代码输入参数")),
                Arrays.asList(buildPort("output", "object", "代码执行结果"))));
        list.add(buildNodeType("httpRequestNode", "HTTP 请求", "tool", "发起 HTTP 请求调用外部接口",
                buildSchema(new String[][]{{"url", "string", "请求 URL", "true"}, {"method", "string", "请求方法（GET/POST/PUT/DELETE）", "true"}, {"headers", "object", "请求头", "false"}, {"body", "string", "请求体", "false"}}),
                Arrays.asList(buildPort("input", "object", "请求参数")),
                Arrays.asList(buildPort("response", "object", "响应数据"))));
        list.add(buildNodeType("internalNode", "内部处理", "tool", "TinyFlow 内置处理节点，进行变量赋值、数据转换等",
                buildSchema(new String[][]{{"config", "object", "内部处理配置", "false"}}),
                Arrays.asList(buildPort("input", "object", "输入数据")),
                Arrays.asList(buildPort("output", "object", "处理后的数据"))));

        return list;
    }

    /**
     * 构建单个节点类型
     */
    private AiWorkflowNodeTypeRespVO buildNodeType(String nodeType, String name, String category,
                                                   String description, Map<String, Object> configSchema,
                                                   List<AiWorkflowNodeTypeRespVO.PortDefinition> inputs,
                                                   List<AiWorkflowNodeTypeRespVO.PortDefinition> outputs) {
        AiWorkflowNodeTypeRespVO vo = new AiWorkflowNodeTypeRespVO();
        vo.setNodeType(nodeType);
        vo.setName(name);
        vo.setCategory(category);
        vo.setDescription(description);
        vo.setConfigSchema(configSchema);
        vo.setInputs(inputs);
        vo.setOutputs(outputs);
        return vo;
    }

    /**
     * 构建端口定义
     */
    private AiWorkflowNodeTypeRespVO.PortDefinition buildPort(String name, String type, String description) {
        AiWorkflowNodeTypeRespVO.PortDefinition port = new AiWorkflowNodeTypeRespVO.PortDefinition();
        port.setName(name);
        port.setType(type);
        port.setDescription(description);
        return port;
    }

    /**
     * 构建 JSON Schema 风格的配置项定义
     *
     * @param fields 字段数组：[字段名, 类型, 描述, 是否必填]
     */
    private Map<String, Object> buildSchema(String[][] fields) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        List<Map<String, Object>> properties = new ArrayList<>();
        List<String> required = new ArrayList<>();
        for (String[] field : fields) {
            Map<String, Object> prop = new LinkedHashMap<>();
            prop.put("field", field[0]);
            prop.put("type", field[1]);
            prop.put("description", field[2]);
            prop.put("required", Boolean.parseBoolean(field[3]));
            properties.add(prop);
            if (Boolean.parseBoolean(field[3])) {
                required.add(field[0]);
            }
        }
        schema.put("properties", properties);
        schema.put("required", required);
        return schema;
    }

    // ========== 工作流模板 ==========

    /**
     * 构建工作流模板列表
     */
    private List<AiWorkflowTemplateRespVO> buildWorkflowTemplates() {
        List<AiWorkflowTemplateRespVO> list = new ArrayList<>();

        // 模板 1：LLM 问答
        AiWorkflowTemplateRespVO template1 = new AiWorkflowTemplateRespVO();
        template1.setCode("llm-qa");
        template1.setName("LLM 问答");
        template1.setDescription("最基础的 LLM 问答工作流：开始 → LLM 调用 → 结束");
        template1.setCategory("ai");
        template1.setGraph("{\"nodes\":[{\"id\":\"start\",\"type\":\"startNode\",\"data\":{\"message\":\"${message}\"}},"
                + "{\"id\":\"llm\",\"type\":\"llmNode\",\"data\":{\"llmId\":1,\"prompt\":\"${message}\"}},"
                + "{\"id\":\"end\",\"type\":\"endNode\",\"data\":{}}],"
                + "\"edges\":[{\"source\":\"start\",\"target\":\"llm\"},{\"source\":\"llm\",\"target\":\"end\"}]}");
        list.add(template1);

        // 模板 2：RAG 知识库问答
        AiWorkflowTemplateRespVO template2 = new AiWorkflowTemplateRespVO();
        template2.setCode("rag-qa");
        template2.setName("RAG 知识库问答");
        template2.setDescription("检索增强生成：开始 → RAG 检索 → LLM 调用（带上下文） → 结束");
        template2.setCategory("ai");
        template2.setGraph("{\"nodes\":[{\"id\":\"start\",\"type\":\"startNode\",\"data\":{\"message\":\"${message}\"}},"
                + "{\"id\":\"rag\",\"type\":\"ragNode\",\"data\":{\"knowledgeId\":1,\"query\":\"${message}\",\"topK\":4}},"
                + "{\"id\":\"llm\",\"type\":\"llmNode\",\"data\":{\"llmId\":1,\"prompt\":\"基于以下上下文回答问题：\\n上下文：${context}\\n问题：${message}\"}},"
                + "{\"id\":\"end\",\"type\":\"endNode\",\"data\":{}}],"
                + "\"edges\":[{\"source\":\"start\",\"target\":\"rag\"},{\"source\":\"rag\",\"target\":\"llm\"},{\"source\":\"llm\",\"target\":\"end\"}]}");
        list.add(template2);

        // 模板 3：Tool 工具调用
        AiWorkflowTemplateRespVO template3 = new AiWorkflowTemplateRespVO();
        template3.setCode("tool-call");
        template3.setName("Tool 工具调用");
        template3.setDescription("工具调用工作流：开始 → Tool 调用 → 结束");
        template3.setCategory("ai");
        template3.setGraph("{\"nodes\":[{\"id\":\"start\",\"type\":\"startNode\",\"data\":{\"message\":\"${message}\"}},"
                + "{\"id\":\"tool\",\"type\":\"toolNode\",\"data\":{\"toolId\":1,\"params\":{\"query\":\"${message}\"}}},"
                + "{\"id\":\"end\",\"type\":\"endNode\",\"data\":{}}],"
                + "\"edges\":[{\"source\":\"start\",\"target\":\"tool\"},{\"source\":\"tool\",\"target\":\"end\"}]}");
        list.add(template3);

        // 模板 4：条件分支
        AiWorkflowTemplateRespVO template4 = new AiWorkflowTemplateRespVO();
        template4.setCode("condition-branch");
        template4.setName("条件分支");
        template4.setDescription("条件分支工作流：开始 → 条件判断 → 分支处理 → 结束");
        template4.setCategory("flow");
        template4.setGraph("{\"nodes\":[{\"id\":\"start\",\"type\":\"startNode\",\"data\":{\"message\":\"${message}\"}},"
                + "{\"id\":\"condition\",\"type\":\"conditionNode\",\"data\":{\"expression\":\"${message}.contains('查询')\"}},"
                + "{\"id\":\"llm\",\"type\":\"llmNode\",\"data\":{\"llmId\":1,\"prompt\":\"${message}\"}},"
                + "{\"id\":\"end\",\"type\":\"endNode\",\"data\":{}}],"
                + "\"edges\":[{\"source\":\"start\",\"target\":\"condition\"},{\"source\":\"condition\",\"sourcePort\":\"true\",\"target\":\"llm\"},{\"source\":\"llm\",\"target\":\"end\"}]}");
        list.add(template4);

        return list;
    }

}
