package cn.iocoder.yudao.module.qms.enums.qms;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS 供应商等级枚举
 *
 * <p>基于 PPM（百万分之缺陷数）/ 交期达成率 / 质量合格率综合评级：
 * <ul>
 *   <li>A：优秀供应商</li>
 *   <li>B：合格供应商</li>
 *   <li>C：待改进供应商</li>
 *   <li>D：不合格供应商</li>
 * </ul>
 *
 * @author yudao
 */
@RequiredArgsConstructor
@Getter
public enum SupplierGradeEnum implements ArrayValuable<String> {

    A("A", "优秀"),
    B("B", "合格"),
    C("C", "待改进"),
    D("D", "不合格"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values()).map(SupplierGradeEnum::getGrade).toArray(String[]::new);

    private final String grade;
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}