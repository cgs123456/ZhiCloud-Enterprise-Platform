package cn.zhicloud.module.qms.enums.qms;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS MSA 研究状态枚举
 *
 * @author 智云
 */
@RequiredArgsConstructor
@Getter
public enum MsaStatusEnum implements ArrayValuable<Integer> {

    DRAFT(10, "草稿"),
    IN_PROGRESS(20, "进行中"),
    COMPLETED(30, "已完成"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(MsaStatusEnum::getStatus).toArray(Integer[]::new);

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
