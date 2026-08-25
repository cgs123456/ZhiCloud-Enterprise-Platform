package cn.zhicloud.module.ai.enums.prompt;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * AI Prompt 模板分类的枚举
 *
 * @author zhicloud
 */
@Getter
@RequiredArgsConstructor
public enum AiPromptTemplateCategoryEnum implements ArrayValuable<String> {

    SYSTEM("SYSTEM", "系统提示"),
    CHAT("CHAT", "对话"),
    RAG("RAG", "RAG 检索增强"),
    AGENT("AGENT", "智能体"),
    TRANSLATION("TRANSLATION", "翻译"),
    SUMMARIZATION("SUMMARIZATION", "摘要总结"),
    CODE("CODE", "代码生成");

    /**
     * 分类
     */
    private final String category;
    /**
     * 分类名
     */
    private final String name;

    public static final String[] ARRAYS = Arrays.stream(values()).map(AiPromptTemplateCategoryEnum::getCategory).toArray(String[]::new);

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
