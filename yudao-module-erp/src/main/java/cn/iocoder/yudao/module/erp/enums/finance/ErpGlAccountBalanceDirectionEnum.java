package cn.iocoder.yudao.module.erp.enums.finance;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP 会计科目余额方向枚举（P0-7）
 *
 * <p>借方科目：资产、费用类，余额在借方（正数表示增加）。
 * <p>贷方科目：负债、权益、收入类，余额在贷方（正数表示增加）。
 * <p>共同科目：双向余额（如清算科目）。
 *
 * @author 芋道源码
 */
@RequiredArgsConstructor
@Getter
public enum ErpGlAccountBalanceDirectionEnum implements ArrayValuable<Integer> {

    DEBIT(10, "借方"),
    CREDIT(20, "贷方"),
    BOTH(30, "双向");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpGlAccountBalanceDirectionEnum::getDirection).toArray(Integer[]::new);

    private final Integer direction;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
