package cn.iocoder.yudao.module.mes.dal.mysql.wm.materialstock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.wm.materialstock.vo.MesWmMaterialStockListReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.wm.materialstock.vo.MesWmMaterialStockPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.materialstock.MesWmMaterialStockDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/**
 * MES 库存台账 Mapper
 */
@Mapper
public interface MesWmMaterialStockMapper extends BaseMapperX<MesWmMaterialStockDO> {

    default PageResult<MesWmMaterialStockDO> selectPage(MesWmMaterialStockPageReqVO reqVO,
                                                         Collection<Long> itemTypeIds,
                                                         Collection<Long> itemIds,
                                                         Long virtualWarehouseId) {
        LambdaQueryWrapperX<MesWmMaterialStockDO> wrapper = new LambdaQueryWrapperX<MesWmMaterialStockDO>()
                .inIfPresent(MesWmMaterialStockDO::getItemTypeId, itemTypeIds)
                .inIfPresent(MesWmMaterialStockDO::getItemId, itemIds)
                .likeIfPresent(MesWmMaterialStockDO::getBatchCode, reqVO.getBatchCode())
                .eqIfPresent(MesWmMaterialStockDO::getBatchId, reqVO.getBatchId())
                .eqIfPresent(MesWmMaterialStockDO::getWarehouseId, reqVO.getWarehouseId())
                .eqIfPresent(MesWmMaterialStockDO::getLocationId, reqVO.getLocationId())
                .eqIfPresent(MesWmMaterialStockDO::getAreaId, reqVO.getAreaId())
                .eqIfPresent(MesWmMaterialStockDO::getVendorId, reqVO.getVendorId())
                .eqIfPresent(MesWmMaterialStockDO::getFrozen, reqVO.getFrozen());
        wrapper.ne(MesWmMaterialStockDO::getQuantity, BigDecimal.ZERO)
                .orderByAsc(MesWmMaterialStockDO::getReceiptTime);
        // 虚拟仓过滤（Service 层已将 virtualFilter 解析为 virtualWarehouseId）
        if (virtualWarehouseId != null) {
            if (MesWmMaterialStockPageReqVO.VIRTUAL_FILTER_ONLY.equals(reqVO.getVirtualFilter())) {
                wrapper.eq(MesWmMaterialStockDO::getWarehouseId, virtualWarehouseId);
            } else if (MesWmMaterialStockPageReqVO.VIRTUAL_FILTER_EXCLUDE.equals(reqVO.getVirtualFilter())) {
                wrapper.ne(MesWmMaterialStockDO::getWarehouseId, virtualWarehouseId);
            }
        }
        return selectPage(reqVO, wrapper);
    }

    default Long selectCountByWarehouseId(Long warehouseId) {
        return selectCount(MesWmMaterialStockDO::getWarehouseId, warehouseId);
    }

    default Long selectCountByLocationId(Long locationId) {
        return selectCount(MesWmMaterialStockDO::getLocationId, locationId);
    }

    default Long selectCountByAreaId(Long areaId) {
        return selectCount(MesWmMaterialStockDO::getAreaId, areaId);
    }

    default List<MesWmMaterialStockDO> selectListByIds(Collection<Long> ids) {
        return selectByIds(ids);
    }

    default void updateByIds(Collection<Long> ids, MesWmMaterialStockDO updateObj) {
        update(updateObj, new LambdaUpdateWrapper<MesWmMaterialStockDO>()
                .in(MesWmMaterialStockDO::getId, ids));
    }

    /**
     * 增量更新库存数量
     * <p>
     * 使用 {@code #{count}} 参数绑定，避免 BigDecimal 拼接产生的科学计数法（如 1E+3）导致 SQL 异常。
     * 当 checkFlag 为 true 且为扣减（count &lt; 0）时，通过 {@code AND quantity &gt;= abs(count)} 在数据库层原子校验防负库存。
     *
     * @return 影响行数（0 表示库存不足被 CAS 拦截）
     */
    default int updateQuantity(Long id, BigDecimal count, boolean checkFlag) {
        return updateQuantityInternal(id, count, checkFlag, count.abs());
    }

    @Update("<script>"
            + "UPDATE mes_wm_material_stock SET quantity = quantity + #{count} "
            + "WHERE id = #{id} "
            + "<if test='checkFlag and count != null and count.signum() &lt; 0'>AND quantity &gt;= #{absCount}</if>"
            + "</script>")
    int updateQuantityInternal(@Param("id") Long id, @Param("count") BigDecimal count,
                               @Param("checkFlag") boolean checkFlag, @Param("absCount") BigDecimal absCount);

    default MesWmMaterialStockDO selectByCompositeKey(Long itemId, Long warehouseId, Long locationId,
                                                       Long areaId, Long batchId) {
        LambdaQueryWrapperX<MesWmMaterialStockDO> wrapper = new LambdaQueryWrapperX<MesWmMaterialStockDO>()
                .eqIfPresent(MesWmMaterialStockDO::getItemId, itemId)
                .eqIfPresent(MesWmMaterialStockDO::getWarehouseId, warehouseId)
                .eqIfPresent(MesWmMaterialStockDO::getLocationId, locationId)
                .eqIfPresent(MesWmMaterialStockDO::getAreaId, areaId);
        // batchId=null 时精确匹配 is null，避免 eqIfPresent 跳过条件导致匹配到其他批次
        if (batchId != null) {
            wrapper.eq(MesWmMaterialStockDO::getBatchId, batchId);
        } else {
            wrapper.isNull(MesWmMaterialStockDO::getBatchId);
        }
        return selectOne(wrapper);
    }

    default List<MesWmMaterialStockDO> selectList(MesWmMaterialStockListReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<MesWmMaterialStockDO>()
                .eqIfPresent(MesWmMaterialStockDO::getWarehouseId, reqVO.getWarehouseId())
                .eqIfPresent(MesWmMaterialStockDO::getLocationId, reqVO.getLocationId())
                .eqIfPresent(MesWmMaterialStockDO::getAreaId, reqVO.getAreaId())
                .eqIfPresent(MesWmMaterialStockDO::getItemId, reqVO.getItemId())
                .eqIfPresent(MesWmMaterialStockDO::getBatchId, reqVO.getBatchId())
                .likeIfPresent(MesWmMaterialStockDO::getBatchCode, reqVO.getBatchCode())
                .geIfPresent(MesWmMaterialStockDO::getUpdateTime, reqVO.getStartTime())
                .leIfPresent(MesWmMaterialStockDO::getUpdateTime, reqVO.getEndTime())
                .ne(MesWmMaterialStockDO::getQuantity, BigDecimal.ZERO)
                .orderByAsc(MesWmMaterialStockDO::getReceiptTime));
    }

}
