package cn.zhicloud.module.pay.api.refund;

import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.pay.api.refund.dto.PayRefundCreateReqDTO;
import cn.zhicloud.module.pay.api.refund.dto.PayRefundRespDTO;
import cn.zhicloud.module.pay.dal.dataobject.refund.PayRefundDO;
import cn.zhicloud.module.pay.service.refund.PayRefundService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * 退款单 API 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class PayRefundApiImpl implements PayRefundApi {

    @Resource
    private PayRefundService payRefundService;

    @Override
    public Long createRefund(PayRefundCreateReqDTO reqDTO) {
        return payRefundService.createRefund(reqDTO);
    }

    @Override
    public PayRefundRespDTO getRefund(Long id) {
        PayRefundDO refund = payRefundService.getRefund(id);
        return BeanUtils.toBean(refund, PayRefundRespDTO.class);
    }

}
