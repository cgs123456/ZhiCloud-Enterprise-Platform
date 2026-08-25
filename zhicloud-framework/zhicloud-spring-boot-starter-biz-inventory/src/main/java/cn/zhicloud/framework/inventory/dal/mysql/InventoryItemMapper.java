package cn.zhicloud.framework.inventory.dal.mysql;

import cn.zhicloud.framework.inventory.dal.dataobject.InventoryItemDO;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 共享库存条目 Mapper（P1-4）
 *
 * @author 智云库存治理
 */
@Mapper
public interface InventoryItemMapper extends BaseMapperX<InventoryItemDO> {

    /**
     * 按规范复合键查询库存条目
     */
    default InventoryItemDO selectByCompositeKey(Long itemId, Long warehouseId,
                                                 Long locationId, Long areaId, Long batchId) {
        return selectOne(new LambdaQueryWrapper<InventoryItemDO>()
                .eq(InventoryItemDO::getItemId, itemId)
                .eq(InventoryItemDO::getWarehouseId, warehouseId)
                .eq(InventoryItemDO::getLocationId, locationId)
                .eq(InventoryItemDO::getAreaId, areaId)
                .eq(InventoryItemDO::getBatchId, batchId));
    }

    /**
     * DB 层 CAS：库存数量增量更新，原子防超扣
     * <p>仅当 {@code quantity + delta >= 0} 时更新，返回受影响行数（0 表示会超扣，调用方应抛库存不足）。
     */
    @Update("UPDATE inventory_item SET quantity = quantity + #{delta} " +
            "WHERE id = #{id} AND (quantity + #{delta}) >= 0")
    int updateCountIncrement(@Param("id") Long id, @Param("delta") java.math.BigDecimal delta);

    /**
     * DB 层 CAS：锁定数量增量更新，原子防超释放
     * <p>仅当 {@code locked_count + delta >= 0} 时更新。
     */
    @Update("UPDATE inventory_item SET locked_count = locked_count + #{delta} " +
            "WHERE id = #{id} AND (locked_count + #{delta}) >= 0")
    int updateLockedCountIncrement(@Param("id") Long id, @Param("delta") java.math.BigDecimal delta);

}
