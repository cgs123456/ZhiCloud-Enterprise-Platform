package cn.zhicloud.module.erp.enums.finance;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP 会计凭证状态枚举（P0-7）
 *
 * <p>状态流转：
 * <ul>
 *   <li>{@link #DRAFT} 草稿：可修改、可删除</li>
 *   <li>{@link #APPROVED} 已审核：已过账到总账，不可修改</li>
 *   <li>{@link #REVERSED} 已反审核：凭证已作废，可重新审核</li>
 * </ul>
 *
 * <p>状态流转：DRAFT → APPROVED（审核）→ DRAFT（反审核）。
 * 已审核的凭证会更新对应科目的余额。
 *
 * @author 智云
 */
@RequiredArgsConstructor
@Getter
public enum ErpGlVoucherStatusEnum implements ArrayValuable<Integer> {

    DRAFT(10, "草稿"),
    APPROVED(20, "已审核"),
    REVERSED(30, "已反审核");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpGlVoucherStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
