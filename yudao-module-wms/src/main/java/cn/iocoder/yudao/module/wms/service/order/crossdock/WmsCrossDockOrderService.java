package cn.iocoder.yudao.module.wms.service.order.crossdock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.wms.controller.admin.order.crossdock.vo.WmsCrossDockOrderPageReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.order.crossdock.vo.WmsCrossDockOrderSaveReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.crossdock.WmsCrossDockOrderDO;
import jakarta.validation.Valid;

/**
 * WMS 越库单 Service 接口
 *
 * @author 芋道源码
 */
public interface WmsCrossDockOrderService {

    /**
     * 创建越库单
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createCrossDockOrder(@Valid WmsCrossDockOrderSaveReqVO createReqVO);

    /**
     * 更新越库单
     *
     * @param updateReqVO 更新信息
     */
    void updateCrossDockOrder(@Valid WmsCrossDockOrderSaveReqVO updateReqVO);

    /**
     * 删除越库单
     *
     * @param id 编号
     */
    void deleteCrossDockOrder(Long id);

    /**
     * 获得越库单
     *
     * @param id 编号
     * @return 越库单
     */
    WmsCrossDockOrderDO getCrossDockOrder(Long id);

    /**
     * 获得越库单分页
     *
     * @param pageReqVO 分页查询
     * @return 越库单分页
     */
    PageResult<WmsCrossDockOrderDO> getCrossDockOrderPage(WmsCrossDockOrderPageReqVO pageReqVO);

    /**
     * 确认收货
     *
     * 越库场景：跳过上架，直接分配至出库月台。状态由 待收货 流转至 已分配。
     *
     * @param id 编号
     */
    void confirmReceipt(Long id);

    /**
     * 完成越库单
     *
     * @param id 编号
     */
    void completeCrossDockOrder(Long id);

}
