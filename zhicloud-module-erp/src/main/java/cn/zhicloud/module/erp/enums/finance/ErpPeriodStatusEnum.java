package cn.zhicloud.module.erp.enums.finance;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP 会计期间状态枚举（P0-6）
 *
 * <p>状态流转：OPEN → CLOSING → CLOSED（不可逆）。
 * 仅允许对 OPEN 期间执行月末检查、调汇、损益结转。
 *
 * @author 智云
 */
@RequiredArgsConstructor
@Getter
public enum ErpPeriodStatusEnum implements ArrayValuable<Integer> {

    OPEN(10, "开放"),
    CLOSING(20, "结账中"),
    CLOSED(30, "已关账");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpPeriodStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
