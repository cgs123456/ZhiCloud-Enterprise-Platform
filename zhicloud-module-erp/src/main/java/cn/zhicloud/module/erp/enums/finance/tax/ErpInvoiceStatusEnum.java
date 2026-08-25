package cn.zhicloud.module.erp.enums.finance.tax;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * ERP 发票状态枚举
 *
 * <p>状态流转：
 * <ul>
 *   <li>草稿(DRAFT) → 已开具(ISSUED)：调用 issueInvoice</li>
 *   <li>已开具(ISSUED) → 已作废(REVOKED)：调用 revokeInvoice</li>
 *   <li>已开具(ISSUED) → 已红冲(RED)：调用 redInvoice（生成红字发票）</li>
 * </ul>
 *
 * @author 智云
 */
@Getter
@AllArgsConstructor
public enum ErpInvoiceStatusEnum implements ArrayValuable<Integer> {

    /**
     * 草稿
     */
    DRAFT(10, "草稿"),
    /**
     * 已开具
     */
    ISSUED(20, "已开具"),
    /**
     * 已作废
     */
    REVOKED(30, "已作废"),
    /**
     * 已红冲
     */
    RED(40, "已红冲");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpInvoiceStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
