package cn.zhicloud.module.erp.enums;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP 审核状态枚举
 *
 * TODO 芋艿：目前只有待审批、已审批两个状态，未来接入工作流后，会丰富下：待提交（草稿）=》已提交（待审核）=》审核通过、审核不通过；另外，工作流需要支持“反审核”，把工作流退回到原点；
 *
 * @author 智云
 */
@RequiredArgsConstructor
@Getter
public enum ErpAuditStatus implements ArrayValuable<Integer> {

    PROCESS(10, "未审核"), // 审核中
    APPROVE(20, "已审核"); // 审核通过

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(ErpAuditStatus::getStatus).toArray(Integer[]::new);
    /**
     * 判断状态值是否属于已定义的审核状态。
     *
     * @param status 状态值
     * @return 是否合法
     */
    public static boolean isValid(Integer status) {
        return Arrays.stream(values()).anyMatch(value -> value.status.equals(status));
    }

    /**
     * 状态
     */
    private final Integer status;
    /**
     * 状态名
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
