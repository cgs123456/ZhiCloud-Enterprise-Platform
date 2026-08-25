package cn.zhicloud.module.qms.enums.qms;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS CAPA 状态枚举
 *
 * @author 智云
 */
@RequiredArgsConstructor
@Getter
public enum CAPAStatusEnum implements ArrayValuable<Integer> {

    OPEN(10, "待处理"),
    IN_PROGRESS(20, "处理中"),
    CLOSED(30, "已关闭"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(CAPAStatusEnum::getStatus).toArray(Integer[]::new);

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
