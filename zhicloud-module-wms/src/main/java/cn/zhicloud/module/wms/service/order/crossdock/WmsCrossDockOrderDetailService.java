package cn.zhicloud.module.wms.service.order.crossdock;

import cn.zhicloud.module.wms.controller.admin.order.crossdock.vo.WmsCrossDockOrderSaveReqVO;
import cn.zhicloud.module.wms.dal.dataobject.order.crossdock.WmsCrossDockOrderDetailDO;

import java.util.Collection;
import java.util.List;

/**
 * WMS 越库单明细 Service 接口
 *
 * @author 智云
 */
public interface WmsCrossDockOrderDetailService {

    /**
     * 创建越库单明细列表
     *
     * @param orderId 越库单编号
     * @param reqVO 越库单保存信息
     */
    void createCrossDockOrderDetailList(Long orderId, WmsCrossDockOrderSaveReqVO reqVO);

    /**
     * 更新越库单明细列表
     *
     * @param orderId 越库单编号
     * @param reqVO 越库单保存信息
     */
    void updateCrossDockOrderDetailList(Long orderId, WmsCrossDockOrderSaveReqVO reqVO);

    /**
     * 按越库单编号删除明细列表
     *
     * @param orderId 越库单编号
     */
    void deleteCrossDockOrderDetailListByOrderId(Long orderId);

    /**
     * 按越库单编号获得明细列表
     *
     * @param orderId 越库单编号
     * @return 明细列表
     */
    List<WmsCrossDockOrderDetailDO> getCrossDockOrderDetailList(Long orderId);

    /**
     * 按越库单编号集合获得明细列表
     *
     * @param orderIds 越库单编号集合
     * @return 明细列表
     */
    List<WmsCrossDockOrderDetailDO> getCrossDockOrderDetailList(Collection<Long> orderIds);

    /**
     * 校验越库单明细列表存在
     *
     * @param orderId 越库单编号
     * @return 明细列表
     */
    List<WmsCrossDockOrderDetailDO> validateCrossDockOrderDetailListExists(Long orderId);

}
