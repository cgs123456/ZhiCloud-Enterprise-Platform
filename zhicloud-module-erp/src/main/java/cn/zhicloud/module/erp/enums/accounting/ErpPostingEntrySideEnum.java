package cn.zhicloud.module.erp.enums.accounting;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * ERP 过账规则分录方向。
 *
 * @author 智云
 */
@Getter
@RequiredArgsConstructor
public enum ErpPostingEntrySideEnum {

    DEBIT(10, "借"),
    CREDIT(20, "贷");

    private final Integer side;
    private final String name;

    public static ErpPostingEntrySideEnum valueOf(Integer side) {
        if (side == null) {
            return null;
        }
        for (ErpPostingEntrySideEnum value : values()) {
            if (value.side.equals(side)) {
                return value;
            }
        }
        return null;
    }

}
