package cn.iocoder.yudao.module.qms.enums.qms;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * SPC 控制图类型枚举
 *
 * <p>参考 ISO 7870 / GB/T 17989：
 * <ul>
 *   <li>计量型（连续数据）：XBAR-R / XBAR-S / I-MR</li>
 *   <li>计数型（离散数据）：P / NP / C / U</li>
 * </ul>
 *
 * @author yudao
 */
@Getter
@AllArgsConstructor
public enum SpcChartTypeEnum {

    XBAR_R("XBAR_R", "均值-极差图（子组 2~10）"),
    XBAR_S("XBAR_S", "均值-标准差图（子组 >10）"),
    I_MR("I_MR", "单值-移动极差图（子组=1）"),
    P("P", "不合格品率图（样本大小可变）"),
    NP("NP", "不合格品数图（样本大小固定）"),
    C("C", "缺陷数图（样本大小固定）"),
    U("U", "单位缺陷数图（样本大小可变）");

    /**
     * 图表类型代码
     */
    private final String code;
    /**
     * 描述
     */
    private final String description;

}
