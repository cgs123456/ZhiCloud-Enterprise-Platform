package cn.iocoder.yudao.module.qms.enums.audit;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS 审核结论枚举
 *
 * @author 芋道源码
 */
@RequiredArgsConstructor
@Getter
public enum QmsAuditConclusionEnum implements ArrayValuable<Integer> {

    CONFORM(10, "符合"),
    BASICALLY_CONFORM(20, "基本符合"),
    NON_CONFORM(30, "不符合"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(QmsAuditConclusionEnum::getConclusion).toArray(Integer[]::new);

    /**
     * 结论
     */
    private final Integer conclusion;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
