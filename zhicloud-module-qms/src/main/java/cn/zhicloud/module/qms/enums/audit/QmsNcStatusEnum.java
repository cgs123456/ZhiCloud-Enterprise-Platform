package cn.zhicloud.module.qms.enums.audit;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS 不符合项状态枚举
 *
 * @author 智云
 */
@RequiredArgsConstructor
@Getter
public enum QmsNcStatusEnum implements ArrayValuable<Integer> {

    PENDING(10, "待整改"),
    RECTIFYING(20, "整改中"),
    RECTIFIED(30, "已整改"),
    VERIFIED(40, "已验证"),
    CLOSED(50, "已关闭"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(QmsNcStatusEnum::getStatus).toArray(Integer[]::new);

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
