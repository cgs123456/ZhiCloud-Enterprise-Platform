package cn.iocoder.yudao.module.qms.enums.audit;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS 审核计划状态枚举
 *
 * @author 芋道源码
 */
@RequiredArgsConstructor
@Getter
public enum QmsAuditPlanStatusEnum implements ArrayValuable<Integer> {

    PLANNED(10, "已计划"),
    EXECUTING(20, "已执行"),
    COMPLETED(30, "已完成"),
    CANCELLED(40, "已取消"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(QmsAuditPlanStatusEnum::getStatus).toArray(Integer[]::new);

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
