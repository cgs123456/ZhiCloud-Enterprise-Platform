package cn.iocoder.yudao.module.erp.enums.finance;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * ERP 合并报表抵消类型枚举
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum ErpConsolidationEliminationTypeEnum implements ArrayValuable<Integer> {

    /**
     * 投资与权益抵消（母公司投资 ↔ 子公司所有者权益）
     */
    INVESTMENT_EQUITY(10, "投资权益抵消"),
    /**
     * 内部应收应付抵消
     */
    INTERCOMPANY_AR_AP(20, "内部应收应付抵消"),
    /**
     * 内部销售收入与成本抵消
     */
    INTERCOMPANY_SALE_COGS(30, "内部销售成本抵消"),
    /**
     * 内部固定资产交易未实现利润抵消
     */
    INTERCOMPANY_FA(40, "内部固定资产抵消");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpConsolidationEliminationTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
