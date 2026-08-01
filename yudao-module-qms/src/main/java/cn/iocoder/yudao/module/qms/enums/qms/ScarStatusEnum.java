package cn.iocoder.yudao.module.qms.enums.qms;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS SCAR（供应商纠正措施请求）状态枚举
 *
 * @author yudao
 */
@RequiredArgsConstructor
@Getter
public enum ScarStatusEnum implements ArrayValuable<Integer> {

    OPEN(10, "已发起"),
    IN_PROGRESS(20, "处理中"),
    CLOSED(30, "已关闭"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(ScarStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}