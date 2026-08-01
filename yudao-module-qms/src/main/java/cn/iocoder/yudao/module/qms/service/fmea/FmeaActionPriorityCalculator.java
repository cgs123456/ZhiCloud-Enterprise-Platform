package cn.iocoder.yudao.module.qms.service.fmea;

import cn.iocoder.yudao.module.qms.enums.qms.FmeaActionPriorityEnum;

/**
 * FMEA AIAG-VDA 2019 行动优先级（AP）查表计算器
 *
 * <p>替代 RPN 阈值判定，按 S（严重度）× O（频度）× D（探测度）三维组合查表，
 * 返回 HIGH / MEDIUM / LOW 三级行动优先级。
 *
 * <p>查表规则（AIAG-VDA 1st Edition, 2019）：
 * <ul>
 *   <li>S 高（9-10）：O ≥ 4 → H；O 2-3 → H（D≤3）/ M（D≥4）；O 1 → M</li>
 *   <li>S 中高（7-8）：O ≥ 6 → H；O 4-5 → M；O 2-3 → M；O 1 → L</li>
 *   <li>S 中低（4-6）：O ≥ 8 → H；O 6-7 → M；O 4-5 → M；O 2-3 → L；O 1 → L</li>
 *   <li>S 低（1-3）：O ≥ 6 → M；O 4-5 → L；O 2-3 → L；O 1 → L</li>
 * </ul>
 *
 * @author yudao
 */
public final class FmeaActionPriorityCalculator {

    private FmeaActionPriorityCalculator() {
    }

    /**
     * 根据 S/O/D 计算行动优先级
     *
     * @param severity  严重度（1-10）
     * @param occurrence 频度（1-10）
     * @param detection  探测度（1-10）
     * @return 行动优先级枚举，参数非法时返回 LOW
     */
    public static FmeaActionPriorityEnum calculate(int severity, int occurrence, int detection) {
        if (!isValidSod(severity) || !isValidSod(occurrence) || !isValidSod(detection)) {
            return FmeaActionPriorityEnum.LOW;
        }
        // 按 S 分层查表
        if (severity >= 9) {
            // S 高（9-10）
            if (occurrence >= 4) {
                return FmeaActionPriorityEnum.HIGH;
            }
            if (occurrence >= 2) {
                return detection <= 3 ? FmeaActionPriorityEnum.HIGH : FmeaActionPriorityEnum.MEDIUM;
            }
            return FmeaActionPriorityEnum.MEDIUM;
        }
        if (severity >= 7) {
            // S 中高（7-8）
            if (occurrence >= 6) {
                return FmeaActionPriorityEnum.HIGH;
            }
            if (occurrence >= 2) {
                return FmeaActionPriorityEnum.MEDIUM;
            }
            return FmeaActionPriorityEnum.LOW;
        }
        if (severity >= 4) {
            // S 中低（4-6）
            if (occurrence >= 8) {
                return FmeaActionPriorityEnum.HIGH;
            }
            if (occurrence >= 4) {
                return FmeaActionPriorityEnum.MEDIUM;
            }
            return FmeaActionPriorityEnum.LOW;
        }
        // S 低（1-3）
        if (occurrence >= 6) {
            return FmeaActionPriorityEnum.MEDIUM;
        }
        return FmeaActionPriorityEnum.LOW;
    }

    private static boolean isValidSod(int value) {
        return value >= 1 && value <= 10;
    }

}
