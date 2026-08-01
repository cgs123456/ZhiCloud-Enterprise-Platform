package cn.iocoder.yudao.module.wms.service.order.asn;

import cn.iocoder.yudao.module.wms.controller.admin.order.asn.vo.WmsAsnOrderDetailSaveReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.asn.WmsAsnOrderDetailDO;

import java.util.List;

/**
 * WMS ASN 到货通知单明细 Service 接口
 *
 * @author 芋道源码
 */
public interface WmsAsnOrderDetailService {

    /**
     * 创建 ASN 明细列表
     *
     * @param asnOrderId ASN 单编号
     * @param details 明细列表
     */
    void createAsnOrderDetailList(Long asnOrderId, List<WmsAsnOrderDetailSaveReqVO> details);

    /**
     * 更新 ASN 明细列表（先删后增）
     *
     * @param asnOrderId ASN 单编号
     * @param details 明细列表
     */
    void updateAsnOrderDetailList(Long asnOrderId, List<WmsAsnOrderDetailSaveReqVO> details);

    /**
     * 删除指定 ASN 单的明细
     *
     * @param asnOrderId ASN 单编号
     */
    void deleteAsnOrderDetailListByAsnOrderId(Long asnOrderId);

    /**
     * 获取指定 ASN 单的明细列表
     *
     * @param asnOrderId ASN 单编号
     * @return 明细列表
     */
    List<WmsAsnOrderDetailDO> getAsnOrderDetailList(Long asnOrderId);

    /**
     * 累加已收数量
     *
     * @param detailId 明细编号
     * @param receivedQuantity 本次收货数量
     */
    void addReceivedQuantity(Long detailId, java.math.BigDecimal receivedQuantity);

}
