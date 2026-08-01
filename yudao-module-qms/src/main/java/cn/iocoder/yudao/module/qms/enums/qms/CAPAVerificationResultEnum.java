package cn.iocoder.yudao.module.qms.enums.qms;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS CAPA 有效性验证结果枚举（P0-4）
 *
 * <p>用于 CAPA 阶段流转到 {@link CAPAStageEnum#VERIFICATION} 后的验证结果记录：
 * <ul>
 *   <li>{@link #PENDING}：待验证（验证阶段初始状态）</li>
 *   <li>{@link #PASSED}：验证通过（措施有效，可关闭）</li>
 *   <li>{@link #FAILED}：验证不通过（措施无效，需重新进入纠正/预防措施阶段）</li>
 * </ul>
 *
 * @author 芋道源码
 */
@RequiredArgsConstructor
@Getter
public enum CAPAVerificationResultEnum implements ArrayValuable<Integer> {

    PENDING(10, "待验证"),
    PASSED(20, "通过"),
    FAILED(30, "不通过");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(CAPAVerificationResultEnum::getResult).toArray(Integer[]::new);

    /**
     * 验证结果
     */
    private final Integer result;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
