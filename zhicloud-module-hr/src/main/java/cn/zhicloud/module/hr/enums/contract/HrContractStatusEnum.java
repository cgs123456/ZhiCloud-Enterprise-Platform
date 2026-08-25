package cn.zhicloud.module.hr.enums.contract;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@RequiredArgsConstructor
@Getter
public enum HrContractStatusEnum implements ArrayValuable<Integer> {

    EFFECTIVE(0, "生效"),
    EXPIRING(1, "即将到期"),
    EXPIRED(2, "已到期"),
    TERMINATED(3, "已终止"),
    RENEWED(4, "已续签"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(HrContractStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}