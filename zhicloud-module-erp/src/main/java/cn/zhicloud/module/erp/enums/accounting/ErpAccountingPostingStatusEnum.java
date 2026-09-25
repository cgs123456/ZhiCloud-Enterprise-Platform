package cn.zhicloud.module.erp.enums.accounting;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * ERP 会计过账结果状态。
 *
 * @author 智云
 */
@Getter
@RequiredArgsConstructor
public enum ErpAccountingPostingStatusEnum {

    NEW(0, "新建"),
    POSTING(10, "过账中"),
    POSTED(20, "已过账"),
    FAILED(30, "失败"),
    REVERSED(40, "已冲销");

    private final Integer status;
    private final String name;

}
