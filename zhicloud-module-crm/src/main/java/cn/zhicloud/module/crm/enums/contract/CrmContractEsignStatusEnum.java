package cn.zhicloud.module.crm.enums.contract;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * CRM 合同电子签任务状态枚举（ADR 271 D1）
 *
 * @author dhb52
 */
@RequiredArgsConstructor
@Getter
public enum CrmContractEsignStatusEnum implements ArrayValuable<Integer> {

    PENDING(10, "待签署"),
    SIGNED(20, "已签署"),
    REJECTED(30, "已拒签");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(CrmContractEsignStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    /**
     * 按状态值取状态名（未知返回 null）
     *
     * @param status 状态值
     * @return 状态名
     */
    public static String getName(Integer status) {
        for (CrmContractEsignStatusEnum item : values()) {
            if (item.getStatus().equals(status)) {
                return item.getName();
            }
        }
        return null;
    }

}
