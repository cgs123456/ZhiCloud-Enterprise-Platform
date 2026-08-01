package cn.iocoder.yudao.module.qms.enums.qms;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS 岗位资格状态枚举
 *
 * @author yudao
 */
@RequiredArgsConstructor
@Getter
public enum QualificationStatusEnum implements ArrayValuable<Integer> {

    VALID(10, "有效"),
    EXPIRING(20, "即将到期"),
    EXPIRED(30, "已到期"),
    REVOKED(40, "已撤销"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(QualificationStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}