package cn.iocoder.yudao.module.wms.service.order.asn;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.wms.controller.admin.order.asn.vo.WmsAsnOrderPageReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.order.asn.vo.WmsAsnOrderSaveReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.asn.WmsAsnOrderDO;
import jakarta.validation.Valid;

/**
 * WMS ASN 到货通知单 Service 接口
 *
 * @author 芋道源码
 */
public interface WmsAsnOrderService {

    /**
     * 创建 ASN 到货通知单
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createAsnOrder(@Valid WmsAsnOrderSaveReqVO createReqVO);

    /**
     * 更新 ASN 到货通知单
     *
     * @param updateReqVO 更新信息
     */
    void updateAsnOrder(@Valid WmsAsnOrderSaveReqVO updateReqVO);

    /**
     * 删除 ASN 到货通知单
     *
     * @param id 编号
     */
    void deleteAsnOrder(Long id);

    /**
     * 关闭 ASN 到货通知单
     *
     * @param id 编号
     */
    void closeAsnOrder(Long id);

    /**
     * 确认到货
     *
     * <p>将 ASN 状态从「待到货」流转为「已到货」，记录实际到货时间，并将月台置为占用。
     *
     * @param id 编号
     */
    void confirmArrival(Long id);

    /**
     * 转收货单
     *
     * <p>将已到货的 ASN 转换为入库收货单，ASN 状态流转为「已收货」。
     *
     * @param id 编号
     * @return 收货单编号
     */
    Long convertToReceipt(Long id);

    /**
     * 获得 ASN 到货通知单
     *
     * @param id 编号
     * @return ASN 到货通知单
     */
    WmsAsnOrderDO getAsnOrder(Long id);

    /**
     * 根据 ASN 编号查询
     *
     * @param no ASN 编号
     * @return ASN 到货通知单
     */
    WmsAsnOrderDO getAsnOrderByNo(String no);

    /**
     * 获得 ASN 到货通知单分页
     *
     * @param pageReqVO 分页查询
     * @return ASN 到货通知单分页
     */
    PageResult<WmsAsnOrderDO> getAsnOrderPage(WmsAsnOrderPageReqVO pageReqVO);

}
