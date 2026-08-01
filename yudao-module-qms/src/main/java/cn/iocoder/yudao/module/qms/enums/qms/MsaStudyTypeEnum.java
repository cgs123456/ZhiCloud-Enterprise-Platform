package cn.iocoder.yudao.module.qms.enums.qms;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS MSA 研究类型枚举
 *
 * @author 芋道源码
 */
@RequiredArgsConstructor
@Getter
public enum MsaStudyTypeEnum implements ArrayValuable<Integer> {

    GAGE_RR(10, "Gage R&R 类型 I 和 II"),
    ATTRIBUTE(20, "属性型"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(MsaStudyTypeEnum::getStudyType).toArray(Integer[]::new);

    /**
     * 类型
     */
    private final Integer studyType;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
