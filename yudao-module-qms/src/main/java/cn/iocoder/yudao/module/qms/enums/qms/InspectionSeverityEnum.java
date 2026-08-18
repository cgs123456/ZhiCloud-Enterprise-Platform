package cn.iocoder.yudao.module.qms.enums.qms;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS 检验缺陷严重度枚举
 *
 * <p>严重度决定 AQL 判定权重：CRITICAL（致命缺陷）触发一票否决，MAJOR/MINOR 参与 Ac/Re 计数。
 *
 * @author 智云
 */
@RequiredArgsConstructor
@Getter
public enum InspectionSeverityEnum implements ArrayValuable<Integer> {

    CRITICAL(10, "致命"),
    MAJOR(20, "严重"),
    MINOR(30, "轻微"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(InspectionSeverityEnum::getSeverity).toArray(Integer[]::new);

    /**
     * 严重度
     */
    private final Integer severity;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
