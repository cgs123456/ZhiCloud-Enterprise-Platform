package cn.zhicloud.module.qms.enums.qms;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS 客户投诉状态枚举
 *
 * @author zhicloud
 */
@RequiredArgsConstructor
@Getter
public enum ComplaintStatusEnum implements ArrayValuable<Integer> {

    REGISTERED(10, "已登记"),
    INVESTIGATING(20, "调查中"),
    HANDLING(30, "处理中"),
    CLOSED(40, "已关闭"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(ComplaintStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}