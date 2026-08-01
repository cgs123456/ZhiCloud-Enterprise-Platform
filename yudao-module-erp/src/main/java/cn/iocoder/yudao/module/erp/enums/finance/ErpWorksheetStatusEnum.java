package cn.iocoder.yudao.module.erp.enums.finance;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * ERP 合并工作底稿状态枚举（P1-合并报表引擎）
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum ErpWorksheetStatusEnum implements ArrayValuable<Integer> {

    /**
     * 待审核
     */
    PENDING(10, "待审核"),
    /**
     * 已审核
     */
    APPROVED(20, "已审核"),
    /**
     * 已驳回
     */
    REJECTED(30, "已驳回");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpWorksheetStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
