package cn.iocoder.yudao.module.mes.enums.pro;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * MES 返工类型枚举
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum MesProReworkTypeEnum implements ArrayValuable<Integer> {

    PRODUCTION(10, "生产返工"),
    INCOMING_DEFECTIVE(20, "来料不良"),
    CUSTOMER_RETURN(30, "客户退货返工");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(MesProReworkTypeEnum::getType).toArray(Integer[]::new);

    /**
     * 类型值
     */
    private final Integer type;
    /**
     * 类型名
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
