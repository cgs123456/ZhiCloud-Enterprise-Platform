package cn.iocoder.yudao.module.erp.enums.finance;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * ERP 固定资产状态枚举
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum ErpFixedAssetStatusEnum implements ArrayValuable<Integer> {

    /**
     * 在用
     */
    IN_USE(10, "在用"),
    /**
     * 闲置
     */
    IDLE(20, "闲置"),
    /**
     * 已处置
     */
    DISPOSED(30, "已处置"),
    /**
     * 已报废
     */
    SCRAPPED(40, "已报废");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpFixedAssetStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
