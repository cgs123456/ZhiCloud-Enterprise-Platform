package cn.iocoder.yudao.framework.inventory.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.*;

import java.math.BigDecimal;

/**
 * 共享库存条目 DO（P1-4 统一库存模型）
 *
 * <p>以 MES 物料库存的 {@code (item_id, warehouse_id, location_id, area_id, batch_id)} 复合唯一键为规范键。
 * 各业务模块的物品标识（sku / product / item）在接入共享 Starter 时通过 {@code ItemIdentity} 映射层对齐。
 *
 * <p>并发保护采用 P2 已验证的「三件套」：
 * <ol>
 *     <li>复合唯一键 + {@code getOrCreate} 的 {@code DuplicateKeyException} 兜底（防并发插入冲突）；</li>
 *     <li>DB 层 CAS {@code UPDATE ... SET quantity = quantity + ? WHERE ... AND (quantity + ?) >= 0}（防超扣）；</li>
 *     <li>{@code @Version} 乐观锁（守护全量 {@code updateById} 路径）。</li>
 * </ol>
 *
 * @author 智云库存治理
 */
@TableName("inventory_item")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItemDO extends BaseDO {

    /**
     * 主键编号（DB 自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 乐观锁版本号（P2：@Version 并发保护）
     */
    @Version
    private Long version;

    /**
     * 物品编号（规范键：统一标识，由各模块 ItemIdentity 映射层对齐 sku / product / item）
     */
    private Long itemId;

    /**
     * 仓库编号（规范键）
     */
    private Long warehouseId;

    /**
     * 库位编号（规范键，未指定填 0）
     */
    private Long locationId;

    /**
     * 库区编号（规范键，未指定填 0）
     */
    private Long areaId;

    /**
     * 批次编号（规范键，未指定填 0）
     */
    private Long batchId;

    /**
     * 批次编码（冗余展示字段）
     */
    private String batchCode;

    /**
     * 库存数量（物理总量）
     */
    private BigDecimal quantity;

    /**
     * 锁定数量（已被订单/波次预占但未出库的数量）
     */
    private BigDecimal lockedCount;

    /**
     * 租户编号
     */
    private Long tenantId;

}
