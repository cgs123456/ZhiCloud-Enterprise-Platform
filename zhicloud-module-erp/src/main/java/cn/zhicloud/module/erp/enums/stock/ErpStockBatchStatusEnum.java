package cn.zhicloud.module.erp.enums.stock;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP 库存批次状态枚举
 *
 * <p>状态流转：
 * <ul>
 *   <li>{@link #AVAILABLE} 可用：正常可用库存</li>
 *   <li>{@link #FROZEN} 冻结：因质检/冻结等不可用</li>
 *   <li>{@link #EXPIRED} 过期：超过有效期，不可用</li>
 * </ul>
 *
 * @author 智云
 */
@RequiredArgsConstructor
@Getter
public enum ErpStockBatchStatusEnum implements ArrayValuable<Integer> {

    AVAILABLE(10, "可用"),
    FROZEN(20, "冻结"),
    EXPIRED(30, "过期");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpStockBatchStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
