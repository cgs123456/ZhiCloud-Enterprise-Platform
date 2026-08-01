package cn.iocoder.yudao.module.hr.enums.leave;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@RequiredArgsConstructor
@Getter
public enum HrLeaveStatusEnum implements ArrayValuable<Integer> {

    PENDING(0, "待审批"),
    APPROVED(1, "已批准"),
    REJECTED(2, "已驳回"),
    CANCELLED(3, "已撤销"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(HrLeaveStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}