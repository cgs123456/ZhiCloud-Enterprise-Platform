package cn.iocoder.yudao.module.qms.enums.audit;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS 审核类型枚举
 *
 * @author 芋道源码
 */
@RequiredArgsConstructor
@Getter
public enum QmsAuditTypeEnum implements ArrayValuable<Integer> {

    INTERNAL(10, "内审"),
    EXTERNAL(20, "外审"),
    SECOND_PARTY(30, "第二方审核"),
    SYSTEM(40, "体系审核"),
    PROCESS(50, "过程审核"),
    PRODUCT(60, "产品审核"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(QmsAuditTypeEnum::getAuditType).toArray(Integer[]::new);

    /**
     * 审核类型
     */
    private final Integer auditType;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
