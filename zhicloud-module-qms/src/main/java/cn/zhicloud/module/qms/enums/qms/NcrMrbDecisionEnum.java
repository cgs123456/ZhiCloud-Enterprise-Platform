package cn.zhicloud.module.qms.enums.qms;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS NCR MRB 物料评审委员会决议枚举
 *
 * @author 智云
 */
@RequiredArgsConstructor
@Getter
public enum NcrMrbDecisionEnum implements ArrayValuable<Integer> {

    ACCEPT_REWORK(10, "同意返工"),
    ACCEPT_REPAIR(20, "同意返修"),
    ACCEPT_DEGRADE(30, "同意降级"),
    SCRAP(40, "报废"),
    USE_AS_IS(50, "让步接收"),
    RETURN(60, "退货"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(NcrMrbDecisionEnum::getDecision).toArray(Integer[]::new);

    /**
     * 决议
     */
    private final Integer decision;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
