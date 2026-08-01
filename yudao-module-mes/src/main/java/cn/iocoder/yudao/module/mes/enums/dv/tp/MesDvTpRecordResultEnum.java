package cn.iocoder.yudao.module.mes.enums.dv.tp;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * MES TPM 执行记录结果枚举
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum MesDvTpRecordResultEnum implements ArrayValuable<Integer> {

    QUALIFIED(10, "合格"),
    ABNORMAL(20, "异常"),
    NOT_EXECUTED(30, "未执行");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(MesDvTpRecordResultEnum::getResult).toArray(Integer[]::new);

    /**
     * 结果值
     */
    private final Integer result;
    /**
     * 结果名
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}