package cn.iocoder.yudao.module.qms.enums.qms;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * SPC Western Electric 8 条失控规则枚举
 *
 * <p>参考 Western Electric Handbook / Nelson Rules，用于识别控制图上的非随机模式。
 *
 * @author yudao
 */
@Getter
@AllArgsConstructor
public enum WesternElectricRuleEnum {

    RULE_1(1, "1 点落在 3σ 之外（A 区外）"),
    RULE_2(2, "连续 9 点落在中心线同一侧"),
    RULE_3(3, "连续 6 点递增或递减"),
    RULE_4(4, "连续 14 点交替上下"),
    RULE_5(5, "连续 3 点中有 2 点落在 A 区（同侧 2σ~3σ）"),
    RULE_6(6, "连续 5 点中有 4 点落在 B 区之外（同侧 >1σ）"),
    RULE_7(7, "连续 15 点落在 C 区内（≤1σ）"),
    RULE_8(8, "连续 8 点落在 C 区之外（两侧 >1σ）");

    /**
     * 规则编号
     */
    private final int code;
    /**
     * 规则描述
     */
    private final String description;

}
