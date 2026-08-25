package cn.zhicloud.module.qms.enums.qms;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS FMEA 文档状态枚举
 *
 * @author 智云
 */
@RequiredArgsConstructor
@Getter
public enum FmeaStatusEnum implements ArrayValuable<Integer> {

    DRAFT(10, "草稿"),
    REVIEWED(20, "已评审"),
    APPROVED(30, "已批准"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(FmeaStatusEnum::getStatus).toArray(Integer[]::new);

    /**
     * 状态
     */
    private final Integer status;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
