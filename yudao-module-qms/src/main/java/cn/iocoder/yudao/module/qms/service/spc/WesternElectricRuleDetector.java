package cn.iocoder.yudao.module.qms.service.spc;

import cn.iocoder.yudao.module.qms.controller.admin.spc.vo.SpcRuleViolationVO;
import cn.iocoder.yudao.module.qms.enums.qms.WesternElectricRuleEnum;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * SPC Western Electric 8 条规则检测器
 *
 * <p>基于控制图中心线 (CL)、控制限 (UCL/LCL) 与样本序列，识别以下非随机模式：
 * <ul>
 *   <li>规则1：1 点落在 3σ 之外（A 区外）</li>
 *   <li>规则2：连续 9 点落在中心线同一侧</li>
 *   <li>规则3：连续 6 点递增或递减</li>
 *   <li>规则4：连续 14 点交替上下</li>
 *   <li>规则5：连续 3 点中有 2 点落在 A 区（同侧 2σ~3σ）</li>
 *   <li>规则6：连续 5 点中有 4 点落在 B 区之外（同侧 >1σ）</li>
 *   <li>规则7：连续 15 点落在 C 区内（≤1σ）</li>
 *   <li>规则8：连续 8 点落在 C 区之外（两侧 >1σ）</li>
 * </ul>
 *
 * <p>各区定义（以 σ 为单位）：
 * <ul>
 *   <li>A 区：2σ ~ 3σ（含外侧）</li>
 *   <li>B 区：1σ ~ 2σ</li>
 *   <li>C 区：0 ~ 1σ（中心线附近）</li>
 * </ul>
 *
 * @author yudao
 */
public final class WesternElectricRuleDetector {

    private WesternElectricRuleDetector() {
    }

    /**
     * 对样本序列运行全部 8 条规则检测
     *
     * @param samples 样本值序列（按时间升序）
     * @param mean    中心线 CL（样本均值）
     * @param stdDev  样本标准差 σ
     * @return 违规记录列表（空列表表示无违规）
     */
    public static List<SpcRuleViolationVO> detect(List<BigDecimal> samples, BigDecimal mean, BigDecimal stdDev) {
        List<SpcRuleViolationVO> violations = new ArrayList<>();
        if (samples == null || samples.isEmpty() || mean == null || stdDev == null
                || stdDev.compareTo(BigDecimal.ZERO) == 0) {
            return violations;
        }
        // 区间边界（以 σ 倍数表示，统一使用 BigDecimal）
        BigDecimal sigma1 = stdDev;
        BigDecimal sigma2 = stdDev.multiply(new BigDecimal("2"));
        BigDecimal sigma3 = stdDev.multiply(new BigDecimal("3"));

        rule1(violations, samples, mean, sigma3);
        rule2(violations, samples, mean);
        rule3(violations, samples);
        rule4(violations, samples);
        rule5(violations, samples, mean, sigma2, sigma3);
        rule6(violations, samples, mean, sigma1, sigma2);
        rule7(violations, samples, mean, sigma1);
        rule8(violations, samples, mean, sigma1);
        return violations;
    }

    // ============ 规则 1：1 点落在 3σ 之外 ============
    private static void rule1(List<SpcRuleViolationVO> violations, List<BigDecimal> samples,
                              BigDecimal mean, BigDecimal sigma3) {
        BigDecimal ucl = mean.add(sigma3);
        BigDecimal lcl = mean.subtract(sigma3);
        for (int i = 0; i < samples.size(); i++) {
            BigDecimal v = samples.get(i);
            if (v.compareTo(ucl) > 0 || v.compareTo(lcl) < 0) {
                violations.add(build(WesternElectricRuleEnum.RULE_1, i, i, 1));
            }
        }
    }

    // ============ 规则 2：连续 9 点落在中心线同一侧 ============
    private static void rule2(List<SpcRuleViolationVO> violations, List<BigDecimal> samples, BigDecimal mean) {
        int required = 9;
        int runLen = 0;
        Boolean side = null; // true=正侧（>mean），false=负侧（<mean）
        int startIdx = 0;
        for (int i = 0; i < samples.size(); i++) {
            BigDecimal v = samples.get(i);
            int cmp = v.compareTo(mean);
            Boolean curSide = cmp > 0 ? Boolean.TRUE : (cmp < 0 ? Boolean.FALSE : null);
            if (curSide == null) {
                // 等于均值，重置
                runLen = 0;
                side = null;
                continue;
            }
            if (side != null && side.equals(curSide)) {
                runLen++;
            } else {
                runLen = 1;
                side = curSide;
                startIdx = i;
            }
            if (runLen >= required) {
                violations.add(build(WesternElectricRuleEnum.RULE_2, startIdx, i, runLen));
                // 命中后重置，避免重复
                runLen = 0;
                side = null;
            }
        }
    }

    // ============ 规则 3：连续 6 点递增或递减 ============
    private static void rule3(List<SpcRuleViolationVO> violations, List<BigDecimal> samples) {
        int required = 6;
        if (samples.size() < required) {
            return;
        }
        // 递增
        int incRun = 1;
        int incStart = 0;
        for (int i = 1; i < samples.size(); i++) {
            if (samples.get(i).compareTo(samples.get(i - 1)) > 0) {
                if (incRun == 1) {
                    incStart = i - 1;
                }
                incRun++;
                if (incRun >= required) {
                    violations.add(build(WesternElectricRuleEnum.RULE_3, incStart, i, incRun));
                    incRun = 0;
                }
            } else {
                incRun = 1;
            }
        }
        // 递减
        int decRun = 1;
        int decStart = 0;
        for (int i = 1; i < samples.size(); i++) {
            if (samples.get(i).compareTo(samples.get(i - 1)) < 0) {
                if (decRun == 1) {
                    decStart = i - 1;
                }
                decRun++;
                if (decRun >= required) {
                    violations.add(build(WesternElectricRuleEnum.RULE_3, decStart, i, decRun));
                    decRun = 0;
                }
            } else {
                decRun = 1;
            }
        }
    }

