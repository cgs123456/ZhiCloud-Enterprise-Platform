package cn.zhicloud.module.mes.enums.pro;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * MES 返工工单状态枚举
 *
 * 状态转换图：
 * <pre>
 * PENDING ──开工──▶ REWORKING ──完工──▶ COMPLETED
 *    │                  │
 *    └──────────取消──────────────────────▶ CANCELED
 * </pre>
 *
 * @author 智云
 */
@Getter
@AllArgsConstructor
public enum MesProReworkStatusEnum implements ArrayValuable<Integer> {

    /**
     * 待返工
     */
    PENDING(10, "待返工"),
    /**
     * 返工中
     */
    REWORKING(20, "返工中"),
    /**
     * 已完成
     */
    COMPLETED(30, "已完成"),
    /**
     * 已取消
     */
    CANCELED(40, "已取消");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(MesProReworkStatusEnum::getStatus).toArray(Integer[]::new);

    /**
     * 状态值
     */
    private final Integer status;
    /**
     * 状态名
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
