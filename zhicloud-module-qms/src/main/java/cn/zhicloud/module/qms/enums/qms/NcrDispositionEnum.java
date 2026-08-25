package cn.zhicloud.module.qms.enums.qms;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS NCR 处置方式枚举
 *
 * @author 智云
 */
@RequiredArgsConstructor
@Getter
public enum NcrDispositionEnum implements ArrayValuable<Integer> {

    REWORK(10, "返工"),
    REPAIR(20, "返修"),
    DEGRADE(30, "降级"),
    SCRAP(40, "报废"),
    USE_AS_IS(50, "让步接收"),
    RETURN(60, "退货"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(NcrDispositionEnum::getDisposition).toArray(Integer[]::new);

    /**
     * 处置方式
     */
    private final Integer disposition;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
