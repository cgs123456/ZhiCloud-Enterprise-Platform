package cn.zhicloud.module.qms.service.spc;

import cn.zhicloud.framework.test.core.ut.BaseMockitoUnitTest;
import cn.zhicloud.module.qms.controller.admin.spc.vo.SpcRuleViolationVO;
import cn.zhicloud.module.qms.enums.qms.WesternElectricRuleEnum;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link WesternElectricRuleDetector} 的单元测试
 *
 * <p>覆盖 Western Electric 8 条失控规则（含各区 A/B/C 边界判定）及退化输入。
 *
 * @author zhicloud
 */
public class WesternElectricRuleDetectorTest extends BaseMockitoUnitTest {

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static List<BigDecimal> list(String... values) {
        List<BigDecimal> result = new ArrayList<>();
        for (String v : values) {
            result.add(bd(v));
        }
        return result;
    }

    private static boolean hasRule(List<SpcRuleViolationVO> violations, WesternElectricRuleEnum rule) {
        return violations.stream().anyMatch(v -> Integer.valueOf(rule.getCode()).equals(v.getRuleCode()));
    }

    @Test
    public void testDetect_nullOrEmpty() {
        assertTrue(WesternElectricRuleDetector.detect(null, bd("10"), bd("1")).isEmpty());
        assertTrue(WesternElectricRuleDetector.detect(list(), bd("10"), bd("1")).isEmpty());
        // stdDev 为 0 时直接返回（避免除零 / 无意义）
        assertTrue(WesternElectricRuleDetector.detect(list("10", "10"), bd("10"), BigDecimal.ZERO).isEmpty());
    }

    @Test
    public void testRule1_pointBeyond3Sigma() {
        // mean=10, stdDev=1 -> UCL=13, LCL=7，20 远超 3σ
        List<SpcRuleViolationVO> violations = WesternElectricRuleDetector.detect(
                list("10", "10", "10", "20"), bd("10"), bd("1"));
        assertTrue(hasRule(violations, WesternElectricRuleEnum.RULE_1));
    }

    @Test
    public void testRule2_nineOnSameSide() {
        // 10 个连续落在均值同一侧（>10）的点
        List<SpcRuleViolationVO> violations = WesternElectricRuleDetector.detect(
                list("11.0", "11.1", "11.2", "11.3", "11.4", "11.5", "11.6", "11.7", "11.8", "11.9"),
                bd("10"), bd("1"));
        assertTrue(hasRule(violations, WesternElectricRuleEnum.RULE_2));
    }

    @Test
    public void testRule3_sixMonotonic() {
        List<SpcRuleViolationVO> violations = WesternElectricRuleDetector.detect(
                list("1", "2", "3", "4", "5", "6", "7"), bd("4"), bd("2"));
        assertTrue(hasRule(violations, WesternElectricRuleEnum.RULE_3));
    }

    @Test
    public void testRule4_fourteenAlternating() {
        List<SpcRuleViolationVO> violations = WesternElectricRuleDetector.detect(
                list("1", "3", "1", "3", "1", "3", "1", "3", "1", "3", "1", "3", "1", "3", "1", "3"),
                bd("2"), bd("1"));
        assertTrue(hasRule(violations, WesternElectricRuleEnum.RULE_4));
    }

    @Test
    public void testRule5_twoInAZoneSameSide() {
        // mean=10, stdDev=1 -> A 区 = (2,3]，12.1 与 12.5 落在 A 区同侧
        List<SpcRuleViolationVO> violations = WesternElectricRuleDetector.detect(
                list("10.0", "12.1", "12.5"), bd("10"), bd("1"));
        assertTrue(hasRule(violations, WesternElectricRuleEnum.RULE_5));
    }

    @Test
    public void testRule6_fourOutsideBZoneSameSide() {
        // 5 窗口内 4 个 >1σ（同侧），5.0 落在另一侧不参与
        List<SpcRuleViolationVO> violations = WesternElectricRuleDetector.detect(
                list("11.5", "11.6", "11.7", "11.8", "5.0"), bd("10"), bd("1"));
        assertTrue(hasRule(violations, WesternElectricRuleEnum.RULE_6));
    }

    @Test
    public void testRule7_fifteenWithinCZone() {
        // 15 个连续落入 C 区（|diff| <= 1σ）
        List<SpcRuleViolationVO> violations = WesternElectricRuleDetector.detect(
                list("10.1", "10.2", "10.3", "10.4", "10.5", "10.6", "10.7", "10.8", "10.9",
                        "9.1", "9.2", "9.3", "9.4", "9.5", "9.6"),
                bd("10"), bd("1"));
        assertTrue(hasRule(violations, WesternElectricRuleEnum.RULE_7));
    }

    @Test
    public void testRule8_eightOutsideCZoneBothSides() {
        // 8+ 个连续落在 C 区之外（两侧），交替分布
        List<SpcRuleViolationVO> violations = WesternElectricRuleDetector.detect(
                list("11.5", "8.5", "11.5", "8.5", "11.5", "8.5", "11.5", "8.5", "11.5"),
                bd("10"), bd("1"));
        assertTrue(hasRule(violations, WesternElectricRuleEnum.RULE_8));
    }

    @Test
    public void testNoViolation_normalSpread() {
        List<SpcRuleViolationVO> violations = WesternElectricRuleDetector.detect(
                list("9.5", "10.2", "9.8", "10.1", "9.9", "10.0", "10.3", "9.7"),
                bd("10"), bd("0.3"));
        assertFalse(hasRule(violations, WesternElectricRuleEnum.RULE_1));
        assertFalse(hasRule(violations, WesternElectricRuleEnum.RULE_2));
        assertEquals(0, violations.size());
    }
}
