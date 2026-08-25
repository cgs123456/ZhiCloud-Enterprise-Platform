package cn.zhicloud.module.qms.enums.document;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS 受控文档类型枚举
 *
 * @author 智云
 */
@RequiredArgsConstructor
@Getter
public enum QmsDocTypeEnum implements ArrayValuable<Integer> {

    QUALITY_MANUAL(10, "质量手册"),
    PROCEDURE(20, "程序文件"),
    WORK_INSTRUCTION(30, "作业指导书"),
    QUALITY_RECORD(40, "质量记录"),
    EXTERNAL_DOCUMENT(50, "外来文件"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(QmsDocTypeEnum::getDocType).toArray(Integer[]::new);

    /**
     * 文档类型
     */
    private final Integer docType;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
