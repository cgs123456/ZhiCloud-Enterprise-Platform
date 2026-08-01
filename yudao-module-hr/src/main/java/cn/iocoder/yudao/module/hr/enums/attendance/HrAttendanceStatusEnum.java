package cn.iocoder.yudao.module.hr.enums.attendance;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * HR 考勤状态枚举
 *
 * @author yudao
 */
@RequiredArgsConstructor
@Getter
public enum HrAttendanceStatusEnum implements ArrayValuable<Integer> {

    NORMAL(10, "正常"),
    LATE(20, "迟到"),
    EARLY_LEAVE(30, "早退"),
    ABSENT(40, "缺勤"),
    OVERTIME(50, "加班"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(HrAttendanceStatusEnum::getStatus).toArray(Integer[]::new);

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