package cn.zhicloud.module.qms.enums.qms;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS 培训计划状态枚举
 *
 * @author zhicloud
 */
@RequiredArgsConstructor
@Getter
public enum TrainingPlanStatusEnum implements ArrayValuable<Integer> {

    DRAFT(10, "草稿"),
    SCHEDULED(20, "已安排"),
    IN_PROGRESS(30, "进行中"),
    COMPLETED(40, "已完成"),
    CANCELED(50, "已取消"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(TrainingPlanStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}