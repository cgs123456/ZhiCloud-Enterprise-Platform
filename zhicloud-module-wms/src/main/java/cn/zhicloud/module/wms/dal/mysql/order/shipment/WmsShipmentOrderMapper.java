package cn.zhicloud.module.wms.dal.mysql.order.shipment;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.wms.controller.admin.order.shipment.vo.order.WmsShipmentOrderPageReqVO;
import cn.zhicloud.module.wms.dal.dataobject.order.shipment.WmsShipmentOrderDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * WMS 出库单 Mapper
 *
 * @author 智云
 */
@Mapper
public interface WmsShipmentOrderMapper extends BaseMapperX<WmsShipmentOrderDO> {

    default PageResult<WmsShipmentOrderDO> selectPage(WmsShipmentOrderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<WmsShipmentOrderDO>()
                .likeIfPresent(WmsShipmentOrderDO::getNo, reqVO.getNo())
                .eqIfPresent(WmsShipmentOrderDO::getStatus, reqVO.getStatus())
                .eqIfPresent(WmsShipmentOrderDO::getWarehouseId, reqVO.getWarehouseId())
                .eqIfPresent(WmsShipmentOrderDO::getMerchantId, reqVO.getMerchantId())
                .betweenIfPresent(WmsShipmentOrderDO::getOrderTime, reqVO.getOrderTime())
                .geIfPresent(WmsShipmentOrderDO::getTotalQuantity, reqVO.getTotalQuantityMin())
                .leIfPresent(WmsShipmentOrderDO::getTotalQuantity, reqVO.getTotalQuantityMax())
                .geIfPresent(WmsShipmentOrderDO::getTotalPrice, reqVO.getTotalPriceMin())
                .leIfPresent(WmsShipmentOrderDO::getTotalPrice, reqVO.getTotalPriceMax())
                .eqIfPresent(WmsShipmentOrderDO::getType, reqVO.getType())
                .likeIfPresent(WmsShipmentOrderDO::getBizOrderNo, reqVO.getBizOrderNo())
                .eqIfPresent(WmsShipmentOrderDO::getCreator, reqVO.getCreator())
                .eqIfPresent(WmsShipmentOrderDO::getUpdater, reqVO.getUpdater())
                .betweenIfPresent(WmsShipmentOrderDO::getCreateTime, reqVO.getCreateTime())
                .betweenIfPresent(WmsShipmentOrderDO::getUpdateTime, reqVO.getUpdateTime())
                .orderByDesc(WmsShipmentOrderDO::getId));
    }

    default WmsShipmentOrderDO selectByNo(String no) {
        return selectOne(WmsShipmentOrderDO::getNo, no);
    }

    /**
     * 按业务单号查出库单（V101：ERP -> WMS 链路的业务级幂等查重）。
     * <p>Inbox 唯一键挡的是「同一事件重复投递」，本方法挡的是「不同事件指向同一业务单据」，
     * 例如人工重放后又来了一条修正事件；两层都要有，缺一层就会建出重复出库单。
     */
    default WmsShipmentOrderDO selectByBizOrderNo(String bizOrderNo) {
        return selectOne(WmsShipmentOrderDO::getBizOrderNo, bizOrderNo);
    }
    default Long selectCountByMerchantId(Long merchantId) {
        return selectCount(WmsShipmentOrderDO::getMerchantId, merchantId);
    }

    default Long selectCountByWarehouseId(Long warehouseId) {
        return selectCount(WmsShipmentOrderDO::getWarehouseId, warehouseId);
    }

    default int updateByIdAndStatus(Long id, Integer status, WmsShipmentOrderDO updateObj) {
        return update(updateObj, new LambdaQueryWrapper<WmsShipmentOrderDO>()
                .eq(WmsShipmentOrderDO::getId, id)
                .eq(WmsShipmentOrderDO::getStatus, status));
    }

}
