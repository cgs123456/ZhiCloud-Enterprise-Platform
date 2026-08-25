package cn.zhicloud.module.bpm.enums.definition;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * BPM 流程监听器的类型
 *
 * @author 智云
 */
@Getter
@AllArgsConstructor
public enum BpmProcessListenerTypeEnum {

    EXECUTION("execution", "执行监听器"),
    TASK("task", "任务执行器");

    private final String type;
    private final String name;

}
