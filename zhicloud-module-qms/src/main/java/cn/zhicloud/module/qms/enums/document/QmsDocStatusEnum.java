package cn.zhicloud.module.qms.enums.document;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS 受控文档状态枚举
 *
 * @author 智云
 */
@RequiredArgsConstructor
@Getter
public enum QmsDocStatusEnum implements ArrayValuable<Integer> {

    DRAFT(10, "草稿"),
    PENDING_APPROVAL(20, "待审"),
    PUBLISHED(30, "已发布"),
    OBSOLETE(40, "已作废"),
    RETURNED(50, "已回收"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(QmsDocStatusEnum::getStatus).toArray(Integer[]::new);

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
