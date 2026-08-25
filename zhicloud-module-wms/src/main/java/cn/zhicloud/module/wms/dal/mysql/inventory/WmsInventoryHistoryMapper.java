package cn.zhicloud.module.wms.dal.mysql.inventory;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.zhicloud.module.wms.controller.admin.inventory.vo.history.WmsInventoryHistoryPageReqVO;
import cn.zhicloud.module.wms.dal.dataobject.inventory.WmsInventoryHistoryDO;
import cn.zhicloud.module.wms.dal.dataobject.md.item.WmsItemDO;
import cn.zhicloud.module.wms.dal.dataobject.md.item.WmsItemSkuDO;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * WMS 库存流水 Mapper
 *
 * @author 智云
 */
@Mapper
public interface WmsInventoryHistoryMapper extends BaseMapperX<WmsInventoryHistoryDO> {

    default PageResult<WmsInventoryHistoryDO> selectPage(WmsInventoryHistoryPageReqVO reqVO) {
        MPJLambdaWrapperX<WmsInventoryHistoryDO> query = new MPJLambdaWrapperX<WmsInventoryHistoryDO>()
                .selectAll(WmsInventoryHistoryDO.class)
                .leftJoin(WmsItemSkuDO.class, WmsItemSkuDO::getId, WmsInventoryHistoryDO::getSkuId)
                .leftJoin(WmsItemDO.class, WmsItemDO::getId, WmsItemSkuDO::getItemId)
                .likeIfPresent(WmsItemDO::getCode, reqVO.getItemCode())
                .likeIfPresent(WmsItemDO::getName, reqVO.getItemName())
                .eqIfPresent(WmsInventoryHistoryDO::getSkuId, reqVO.getSkuId())
                .likeIfPresent(WmsItemSkuDO::getCode, reqVO.getSkuCode())
                .likeIfPresent(WmsItemSkuDO::getName, reqVO.getSkuName())
                .eqIfPresent(WmsInventoryHistoryDO::getWarehouseId, reqVO.getWarehouseId())
                .eqIfPresent(WmsInventoryHistoryDO::getOrderNo, reqVO.getOrderNo())
                .eqIfPresent(WmsInventoryHistoryDO::getOrderType, reqVO.getOrderType())
                .betweenIfPresent(WmsInventoryHistoryDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(WmsInventoryHistoryDO::getCreateTime)
                .orderByDesc(WmsInventoryHistoryDO::getId);
        return selectJoinPage(reqVO, WmsInventoryHistoryDO.class, query);
    }

    /**
     * 按单据类型与创建时间区间查询库存流水列表
     *
     * @param orderType 单据类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 库存流水列表
     */
    default List<WmsInventoryHistoryDO> selectListByOrderTypeAndCreateTimeBetween(
            Integer orderType, LocalDateTime startTime, LocalDateTime endTime) {
        return selectList(new LambdaQueryWrapperX<WmsInventoryHistoryDO>()
                .eq(WmsInventoryHistoryDO::getOrderType, orderType)
                .ge(WmsInventoryHistoryDO::getCreateTime, startTime)
                .le(WmsInventoryHistoryDO::getCreateTime, endTime));
    }

    /**
     * SKU 出库汇总轻量 DTO（ABC 分析用）
     */
    @Data
    class SkuOutboundSummary {
        /**
         * SKU 编号
         */
        private Long skuId;
        /**
         * 出库次数
         */
        private Long outCount;
        /**
         * 出库数量（绝对值合计）
         */
        private BigDecimal outQuantity;
        /**
         * 出库金额（绝对值合计）
         */
        private BigDecimal outAmount;
    }

    /**
     * 按单据类型与时间区间，按 SKU 聚合出库次数、数量、金额（GROUP BY 下推 SQL，避免全量拉取内存聚合）
     *
     * <p>出库流水 quantity / total_price 为负数，SQL 层用 ABS 取绝对值后求和。
     *
     * @param orderType 单据类型（如 SHIPMENT）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 每个 SKU 的出库汇总列表
     */
    @Select("SELECT sku_id AS sku_id, COUNT(*) AS out_count, "
            + "SUM(ABS(quantity)) AS out_quantity, SUM(ABS(total_price)) AS out_amount "
            + "FROM wms_inventory_history "
            + "WHERE deleted = 0 AND order_type = #{orderType} AND sku_id IS NOT NULL "
            + "AND create_time >= #{startTime} AND create_time <= #{endTime} "
            + "GROUP BY sku_id")
    List<SkuOutboundSummary> selectOutboundSummaryGroupBySku(@Param("orderType") Integer orderType,
                                                             @Param("startTime") LocalDateTime startTime,
                                                             @Param("endTime") LocalDateTime endTime);

}
