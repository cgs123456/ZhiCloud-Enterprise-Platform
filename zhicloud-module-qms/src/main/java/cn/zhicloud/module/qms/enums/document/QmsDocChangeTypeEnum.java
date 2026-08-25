package cn.zhicloud.module.qms.enums.document;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS 文件变更类型枚举
 *
 * @author 智云
 */
@RequiredArgsConstructor
@Getter
public enum QmsDocChangeTypeEnum implements ArrayValuable<Integer> {

    CREATE(10, "新增"),
    REVISE(20, "修订"),
    OBSOLETE(30, "作废"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(QmsDocChangeTypeEnum::getChangeType).toArray(Integer[]::new);

    /**
     * 变更类型
     */
    private final Integer changeType;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
