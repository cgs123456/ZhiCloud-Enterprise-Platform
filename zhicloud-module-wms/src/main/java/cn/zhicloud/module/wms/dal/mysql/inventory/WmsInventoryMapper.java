package cn.zhicloud.module.wms.dal.mysql.inventory;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.zhicloud.module.wms.controller.admin.inventory.vo.WmsInventoryListReqVO;
import cn.zhicloud.module.wms.controller.admin.inventory.vo.WmsInventoryPageReqVO;
import cn.zhicloud.module.wms.dal.dataobject.inventory.WmsInventoryDO;
import cn.zhicloud.module.wms.dal.dataobject.md.item.WmsItemDO;
import cn.zhicloud.module.wms.dal.dataobject.md.item.WmsItemSkuDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * WMS 库存 Mapper
 *
 * @author 智云
 */
@Mapper
public interface WmsInventoryMapper extends BaseMapperX<WmsInventoryDO> {

    default PageResult<WmsInventoryDO> selectPage(WmsInventoryPageReqVO reqVO) {
        MPJLambdaWrapperX<WmsInventoryDO> query = new MPJLambdaWrapperX<WmsInventoryDO>()
                .selectAll(WmsInventoryDO.class)
                .innerJoin(WmsItemSkuDO.class, WmsItemSkuDO::getId, WmsInventoryDO::getSkuId)
                .innerJoin(WmsItemDO.class, WmsItemDO::getId, WmsItemSkuDO::getItemId)
                .likeIfPresent(WmsItemDO::getCode, reqVO.getItemCode())
                .likeIfPresent(WmsItemDO::getName, reqVO.getItemName())
                .eqIfPresent(WmsInventoryDO::getSkuId, reqVO.getSkuId())
                .likeIfPresent(WmsItemSkuDO::getCode, reqVO.getSkuCode())
                .likeIfPresent(WmsItemSkuDO::getName, reqVO.getSkuName())
                .eqIfPresent(WmsInventoryDO::getWarehouseId, reqVO.getWarehouseId())
                .eqIfPresent(WmsInventoryDO::getLocationId, reqVO.getLocationId())
                .geIfPresent(WmsInventoryDO::getQuantity, reqVO.getMinQuantity());
        if (Boolean.TRUE.equals(reqVO.getOnlyPositiveQuantity())) {
            query.gt(WmsInventoryDO::getQuantity, BigDecimal.ZERO);
        }
        appendDimensionOrder(query, reqVO.getType());
        return selectJoinPage(reqVO, WmsInventoryDO.class, query);
    }

    default Long selectCountBySkuId(Long skuId) {
        return selectCount(WmsInventoryDO::getSkuId, skuId);
    }

    default Long selectCountByWarehouseId(Long warehouseId) {
        return selectCount(WmsInventoryDO::getWarehouseId, warehouseId);
    }