    // ============ 规则 4：连续 14 点交替上下 ============
    private static void rule4(List<SpcRuleViolationVO> violations, List<BigDecimal> samples) {
        int required = 14;
        if (samples.size() < required) {
            return;
        }
        int run = 1;
        int startIdx = 0;
        Boolean prevDiff = null; // true=升，false=降
        for (int i = 1; i < samples.size(); i++) {
            int cmp = samples.get(i).compareTo(samples.get(i - 1));
            Boolean curDiff = cmp > 0 ? Boolean.TRUE : (cmp < 0 ? Boolean.FALSE : null);
            if (curDiff == null) {
                run = 1;
                prevDiff = null;
                continue;
            }
            if (prevDiff != null && !prevDiff.equals(curDiff)) {
                if (run == 1) {
                    startIdx = i - 1;
                }
                run++;
                if (run >= required) {
                    violations.add(build(WesternElectricRuleEnum.RULE_4, startIdx, i, run));
                    run = 0;
                    prevDiff = null;
                } else {
                    prevDiff = curDiff;
                }
            } else {
                run = 1;
                prevDiff = curDiff;
            }
        }
    }

    // ============ 规则 5：连续 3 点中有 2 点落在 A 区（同侧 2σ~3σ） ============
    private static void rule5(List<SpcRuleViolationVO> violations, List<BigDecimal> samples,
                              BigDecimal mean, BigDecimal sigma2, BigDecimal sigma3) {
        int window = 3;
        int threshold = 2;
        if (samples.size() < window) {
            return;
        }
        for (int i = 0; i + window - 1 < samples.size(); i++) {
            int posA = 0; // 正侧 A 区点数
            int negA = 0; // 负侧 A 区点数
            for (int j = i; j < i + window; j++) {
                BigDecimal v = samples.get(j);
                BigDecimal diff = v.subtract(mean).abs();
                if (diff.compareTo(sigma2) > 0 && diff.compareTo(sigma3) <= 0) {
                    if (v.compareTo(mean) > 0) {
                        posA++;
                    } else if (v.compareTo(mean) < 0) {
                        negA++;
                    }
                }
            }
            if (posA >= threshold || negA >= threshold) {
                violations.add(build(WesternElectricRuleEnum.RULE_5, i, i + window - 1, window));
            }
        }
    }

    // ============ 规则 6：连续 5 点中有 4 点落在 B 区之外（同侧 >1σ） ============
    private static void rule6(List<SpcRuleViolationVO> violations, List<BigDecimal> samples,
                              BigDecimal mean, BigDecimal sigma1, BigDecimal sigma2) {
        int window = 5;
        int threshold = 4;
        if (samples.size() < window) {
            return;
        }
        for (int i = 0; i + window - 1 < samples.size(); i++) {
            int posOut = 0; // 正侧 >1σ
            int negOut = 0; // 负侧 >1σ
            for (int j = i; j < i + window; j++) {
                BigDecimal v = samples.get(j);
                BigDecimal diff = v.subtract(mean);
                if (diff.abs().compareTo(sigma1) > 0) {
                    if (diff.compareTo(BigDecimal.ZERO) > 0) {
                        posOut++;
                    } else {
                        negOut++;
                    }
                }
            }
            if (posOut >= threshold || negOut >= threshold) {
                violations.add(build(WesternElectricRuleEnum.RULE_6, i, i + window - 1, window));
            }
        }
    }

    // ============ 规则 7：连续 15 点落在 C 区内（≤1σ） ============
    private static void rule7(List<SpcRuleViolationVO> violations, List<BigDecimal> samples,
                               BigDecimal mean, BigDecimal sigma1) {
        int required = 15;
        if (samples.size() < required) {
            return;
        }
        int run = 0;
        int startIdx = 0;
        for (int i = 0; i < samples.size(); i++) {
            BigDecimal diff = samples.get(i).subtract(mean).abs();
            if (diff.compareTo(sigma1) <= 0) {
                if (run == 0) {
                    startIdx = i;
                }
                run++;
                if (run >= required) {
                    violations.add(build(WesternElectricRuleEnum.RULE_7, startIdx, i, run));
                    run = 0;
                }
            } else {
                run = 0;
            }
        }
    }

    // ============ 规则 8：连续 8 点落在 C 区之外（两侧 >1σ） ============
    private static void rule8(List<SpcRuleViolationVO> violations, List<BigDecimal> samples,
                              BigDecimal mean, BigDecimal sigma1) {
        int required = 8;
        if (samples.size() < required) {
            return;
        }
        int run = 0;
        int startIdx = 0;
        for (int i = 0; i < samples.size(); i++) {
            BigDecimal diff = samples.get(i).subtract(mean).abs();
            if (diff.compareTo(sigma1) > 0) {
                if (run == 0) {
                    startIdx = i;
                }
                run++;
                if (run >= required) {
                    violations.add(build(WesternElectricRuleEnum.RULE_8, startIdx, i, run));
                    run = 0;
                }
            } else {
                run = 0;
            }
        }
    }

    private static SpcRuleViolationVO build(WesternElectricRuleEnum rule, int startIdx, int endIdx, int count) {
        SpcRuleViolationVO vo = new SpcRuleViolationVO();
        vo.setRuleCode(rule.getCode());
        vo.setRuleDescription(rule.getDescription());
        vo.setStartIndex(startIdx);
        vo.setEndIndex(endIdx);
        vo.setAffectedCount(count);
        return vo;
    }

}
