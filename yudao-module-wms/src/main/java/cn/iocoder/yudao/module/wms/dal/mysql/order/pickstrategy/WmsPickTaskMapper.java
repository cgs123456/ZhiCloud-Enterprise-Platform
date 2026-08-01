package cn.iocoder.yudao.module.wms.dal.mysql.order.pickstrategy;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.wms.controller.admin.order.pickstrategy.vo.WmsPickTaskPageReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.pickstrategy.WmsPickTaskDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * WMS 拣货任务 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface WmsPickTaskMapper extends BaseMapperX<WmsPickTaskDO> {

    default PageResult<WmsPickTaskDO> selectPage(WmsPickTaskPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<WmsPickTaskDO>()
                .likeIfPresent(WmsPickTaskDO::getTaskNo, reqVO.getTaskNo())
                .eqIfPresent(WmsPickTaskDO::getShipmentOrderId, reqVO.getShipmentOrderId())
                .eqIfPresent(WmsPickTaskDO::getWaveOrderId, reqVO.getWaveOrderId())
                .eqIfPresent(WmsPickTaskDO::getSkuId, reqVO.getSkuId())
                .eqIfPresent(WmsPickTaskDO::getStatus, reqVO.getStatus())
                .eqIfPresent(WmsPickTaskDO::getPickerUserId, reqVO.getPickerUserId())
                .orderByAsc(WmsPickTaskDO::getPickSequence)
                .orderByDesc(WmsPickTaskDO::getId));
    }

    default List<WmsPickTaskDO> selectListByShipmentOrderId(Long shipmentOrderId) {
        return selectList(WmsPickTaskDO::getShipmentOrderId, shipmentOrderId);
    }

    default List<WmsPickTaskDO> selectListByWaveOrderId(Long waveOrderId) {
        return selectList(WmsPickTaskDO::getWaveOrderId, waveOrderId);
    }

    default List<WmsPickTaskDO> selectListByPickerUserId(Long pickerUserId) {
        return selectList(new LambdaQueryWrapperX<WmsPickTaskDO>()
                .eq(WmsPickTaskDO::getPickerUserId, pickerUserId)
                .orderByAsc(WmsPickTaskDO::getPickSequence)
                .orderByAsc(WmsPickTaskDO::getId));
    }

    default WmsPickTaskDO selectByTaskNo(String taskNo) {
        return selectOne(WmsPickTaskDO::getTaskNo, taskNo);
    }

    default Long selectCountByShipmentOrderId(Long shipmentOrderId) {
        return selectCount(WmsPickTaskDO::getShipmentOrderId, shipmentOrderId);
    }

    default void deleteByShipmentOrderId(Long shipmentOrderId) {
        delete(WmsPickTaskDO::getShipmentOrderId, shipmentOrderId);
    }

    default List<WmsPickTaskDO> selectListByShipmentOrderIds(Collection<Long> shipmentOrderIds) {
        return selectList(new LambdaQueryWrapperX<WmsPickTaskDO>()
                .inIfPresent(WmsPickTaskDO::getShipmentOrderId, shipmentOrderIds)
                .orderByAsc(WmsPickTaskDO::getShipmentOrderId)
                .orderByAsc(WmsPickTaskDO::getPickSequence));
    }

}
