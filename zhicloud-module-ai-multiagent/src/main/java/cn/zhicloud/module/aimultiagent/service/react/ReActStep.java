package cn.zhicloud.module.aimultiagent.service.react;

/**
 * ReAct 单步执行记录
 *
 * 记录一次 Thought → Action → Observation 循环的完整信息。
 *
 * @param thought     LLM 的思考内容
 * @param action      动作名称（工具名或 "Final Answer"）
 * @param actionInput 动作输入（工具参数 JSON 字符串或最终答案文本）
 * @param observation 工具执行结果（Observation），最终答案步骤为 null
 * @param tokenUsage  本步消耗的 Token 数
 *
 * @author zhicloud
 */
public record ReActStep(
        String thought,
        String action,
        String actionInput,
        String observation,
        Integer tokenUsage
) {
}