    default List<WmsInventoryDO> selectList(WmsInventoryListReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<WmsInventoryDO>()
                .eq(WmsInventoryDO::getWarehouseId, reqVO.getWarehouseId())
                .orderByAsc(WmsInventoryDO::getSkuId)
                .orderByAsc(WmsInventoryDO::getId));
    }

    /**
     * 库位维度接入（V205）后两列单行键已不可用：同一 (sku, warehouse) 对应多行
     *（(sku, warehouse, location_id)，location_id = 0 为未定位行，唯一键三元扩展）。
     *
     * <ul>
     *   <li>{@link #selectListBySkuIdAndWarehouseId}：行集（P-deduct 分配 / 聚合计数 / 明细合计口径）</li>
     *   <li>{@link #selectPrimaryRowBySkuIdAndWarehouseId}：主行选择器（0 行有货优先，否则定位行
     *       可用量降序；锁定/禁用库位的行由 excludedLocationIds 排除）</li>
     *   <li>{@link WmsInventoryMapper#pickPrimary}：主行选择核心（行集已在手时复用）</li>
     *   <li>{@link #selectBySkuIdAndWarehouseIdAndLocationId}：三元唯一键精确回查</li>
     *   <li>{@link #selectListByLocationId} / {@link #selectCountByLocationIdAndQuantityPositive}：
     *       库位维度的存量冲洗与占用派生计数（ADR 259 D9/D4）</li>
     *   <li>{@link #deleteByIdListPhysical}：纯零行物理冲洗（绕过 @TableLogic，避免唯一键 tombstone）</li>
     * </ul>
     *
     * 旧「两列键单行查询」已删除：库位维度接入后同一两列键对应多于一行，
     * 单行语义注定抛 SelectOne 异常，由编译期强制全部调用点迁移（ADR 259 D8，十三调用点重定向）。
     */
    default List<WmsInventoryDO> selectListBySkuIdAndWarehouseId(Long skuId, Long warehouseId) {
        return selectList(new LambdaQueryWrapperX<WmsInventoryDO>()
                .eq(WmsInventoryDO::getSkuId, skuId)
                .eq(WmsInventoryDO::getWarehouseId, warehouseId));
    }

    default WmsInventoryDO selectPrimaryRowBySkuIdAndWarehouseId(Long skuId, Long warehouseId,
                                                                 Collection<Long> excludedLocationIds) {
        return pickPrimary(selectListBySkuIdAndWarehouseId(skuId, warehouseId), excludedLocationIds);
    }

    /**
     * 主行选择核心（ADR 259 D8）：0 行（未定位）有货优先 -> 定位行可用量最大者（available 降序、
     * id 升序）-> 0 行兜底（供上游复现既有「缺货报错」语义，行为等价换锚）。
     * excludedLocationIds 为锁定/禁用库位编号集合（null 视为空集）。
     */
    public static WmsInventoryDO pickPrimary(List<WmsInventoryDO> rows, Collection<Long> excludedLocationIds) {
        if (CollUtil.isEmpty(rows)) {
            return null;
        }
        WmsInventoryDO zeroStocked = null;
        WmsInventoryDO zeroAny = null;
        WmsInventoryDO locatedBest = null;
        BigDecimal locatedBestAvailable = BigDecimal.ZERO;
        for (WmsInventoryDO row : rows) {
            Long rowLocation = row.getLocationId() == null ? 0L : row.getLocationId();
            BigDecimal quantity = row.getQuantity() == null ? BigDecimal.ZERO : row.getQuantity();
            BigDecimal available = row.getAvailableQuantity() == null ? BigDecimal.ZERO : row.getAvailableQuantity();
            if (rowLocation != 0L) {
                // 锁定/禁用库位的行不参与主行选择（§12.242 冻结语义落到库存行）
                if (excludedLocationIds != null && excludedLocationIds.contains(rowLocation)) {
                    continue;
                }
                boolean better = locatedBest == null || available.compareTo(locatedBestAvailable) > 0
                        || (available.compareTo(locatedBestAvailable) == 0 && row.getId() != null
                        && locatedBest.getId() != null && row.getId().compareTo(locatedBest.getId()) < 0);
                if (quantity.compareTo(BigDecimal.ZERO) > 0 && better) {
                    locatedBest = row;
                    locatedBestAvailable = available;
                }
                continue;
            }
            if (zeroAny == null || (row.getId() != null && zeroAny.getId() != null
                    && row.getId().compareTo(zeroAny.getId()) < 0)) {
                zeroAny = row;
            }
            if (zeroStocked == null && quantity.compareTo(BigDecimal.ZERO) > 0) {
                zeroStocked = row;
            }
        }
        if (zeroStocked != null) {
            return zeroStocked;
        }
        if (locatedBest != null) {
            return locatedBest;
        }
        return zeroAny;
    }

    default WmsInventoryDO selectBySkuIdAndWarehouseIdAndLocationId(Long skuId, Long warehouseId, long locationId) {
        return selectOne(new LambdaQueryWrapperX<WmsInventoryDO>()
                .eq(WmsInventoryDO::getSkuId, skuId)
                .eq(WmsInventoryDO::getWarehouseId, warehouseId)
                .eq(WmsInventoryDO::getLocationId, locationId));
    }

    default List<WmsInventoryDO> selectListByLocationId(Long locationId) {
        return selectList(new LambdaQueryWrapperX<WmsInventoryDO>()
                .eq(WmsInventoryDO::getLocationId, locationId));
    }

    default Long selectCountByLocationIdAndQuantityPositive(Long locationId) {
        return selectCount(new LambdaQueryWrapperX<WmsInventoryDO>()
                .eq(WmsInventoryDO::getLocationId, locationId)
                .gt(WmsInventoryDO::getQuantity, BigDecimal.ZERO));
    }

    /**
     * 物理删除库存行（绕过 {@code @TableLogic}）
     *
     * V205（ADR 259 D9）：deleteLocation 纯零行冲洗专用——逻辑删留下的 (sku, warehouse, location)
     * tombstone 会占用三元唯一键，同位置重上架将撞键，故必须物理删除。
     */
    @Delete("<script>"
            + "DELETE FROM wms_inventory WHERE id IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    int deleteByIdListPhysical(@Param("ids") List<Long> ids);

    default WmsInventoryDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<WmsInventoryDO>()
                .eq(WmsInventoryDO::getId, id)
                .last("FOR UPDATE"));
    }

    /**
     * 根据多个唯一键，批量查询库存列表
     *
     * <p>使用 MySQL 行构造器 {@code (sku_id, warehouse_id, location_id) IN ((?,?,?), ...)}
     * 代替 OR 链，命中三元唯一索引，避免大 key 集下 OR 链导致的执行计划劣化。
     *
     * @param keys 唯一键列表：由 SKU 编号 + 仓库编号 + 库位编号组成（location_id 不得为 null，
     *             未定位以 0 归一入库——键内 null 在行构造器比较中永不命中，ADR 259 D1/D8）
     * @return 库存列表
     */
    default List<WmsInventoryDO> selectListByKeys(Collection<WmsInventoryDO> keys) {
        if (CollUtil.isEmpty(keys)) {
            return Collections.emptyList();
        }
        List<WmsInventoryDO> keyList = new ArrayList<>(keys);
        return selectListBySkuIdAndWarehouseIdPairs(keyList);
    }

    @Select("<script>"
            + "SELECT * FROM wms_inventory WHERE deleted = 0 AND (sku_id, warehouse_id, location_id) IN "
            + "<foreach collection='keys' item='key' open='(' separator=',' close=')'>"
            + "(#{key.skuId}, #{key.warehouseId}, #{key.locationId})"
            + "</foreach>"
            + "</script>")
    List<WmsInventoryDO> selectListBySkuIdAndWarehouseIdPairs(@Param("keys") List<WmsInventoryDO> keys);

    default List<WmsInventoryDO> selectListByIdsForUpdate(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<WmsInventoryDO>()
                .in(WmsInventoryDO::getId, ids)
                .last("FOR UPDATE"));
    }

    /**
     * 查询待处理呆滞库存（update_time <= threshold）
     * 用于定时任务扫描，避免全表扫描后内存过滤
     */
    default List<WmsInventoryDO> selectDeadStockCandidates(java.time.LocalDateTime threshold) {
        return selectList(new LambdaQueryWrapperX<WmsInventoryDO>()
                .le(WmsInventoryDO::getUpdateTime, threshold)
                .gt(WmsInventoryDO::getQuantity, BigDecimal.ZERO));
    }

    static void appendDimensionOrder(MPJLambdaWrapperX<WmsInventoryDO> query, String type) {
        if (StrUtil.equals(WmsInventoryPageReqVO.TYPE_WAREHOUSE, type)) {
            query.orderByAsc(WmsInventoryDO::getWarehouseId)
                    .orderByAsc(WmsItemSkuDO::getItemId)
                    .orderByAsc(WmsInventoryDO::getSkuId)
                    .orderByAsc(WmsInventoryDO::getId);
            return;
        }
        if (StrUtil.equals(WmsInventoryPageReqVO.TYPE_ITEM, type)) {
            query.orderByAsc(WmsItemSkuDO::getItemId)
                    .orderByAsc(WmsInventoryDO::getSkuId)
                    .orderByAsc(WmsInventoryDO::getWarehouseId)
                    .orderByAsc(WmsInventoryDO::getId);
            return;
        }
        throw new IllegalArgumentException("未知库存统计维度：" + type);
    }

}
