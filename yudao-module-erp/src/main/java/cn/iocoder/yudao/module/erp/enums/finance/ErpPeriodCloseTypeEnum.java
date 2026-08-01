package cn.iocoder.yudao.module.erp.enums.finance;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP 期末处理类型枚举（P0-6）
 *
 * <p>三类期末处理，需按顺序执行：
 * <ol>
 *   <li>{@link #MONTH_CHECK} 月末检查：统计未审核单据、未付款采购单、未收款销售单</li>
 *   <li>{@link #REVALUATION} 调汇：根据汇率调整外币账户余额（如有外币业务）</li>
 *   <li>{@link #PROFIT_LOSS_TRANSFER} 损益结转：汇总收入/支出，计算本期净利润</li>
 * </ol>
 *
 * @author 芋道源码
 */
@RequiredArgsConstructor
@Getter
public enum ErpPeriodCloseTypeEnum implements ArrayValuable<Integer> {

    MONTH_CHECK(10, "月末检查"),
    REVALUATION(20, "调汇"),
    PROFIT_LOSS_TRANSFER(30, "损益结转");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpPeriodCloseTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
