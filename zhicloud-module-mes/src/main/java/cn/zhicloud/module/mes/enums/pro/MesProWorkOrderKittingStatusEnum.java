package cn.zhicloud.module.mes.enums.pro;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * MES 工单齐套状态枚举（P0-5）
 *
 * <p>用于齐套分析结果中每条 BOM 行的齐套判定：
 * <ul>
 *   <li>{@link #FULLY_KITTED}：齐套（库存 + 在途 ≥ 需求量）</li>
 *   <li>{@link #PARTIAL}：部分齐套（库存 &lt; 需求量，但库存 + 在途 ≥ 需求量）</li>
 *   <li>{@link #SHORTAGE}：短缺（库存 + 在途 &lt; 需求量）</li>
 * </ul>
 *
 * @author zhicloud
 */
@Getter
@AllArgsConstructor
public enum MesProWorkOrderKittingStatusEnum implements ArrayValuable<Integer> {

    /**
     * 齐套：当前库存即可满足需求
     */
    FULLY_KITTED(1, "齐套"),
    /**
     * 部分齐套：库存不足，但加上在途可以满足
     */
    PARTIAL(2, "部分齐套"),
    /**
     * 短缺：库存 + 在途仍不足以满足需求
     */
    SHORTAGE(3, "短缺");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(MesProWorkOrderKittingStatusEnum::getStatus).toArray(Integer[]::new);

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
