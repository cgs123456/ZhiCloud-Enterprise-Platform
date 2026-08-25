package cn.zhicloud.module.hr.enums.contract;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@RequiredArgsConstructor
@Getter
public enum HrContractTypeEnum implements ArrayValuable<Integer> {

    FIXED_TERM(1, "固定期限"),
    NON_FIXED_TERM(2, "无固定期限"),
    TASK_COMPLETION(3, "完成任务"),
    INTERNSHIP(4, "实习"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(HrContractTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}