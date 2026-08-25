package cn.zhicloud.module.qms.enums.qms;

import cn.zhicloud.framework.common.core.ArrayValuable;
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
 * → VERIFICATION → CLOSED。常规流转为前进 1 步或回退 1 步（纠错）。
 *
 * <p>额外允许一条跨步回退路径：{@link #VERIFICATION} → {@link #CORRECTIVE_ACTION}。
 * 该路径对应 ISO 9001 中「有效性验证不通过 → 退回重新制定纠正措施」的返工闭环，
 * 流转时会由 Service 清空 verificationResult / verificationComment / verifiedBy / verifiedTime。
 *
 * @author 智云
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
     * 判断是否可以从当前阶段流转到目标阶段
     *
     * <p>规则：
     * <ol>
     *   <li>常规：前进 1 步或后退 1 步</li>
     *   <li>例外：{@link #VERIFICATION} → {@link #CORRECTIVE_ACTION}（验证不通过返工，跨 2 步回退）</li>
     * </ol>
     *
     * @param fromStage 当前阶段
     * @param toStage 目标阶段
     * @return 是否允许流转
     */
    public static boolean canTransition(Integer fromStage, Integer toStage) {
        if (fromStage == null || toStage == null) {
            return false;
        }
        // 例外：有效性验证不通过，退回纠正措施阶段重新返工
        if (VERIFICATION.getStage().equals(fromStage) && CORRECTIVE_ACTION.getStage().equals(toStage)) {
            return true;
        }
        // 常规：允许前进 1 步或后退 1 步
        int diff = toStage - fromStage;
        return diff == 10 || diff == -10;
    }

}
