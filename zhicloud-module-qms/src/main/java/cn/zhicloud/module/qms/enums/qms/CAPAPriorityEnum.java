package cn.zhicloud.module.qms.enums.qms;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS CAPA 优先级枚举（P0-4）
 *
 * <p>用于 CAPA 文档的优先级分类：
 * <ul>
 *   <li>{@link #HIGH}：高优先级，需立即处理（如影响交付/客户投诉/安全风险）</li>
 *   <li>{@link #MEDIUM}：中优先级，需在截止日期前完成</li>
 *   <li>{@link #LOW}：低优先级，可在常规节奏中处理</li>
 * </ul>
 *
 * @author 智云
 */
@RequiredArgsConstructor
@Getter
public enum CAPAPriorityEnum implements ArrayValuable<Integer> {

    HIGH(10, "高"),
    MEDIUM(20, "中"),
    LOW(30, "低");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(CAPAPriorityEnum::getPriority).toArray(Integer[]::new);

    /**
     * 优先级
     */
    private final Integer priority;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
