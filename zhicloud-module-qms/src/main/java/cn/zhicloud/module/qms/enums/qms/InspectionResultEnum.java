package cn.zhicloud.module.qms.enums.qms;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS 检验结果枚举
 *
 * @author 智云
 */
@RequiredArgsConstructor
@Getter
public enum InspectionResultEnum implements ArrayValuable<Integer> {

    PASS(10, "合格"),
    FAIL(20, "不合格"),
    NA(30, "不适用"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(InspectionResultEnum::getResult).toArray(Integer[]::new);

    /**
     * 结果
     */
    private final Integer result;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
