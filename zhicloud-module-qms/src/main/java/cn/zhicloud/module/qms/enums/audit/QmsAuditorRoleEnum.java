package cn.zhicloud.module.qms.enums.audit;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS 审核员角色枚举
 *
 * @author 智云
 */
@RequiredArgsConstructor
@Getter
public enum QmsAuditorRoleEnum implements ArrayValuable<Integer> {

    LEAD_AUDITOR(10, "主审"),
    AUDITOR(20, "组员"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(QmsAuditorRoleEnum::getRole).toArray(Integer[]::new);

    /**
     * 角色
     */
    private final Integer role;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
