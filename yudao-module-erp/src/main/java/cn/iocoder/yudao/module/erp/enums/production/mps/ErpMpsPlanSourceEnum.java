package cn.iocoder.yudao.module.erp.enums.production.mps;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * ERP 主生产计划来源枚举
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum ErpMpsPlanSourceEnum implements ArrayValuable<Integer> {

    SALE_ORDER(10, "销售订单"),
    FORECAST(20, "预测"),
    SAFETY_STOCK(30, "安全库存");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(ErpMpsPlanSourceEnum::getSource).toArray(Integer[]::new);

    /**
     * 来源值
     */
    private final Integer source;
    /**
     * 来源名
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}