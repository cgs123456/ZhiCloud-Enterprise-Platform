package cn.iocoder.yudao.module.crm.enums.clue;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * CRM 线索渠道类型枚举
 *
 * @author dhb52
 */
@RequiredArgsConstructor
@Getter
public enum CrmClueChannelTypeEnum implements ArrayValuable<Integer> {

    OFFICIAL(10, "官网"),
    ADVERTISEMENT(20, "广告"),
    WEWORK(30, "企微"),
    MINI_PROGRAM(40, "小程序"),
    API(50, "API"),
    OFFLINE(60, "线下");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(CrmClueChannelTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
