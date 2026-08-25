package cn.zhicloud.module.qms.enums.qms;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS 质量成本类型枚举（PAIF 模型）
 *
 * <p>PAIF 将质量成本划分为四类：
 * <ul>
 *   <li>PREVENTION：预防成本（如质量培训、质量规划、防错设计）</li>
 *   <li>APPRAISAL：鉴定成本（如检验设备、检测人员薪资、外部认证）</li>
 *   <li>INTERNAL_FAILURE：内部故障成本（如返工、返修、废品、停工损失）</li>
 *   <li>EXTERNAL_FAILURE：外部故障成本（如退货处理、索赔、售后维修、召回）</li>
 * </ul>
 *
 * @author zhicloud
 */
@RequiredArgsConstructor
@Getter
public enum QmsQualityCostTypeEnum implements ArrayValuable<String> {

    PREVENTION("PREVENTION", "预防成本"),
    APPRAISAL("APPRAISAL", "鉴定成本"),
    INTERNAL_FAILURE("INTERNAL_FAILURE", "内部故障"),
    EXTERNAL_FAILURE("EXTERNAL_FAILURE", "外部故障"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values()).map(QmsQualityCostTypeEnum::getType).toArray(String[]::new);

    /**
     * 类型
     */
    private final String type;
    /**
     * 名称
     */
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}