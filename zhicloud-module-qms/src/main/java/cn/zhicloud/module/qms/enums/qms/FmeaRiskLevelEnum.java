package cn.zhicloud.module.qms.enums.qms;

import lombok.Getter;

/**
 * QMS FMEA 风险等级枚举
 *
 * <p>风险等级判定规则：
 * <ul>
 *   <li>RPN >= 200 高风险（红色）</li>
 *   <li>100 <= RPN < 200 中风险（黄色）</li>
 *   <li>RPN < 100 低风险（绿色）</li>
 *   <li>S/O/D 任一为 10 时，无论 RPN 多少均标记为高风险</li>
 * </ul>
 *
 * @author 智云
 */
@Getter
public enum FmeaRiskLevelEnum {

    HIGH("高风险", "red"),
    MEDIUM("中风险", "yellow"),
    LOW("低风险", "green"),
    ;

    /**
     * 名称
     */
    private final String name;
    /**
     * 颜色
     */
    private final String color;

    FmeaRiskLevelEnum(String name, String color) {
        this.name = name;
        this.color = color;
    }

    /**
     * 根据 RPN 判定风险等级（不考虑 S/O/D 为 10 的情况）
     *
     * @param rpn 风险优先数
     * @return 风险等级
     */
    public static FmeaRiskLevelEnum of(int rpn) {
        if (rpn >= 200) {
            return HIGH;
        }
        if (rpn >= 100) {
            return MEDIUM;
        }
        return LOW;
    }

}
