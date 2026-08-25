package cn.zhicloud.module.mes.enums.pro;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * MES APS 排产算法枚举
 *
 * @author 智云
 */
@Getter
@AllArgsConstructor
public enum ApsAlgorithmEnum implements ArrayValuable<String> {

    GREEDY("GREEDY", "贪婪算法"),
    TOC_DBR("TOC_DBR", "TOC-DBR 约束理论"),
    GENETIC("GENETIC", "遗传算法");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(ApsAlgorithmEnum::getAlgorithm).toArray(String[]::new);

    /**
     * 算法标识
     */
    private final String algorithm;
    /**
     * 名称
     */
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }

    /**
     * 按字符串解析算法枚举，未匹配时回退到贪婪算法
     */
    public static ApsAlgorithmEnum parse(String value) {
        if (value == null || value.isEmpty()) {
            return GREEDY;
        }
        for (ApsAlgorithmEnum algo : values()) {
            if (algo.getAlgorithm().equalsIgnoreCase(value)) {
                return algo;
            }
        }
        return GREEDY;
    }

}