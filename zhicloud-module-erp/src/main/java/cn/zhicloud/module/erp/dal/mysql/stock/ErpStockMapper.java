package cn.zhicloud.module.erp.dal.mysql.stock;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO;
import cn.zhicloud.module.erp.dal.dataobject.stock.ErpStockDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

/**
 * ERP 产品库存 Mapper
 *
 * @author 智云
 */
@Mapper
public interface ErpStockMapper extends BaseMapperX<ErpStockDO> {

    default PageResult<ErpStockDO> selectPage(ErpStockPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpStockDO>()
                .eqIfPresent(ErpStockDO::getProductId, reqVO.getProductId())
                .eqIfPresent(ErpStockDO::getWarehouseId, reqVO.getWarehouseId())
                .orderByDesc(ErpStockDO::getId));
    }

    default ErpStockDO selectByProductIdAndWarehouseId(Long productId, Long warehouseId) {
        return selectOne(ErpStockDO::getProductId, productId,
                ErpStockDO::getWarehouseId, warehouseId);
    }

    /**
     * 增加库存数量（参数化，避免 BigDecimal.toString() 科学计数法导致 SQL 异常）
     */
    @Update("UPDATE erp_stock SET count = count + #{count} " +
            "WHERE id = #{id} AND deleted = 0 AND (#{count} >= 0 OR count + #{count} >= 0)")
    int updateCountIncrementNonNegative(@Param("id") Long id, @Param("count") BigDecimal count);

    /**
     * 增加库存数量（允许负库存）
     */
    @Update("UPDATE erp_stock SET count = count + #{count} WHERE id = #{id} AND deleted = 0")
    int updateCountIncrementAllowNegative(@Param("id") Long id, @Param("count") BigDecimal count);

    /**
     * 增量更新库存数量。参数化避免 SQL 拼接。
     *
     * @param id 库存记录编号
     * @param count 增量数量（正数增加，负数减少）
     * @param negativeEnable 是否允许负库存
     * @return 影响行数（0 表示库存不足）
     */
    default int updateCountIncrement(Long id, BigDecimal count, boolean negativeEnable) {
        if (count == null || count.compareTo(BigDecimal.ZERO) == 0) {
            return 1;
        }
        if (negativeEnable) {
            return updateCountIncrementAllowNegative(id, count);
        }
        return updateCountIncrementNonNegative(id, count);
    }

    /**
     * 按产品编号查询所有仓库的库存记录（用于跨仓库锁定/扣减）
     *
     * @param productId 产品编号
     * @return 库存记录列表
     */
    default List<ErpStockDO> selectListByProductId(Long productId) {
        return selectList(ErpStockDO::getProductId, productId);
    }

    /**
     * 锁定库存（参数化）：locked_count += count，且校验可用库存充足
     */
    @Update("UPDATE erp_stock SET locked_count = locked_count + #{count} " +
            "WHERE id = #{id} AND deleted = 0 AND count - locked_count >= #{count}")
    int lockLockedCountIncrement(@Param("id") Long id, @Param("count") BigDecimal count);

    /**
     * 释放锁定库存（参数化）：locked_count -= |count|，且不低于 0
     */
    @Update("UPDATE erp_stock SET locked_count = locked_count - #{absCount} " +
            "WHERE id = #{id} AND deleted = 0 AND locked_count >= #{absCount}")
    int releaseLockedCountDecrement(@Param("id") Long id, @Param("absCount") BigDecimal absCount);

    /**
     * 增量更新锁定数量。参数化避免 SQL 拼接。
     *
     * @param id 库存记录编号
     * @param count 增量数量：正数表示锁定；负数表示释放
     * @return 影响行数
     */
    default int updateLockedCountIncrement(Long id, BigDecimal count) {
        if (count == null || count.compareTo(BigDecimal.ZERO) == 0) {
            return 1;
        }
        if (count.compareTo(BigDecimal.ZERO) > 0) {
            return lockLockedCountIncrement(id, count);
        }
        return releaseLockedCountDecrement(id, count.abs());
    }

    /**
     * 按产品汇总库存数量（COALESCE 保证无记录时返回 0，SUM 直接映射 BigDecimal 避免浮点精度丢失）
     *
     * @param productId 产品编号
     * @return 库存总数量
     */
    @Select("SELECT COALESCE(SUM(count), 0) FROM erp_stock WHERE product_id = #{productId} AND deleted = 0")
    BigDecimal selectSumByProductId(@Param("productId") Long productId);

}
