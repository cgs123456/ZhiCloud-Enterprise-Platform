package cn.iocoder.yudao.module.wms.dal.mysql.inventory;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.wms.controller.admin.inventory.batch.vo.WmsInventoryBatchPageReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.inventory.WmsInventoryBatchDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * WMS 库存批次 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface WmsInventoryBatchMapper extends BaseMapperX<WmsInventoryBatchDO> {

    default PageResult<WmsInventoryBatchDO> selectPage(WmsInventoryBatchPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<WmsInventoryBatchDO>()
                .eqIfPresent(WmsInventoryBatchDO::getInventoryId, reqVO.getInventoryId())
                .likeIfPresent(WmsInventoryBatchDO::getBatchNo, reqVO.getBatchNo())
                .eqIfPresent(WmsInventoryBatchDO::getStatus, reqVO.getStatus())
                .geIfPresent(WmsInventoryBatchDO::getProductionDate, reqVO.getProductionDateStart())
                .leIfPresent(WmsInventoryBatchDO::getProductionDate, reqVO.getProductionDateEnd())
                .geIfPresent(WmsInventoryBatchDO::getExpiryDate, reqVO.getExpiryDateStart())
                .leIfPresent(WmsInventoryBatchDO::getExpiryDate, reqVO.getExpiryDateEnd())
                .orderByAsc(WmsInventoryBatchDO::getExpiryDate)
                .orderByAsc(WmsInventoryBatchDO::getId));
    }

    default List<WmsInventoryBatchDO> selectListByInventoryId(Long inventoryId) {
        return selectList(WmsInventoryBatchDO::getInventoryId, inventoryId);
    }

    default List<WmsInventoryBatchDO> selectListByInventoryIds(Collection<Long> inventoryIds) {
        return selectList(new LambdaQueryWrapperX<WmsInventoryBatchDO>()
                .inIfPresent(WmsInventoryBatchDO::getInventoryId, inventoryIds)
                .orderByAsc(WmsInventoryBatchDO::getExpiryDate));
    }

    default WmsInventoryBatchDO selectByInventoryIdAndBatchNo(Long inventoryId, String batchNo) {
        return selectOne(new LambdaQueryWrapperX<WmsInventoryBatchDO>()
                .eq(WmsInventoryBatchDO::getInventoryId, inventoryId)
                .eq(WmsInventoryBatchDO::getBatchNo, batchNo));
    }

    /**
     * 查询过期日期在指定日期之前（含）的可用批次
     *
     * @param expiryDate 过期日期上限
     * @return 批次列表
     */
    default List<WmsInventoryBatchDO> selectListExpiredBefore(LocalDate expiryDate) {
        return selectList(new LambdaQueryWrapperX<WmsInventoryBatchDO>()
                .le(WmsInventoryBatchDO::getExpiryDate, expiryDate)
                .orderByAsc(WmsInventoryBatchDO::getExpiryDate));
    }

    /**
     * 查询过期日期在指定日期区间内的批次（预警）
     *
     * @param startExpiryDate 过期日期下限（含）
     * @param endExpiryDate 过期日期上限（含）
     * @return 批次列表
     */
    default List<WmsInventoryBatchDO> selectListExpiringBetween(LocalDate startExpiryDate, LocalDate endExpiryDate) {
        return selectList(new LambdaQueryWrapperX<WmsInventoryBatchDO>()
                .ge(WmsInventoryBatchDO::getExpiryDate, startExpiryDate)
                .le(WmsInventoryBatchDO::getExpiryDate, endExpiryDate)
                .orderByAsc(WmsInventoryBatchDO::getExpiryDate));
    }

    /**
     * 查询所有设置了过期日期的批次（用于效期扫描）
     *
     * @return 批次列表
     */
    default List<WmsInventoryBatchDO> selectListWithExpiryDate() {
        return selectList(new LambdaQueryWrapperX<WmsInventoryBatchDO>()
                .isNotNull(WmsInventoryBatchDO::getExpiryDate)
                .orderByAsc(WmsInventoryBatchDO::getExpiryDate));
    }

    /**
     * 按批次号集合查询批次列表
     *
     * @param batchNos 批次号集合
     * @return 批次列表
     */
    default List<WmsInventoryBatchDO> selectListByBatchNos(Collection<String> batchNos) {
        if (batchNos == null || batchNos.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<WmsInventoryBatchDO>()
                .in(WmsInventoryBatchDO::getBatchNo, batchNos));
    }

}
