package cn.zhicloud.module.qms.enums.instrument;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS 计量器具状态枚举
 *
 * @author 智云
 */
@RequiredArgsConstructor
@Getter
public enum QmsInstrumentStatusEnum implements ArrayValuable<Integer> {

    IN_USE(10, "在用"),
    DISABLED(20, "停用"),
    SCRAPPED(30, "报废"),
    SEALED(40, "封存"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(QmsInstrumentStatusEnum::getStatus).toArray(Integer[]::new);

    /**
     * 状态
     */
    private final Integer status;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
