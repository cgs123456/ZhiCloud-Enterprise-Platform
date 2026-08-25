package cn.zhicloud.module.qms.enums.document;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS 文件变更申请状态枚举
 *
 * @author 智云
 */
@RequiredArgsConstructor
@Getter
public enum QmsChangeRequestStatusEnum implements ArrayValuable<Integer> {

    PENDING(10, "待审"),
    APPROVED(20, "已审"),
    REJECTED(30, "已驳回"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(QmsChangeRequestStatusEnum::getStatus).toArray(Integer[]::new);

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
