package cn.iocoder.yudao.module.erp.enums.finance;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP 期末处理执行状态枚举（P0-6）
 *
 * @author 芋道源码
 */
@RequiredArgsConstructor
@Getter
public enum ErpPeriodCloseStatusEnum implements ArrayValuable<Integer> {

    SUCCESS(10, "成功"),
    SKIPPED(20, "跳过"),
    FAILED(30, "失败");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpPeriodCloseStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
