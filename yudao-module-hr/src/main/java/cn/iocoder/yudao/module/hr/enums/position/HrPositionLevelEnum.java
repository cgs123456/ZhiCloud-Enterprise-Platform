package cn.iocoder.yudao.module.hr.enums.position;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * HR 职位级别枚举
 *
 * @author yudao
 */
@RequiredArgsConstructor
@Getter
public enum HrPositionLevelEnum implements ArrayValuable<Integer> {

    JUNIOR(10, "初级"),
    MIDDLE(20, "中级"),
    SENIOR(30, "高级"),
    EXPERT(40, "专家"),
    MANAGER(50, "管理"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(HrPositionLevelEnum::getLevel).toArray(Integer[]::new);

    /**
     * 级别
     */
    private final Integer level;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}