package cn.iocoder.yudao.module.hr.enums.salary;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * HR 薪资状态枚举
 *
 * @author yudao
 */
@RequiredArgsConstructor
@Getter
public enum HrSalaryStatusEnum implements ArrayValuable<Integer> {

    DRAFT(10, "草稿"),
    APPROVED(20, "已审核"),
    PAID(30, "已发放"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(HrSalaryStatusEnum::getStatus).toArray(Integer[]::new);

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