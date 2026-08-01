package cn.iocoder.yudao.module.qms.enums.qms;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS CAPA 阶段枚举（P0-4）
 *
 * <p>CAPA 全流程按以下阶段顺序流转，每阶段对应一组必填字段：
 * <ul>
 *   <li>{@link #CREATED}：已创建（问题描述已完成，等待根本原因分析）</li>
 *   <li>{@link #ROOT_CAUSE_ANALYSIS}：根本原因分析（填写 rootCauseAnalysis）</li>
 *   <li>{@link #CORRECTIVE_ACTION}：纠正措施（填写 correctiveAction）</li>
 *   <li>{@link #PREVENTIVE_ACTION}：预防措施（填写 preventiveAction）</li>
 *   <li>{@link #VERIFICATION}：有效性验证（等待验证结果）</li>
 *   <li>{@link #CLOSED}：已关闭（验证通过后关闭）</li>
 * </ul>
 *
 * <p>状态机：CREATED → ROOT_CAUSE_ANALYSIS → CORRECTIVE_ACTION → PREVENTIVE_ACTION
 * → VERIFICATION → CLOSED。每一步前进不可逆，但允许回退到上一步（纠错）。
 *
 * @author 芋道源码
 */
@RequiredArgsConstructor
@Getter
public enum CAPAStageEnum implements ArrayValuable<Integer> {

    CREATED(10, "已创建"),
    ROOT_CAUSE_ANALYSIS(20, "根本原因分析"),
    CORRECTIVE_ACTION(30, "纠正措施"),
    PREVENTIVE_ACTION(40, "预防措施"),
    VERIFICATION(50, "有效性验证"),
    CLOSED(60, "已关闭");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(CAPAStageEnum::getStage).toArray(Integer[]::new);

    /**
     * 阶段
     */
    private final Integer stage;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    /**
     * 判断是否可以从前一阶段流转到当前阶段
     *
     * @param fromStage 当前阶段
     * @param toStage 目标阶段
     * @return 是否允许流转
     */
    public static boolean canTransition(Integer fromStage, Integer toStage) {
        if (fromStage == null || toStage == null) {
            return false;
        }
        // 允许前进 1 步或后退 1 步
        int diff = toStage - fromStage;
        return diff == 10 || diff == -10;
    }

}
