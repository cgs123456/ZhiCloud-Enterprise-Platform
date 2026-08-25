package cn.zhicloud.module.wms.dal.mysql.inventory;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.wms.controller.admin.inventory.alert.vo.WmsInventoryAlertPageReqVO;
import cn.zhicloud.module.wms.controller.admin.inventory.batch.vo.WmsBatchExpiryAlertPageReqVO;
import cn.zhicloud.module.wms.dal.dataobject.inventory.WmsInventoryAlertDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * WMS 库存预警 Mapper
 *
 * @author 智云
 */
@Mapper
public interface WmsInventoryAlertMapper extends BaseMapperX<WmsInventoryAlertDO> {

    default PageResult<WmsInventoryAlertDO> selectPage(WmsInventoryAlertPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<WmsInventoryAlertDO>()
                .eqIfPresent(WmsInventoryAlertDO::getAlertType, reqVO.getAlertType())
                .eqIfPresent(WmsInventoryAlertDO::getWarehouseId, reqVO.getWarehouseId())
                .eqIfPresent(WmsInventoryAlertDO::getProductId, reqVO.getProductId())
                .eqIfPresent(WmsInventoryAlertDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(WmsInventoryAlertDO::getAlertTime, reqVO.getAlertTime())
                .orderByDesc(WmsInventoryAlertDO::getId));
    }

    default List<WmsInventoryAlertDO> selectListByProductAndType(Long productId, String alertType, Integer status) {
        return selectList(new LambdaQueryWrapperX<WmsInventoryAlertDO>()
                .eq(WmsInventoryAlertDO::getProductId, productId)
                .eq(WmsInventoryAlertDO::getAlertType, alertType)
                .eqIfPresent(WmsInventoryAlertDO::getStatus, status)
                .orderByDesc(WmsInventoryAlertDO::getId));
    }

    default int deleteByAlertTimeBefore(LocalDateTime alertTime) {
        return delete(new LambdaQueryWrapperX<WmsInventoryAlertDO>()
                .lt(WmsInventoryAlertDO::getAlertTime, alertTime));
    }

    /**
     * 按预警类型集合分页查询（用于批次效期预警分页）
     *
     * @param reqVO 分页查询条件
     * @param alertTypes 预警类型集合
     * @return 分页结果
     */
    default PageResult<WmsInventoryAlertDO> selectPageByAlertTypes(WmsBatchExpiryAlertPageReqVO reqVO,
                                                                    Collection<String> alertTypes) {
        return selectPage(reqVO, new LambdaQueryWrapperX<WmsInventoryAlertDO>()
                .inIfPresent(WmsInventoryAlertDO::getAlertType, alertTypes)
                .eqIfPresent(WmsInventoryAlertDO::getWarehouseId, reqVO.getWarehouseId())
                .eqIfPresent(WmsInventoryAlertDO::getProductId, reqVO.getProductId())
                .likeIfPresent(WmsInventoryAlertDO::getBatchNo, reqVO.getBatchNo())
                .eqIfPresent(WmsInventoryAlertDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(WmsInventoryAlertDO::getAlertTime, reqVO.getAlertTime())
                .orderByDesc(WmsInventoryAlertDO::getId));
    }

}