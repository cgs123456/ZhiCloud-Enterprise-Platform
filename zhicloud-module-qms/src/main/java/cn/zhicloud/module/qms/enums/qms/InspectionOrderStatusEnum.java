package cn.zhicloud.module.qms.enums.qms;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS 检验单状态枚举
 *
 * @author 智云
 */
@RequiredArgsConstructor
@Getter
public enum InspectionOrderStatusEnum implements ArrayValuable<Integer> {

    PENDING(10, "待检验"),
    INSPECTING(20, "检验中"),
    PASSED(30, "检验通过"),
    FAILED(40, "检验不通过"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(InspectionOrderStatusEnum::getStatus).toArray(Integer[]::new);

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
