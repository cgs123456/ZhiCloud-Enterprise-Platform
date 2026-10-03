package cn.zhicloud.framework.mdm.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 主数据跨域对照登记请求（V101）
 *
 * <p>登记语义是 upsert 而不是 insert：同一「源键 -> 目标实体」只允许一行，重复登记会把
 * {@code targetId} 更新为最新值。理由是对照表是「当前事实」，不是流水；
 * 保留多行会让解析时必须额外决定「以哪行为准」，反而制造歧义。
 *
 * @author 智云集成治理
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MdmCrossRefUpsertReq {

    /** 租户编号（空则使用当前租户上下文；上下文缺失时拒绝操作，禁止回退默认租户 0） */
    private Long tenantId;

    /** 源系统，如 ERP（必填） */
    private String sourceSystem;

    /** 源实体类型，如 PRODUCT（必填） */
    private String sourceEntity;

    /** 源编号（必填） */
    private String sourceId;

    /** 目标系统，如 WMS（必填） */
    private String targetSystem;

    /** 目标实体类型，如 SKU（必填） */
    private String targetEntity;

    /** 目标编号（必填） */
    private String targetId;

    /** 状态（空则默认 VALID） */
    private String status;

    /** 备注 */
    private String remark;

}