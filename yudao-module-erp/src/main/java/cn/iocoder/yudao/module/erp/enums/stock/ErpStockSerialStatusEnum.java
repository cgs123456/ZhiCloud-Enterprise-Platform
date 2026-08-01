package cn.iocoder.yudao.module.erp.enums.stock;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP 库存序列号状态枚举
 *
 * <p>状态流转：
 * <ul>
 *   <li>{@link #INSTOCK} 在库：序列号对应的产品在库存中</li>
 *   <li>{@link #OUTSTOCK} 出库：序列号对应的产品已出库</li>
 * </ul>
 *
 * @author 芋道源码
 */
@RequiredArgsConstructor
@Getter
public enum ErpStockSerialStatusEnum implements ArrayValuable<Integer> {

    INSTOCK(10, "在库"),
    OUTSTOCK(20, "出库");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpStockSerialStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
