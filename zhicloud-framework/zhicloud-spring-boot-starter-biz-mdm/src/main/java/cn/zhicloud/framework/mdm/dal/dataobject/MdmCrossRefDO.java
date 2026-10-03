package cn.zhicloud.framework.mdm.dal.dataobject;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 主数据跨域对照 DO（V101）
 *
 * <p>解决的问题：ERP 的 productId / warehouseId 与 WMS 的 skuId / warehouseId 是两套独立主键，
 * 「两边编码恰好相同」只是数据巧合，不是契约。没有显式对照表时，跨模块集成只能靠人工约定，
 * 一旦某侧主键重新生成或导入顺序变化，下游就会把单据建到错误的 SKU / 仓库上。
 *
 * <p>字段与 V101__mdm_cross_ref.sql 的 {@code mdm_cross_ref} 列一一对应
 * （{@code tenant_id} 由 {@link TenantBaseDO} 提供，{@code creator/create_time/updater/update_time/deleted} 由父类一并提供）。
 *
 * <p>{@code sourceId/targetId} 用 VARCHAR 而不是 BIGINT：ERP 产品、WMS SKU 目前都是数值主键，
 * 但外部系统（电商平台、客户编码）的标识可能是字符串，用数值列会把「支持字符串标识」这条路堵死。
 *
 * @author 智云集成治理
 */
@TableName("mdm_cross_ref")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MdmCrossRefDO extends TenantBaseDO {

    /**
     * 主键编号（DB 自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;


    /**
     * 源系统，如 ERP
     */
    private String sourceSystem;

    /**
     * 源实体类型，如 PRODUCT / WAREHOUSE / CUSTOMER
     */
    private String sourceEntity;

    /**
     * 源编号（字符串，兼容外部系统的非数值标识）
     */
    private String sourceId;

    /**
     * 目标系统，如 WMS
     */
    private String targetSystem;

    /**
     * 目标实体类型，如 SKU / WAREHOUSE / MERCHANT
     */
    private String targetEntity;

    /**
     * 目标编号
     */
    private String targetId;

    /**
     * 状态：VALID 生效 / INVALID 已失效
     */
    private String status;

    /**
     * 备注（映射来源、维护人、失效原因）
     */
    private String remark;

}