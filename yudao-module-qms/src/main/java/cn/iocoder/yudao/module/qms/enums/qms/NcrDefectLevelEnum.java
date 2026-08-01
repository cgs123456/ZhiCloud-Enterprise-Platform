package cn.iocoder.yudao.module.qms.enums.qms;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS NCR 缺陷等级枚举
 *
 * @author 芋道源码
 */
@RequiredArgsConstructor
@Getter
public enum NcrDefectLevelEnum implements ArrayValuable<Integer> {

    CRITICAL(10, "致命缺陷"),
    MAJOR(20, "严重缺陷"),
    MINOR(30, "轻微缺陷"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(NcrDefectLevelEnum::getDefectLevel).toArray(Integer[]::new);

    /**
     * 缺陷等级
     */
    private final Integer defectLevel;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
