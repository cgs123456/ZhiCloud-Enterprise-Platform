package cn.zhicloud.module.qms.enums.qms;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS 供应商审核状态枚举
 *
 * @author zhicloud
 */
@RequiredArgsConstructor
@Getter
public enum SupplierAuditStatusEnum implements ArrayValuable<Integer> {

    PLANNED(10, "已计划"),
    IN_PROGRESS(20, "审核中"),
    COMPLETED(30, "已完成"),
    CANCELED(40, "已取消"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(SupplierAuditStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}