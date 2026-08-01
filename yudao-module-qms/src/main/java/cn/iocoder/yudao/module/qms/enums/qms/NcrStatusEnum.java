package cn.iocoder.yudao.module.qms.enums.qms;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS NCR 状态枚举
 *
 * @author 芋道源码
 */
@RequiredArgsConstructor
@Getter
public enum NcrStatusEnum implements ArrayValuable<Integer> {

    OPEN(10, "待处理"),
    MRB_REVIEW(20, "MRB 评审中"),
    DISPOSITIONED(30, "已处置"),
    CLOSED(40, "已关闭"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(NcrStatusEnum::getStatus).toArray(Integer[]::new);

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
