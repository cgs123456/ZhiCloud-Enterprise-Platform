package cn.zhicloud.module.aimultiagent.service.react;

import java.util.List;

/**
 * ReAct Agent 执行结果
 *
 * @param finalAnswer     最终答案（LLM 输出 Final Answer 时填充）
 * @param steps           执行步骤列表（按时间顺序）
 * @param totalTokenUsage 总 Token 消耗
 * @param success         是否成功（true 表示 LLM 输出了 Final Answer）
 * @param errorMessage    错误信息（失败时填充）
 *
 * @author zhicloud
 */
public record ReActResult(
        String finalAnswer,
        List<ReActStep> steps,
        Integer totalTokenUsage,
        boolean success,
        String errorMessage
) {

    /**
     * 构造失败结果
     *
     * @param errorMessage 错误信息
     * @param steps        已执行的步骤
     * @param totalTokens  已消耗的 Token
     * @return 失败结果
     */
    public static ReActResult failure(String errorMessage, List<ReActStep> steps, int totalTokens) {
        return new ReActResult(null, steps, totalTokens, false, errorMessage);
    }

    /**
     * 构造成功结果
     *
     * @param finalAnswer 最终答案
     * @param steps       执行步骤
     * @param totalTokens 总 Token 消耗
     * @return 成功结果
     */
    public static ReActResult success(String finalAnswer, List<ReActStep> steps, int totalTokens) {
        return new ReActResult(finalAnswer, steps, totalTokens, true, null);
    }

}
