package cn.zhicloud.module.qms.enums.qms;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS 客户投诉处理方式枚举
 *
 * @author zhicloud
 */
@RequiredArgsConstructor
@Getter
public enum ComplaintHandleTypeEnum implements ArrayValuable<Integer> {

    RETURN(10, "退货"),
    EXCHANGE(20, "换货"),
    COMPENSATION(30, "赔偿"),
    CORRECTIVE(40, "纠正"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(ComplaintHandleTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}