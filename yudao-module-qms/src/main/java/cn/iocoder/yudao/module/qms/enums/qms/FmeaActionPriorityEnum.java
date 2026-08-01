package cn.iocoder.yudao.module.qms.enums.qms;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * FMEA 行动优先级枚举（AIAG-VDA 2019 版）
 *
 * <p>替代传统 RPN 阈值判定，基于 S/O/D 组合查表得到行动优先级：
 * <ul>
 *   <li>HIGH：高优先级，必须采取行动降低风险或进行风险沟通</li>
 *   <li>MEDIUM：中优先级，应采取行动，必要时进行风险沟通</li>
 *   <li>LOW：低优先级，可采取行动，保留记录</li>
 * </ul>
 *
 * @author yudao
 */
@Getter
@AllArgsConstructor
public enum FmeaActionPriorityEnum {

    HIGH("HIGH", "高优先级"),
    MEDIUM("MEDIUM", "中优先级"),
    LOW("LOW", "低优先级");

    /**
     * 优先级代码
     */
    private final String code;
    /**
     * 描述
     */
    private final String description;

}
