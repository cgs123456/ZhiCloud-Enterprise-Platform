package cn.iocoder.yudao.module.wms.service.order.asn;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.wms.controller.admin.order.asn.vo.WmsAsnOrderDetailSaveReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.asn.WmsAsnOrderDetailDO;
import cn.iocoder.yudao.module.wms.dal.mysql.order.asn.WmsAsnOrderDetailMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.ASN_ORDER_DETAIL_NOT_EXISTS;

/**
 * WMS ASN 到货通知单明细 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class WmsAsnOrderDetailServiceImpl implements WmsAsnOrderDetailService {

    @Resource
    private WmsAsnOrderDetailMapper asnOrderDetailMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createAsnOrderDetailList(Long asnOrderId, List<WmsAsnOrderDetailSaveReqVO> details) {
        if (CollUtil.isEmpty(details)) {
            return;
        }
        List<WmsAsnOrderDetailDO> detailDOs = BeanUtils.toBean(details, WmsAsnOrderDetailDO.class);
        for (WmsAsnOrderDetailDO detail : detailDOs) {
            detail.setId(null).setAsnOrderId(asnOrderId).setReceivedQuantity(BigDecimal.ZERO);
        }
        asnOrderDetailMapper.insertBatch(detailDOs);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAsnOrderDetailList(Long asnOrderId, List<WmsAsnOrderDetailSaveReqVO> details) {
        // 1. 先删除旧明细
        deleteAsnOrderDetailListByAsnOrderId(asnOrderId);
        // 2. 再创建新明细
        createAsnOrderDetailList(asnOrderId, details);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAsnOrderDetailListByAsnOrderId(Long asnOrderId) {
        asnOrderDetailMapper.deleteByAsnOrderId(asnOrderId);
    }

    @Override
    public List<WmsAsnOrderDetailDO> getAsnOrderDetailList(Long asnOrderId) {
        return asnOrderDetailMapper.selectListByAsnOrderId(asnOrderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addReceivedQuantity(Long detailId, BigDecimal receivedQuantity) {
        WmsAsnOrderDetailDO detail = asnOrderDetailMapper.selectById(detailId);
        if (detail == null) {
            throw exception(ASN_ORDER_DETAIL_NOT_EXISTS);
        }
        BigDecimal current = detail.getReceivedQuantity() == null ? BigDecimal.ZERO : detail.getReceivedQuantity();
        WmsAsnOrderDetailDO updateObj = new WmsAsnOrderDetailDO();
        updateObj.setId(detailId);
        updateObj.setReceivedQuantity(current.add(receivedQuantity));
        asnOrderDetailMapper.updateById(updateObj);
    }

}
