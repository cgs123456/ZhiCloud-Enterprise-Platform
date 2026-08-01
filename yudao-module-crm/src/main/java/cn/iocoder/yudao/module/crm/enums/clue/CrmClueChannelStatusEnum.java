package cn.iocoder.yudao.module.crm.enums.clue;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * CRM 线索渠道状态枚举
 *
 * @author dhb52
 */
@RequiredArgsConstructor
@Getter
public enum CrmClueChannelStatusEnum implements ArrayValuable<Integer> {

    ENABLE(10, "启用"),
    DISABLE(20, "停用");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(CrmClueChannelStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
