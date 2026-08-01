package cn.iocoder.yudao.module.tms.service.carrier;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.tms.controller.admin.carrier.vo.TmsCarrierPageReqVO;
import cn.iocoder.yudao.module.tms.controller.admin.carrier.vo.TmsCarrierSaveReqVO;
import cn.iocoder.yudao.module.tms.dal.dataobject.carrier.TmsCarrierDO;
import jakarta.validation.Valid;

/**
 * TMS 承运商 Service 接口
 *
 * @author yudao
 */
public interface TmsCarrierService {

    Long createCarrier(@Valid TmsCarrierSaveReqVO createReqVO);

    void updateCarrier(@Valid TmsCarrierSaveReqVO updateReqVO);

    void deleteCarrier(Long id);

    TmsCarrierDO getCarrier(Long id);

    PageResult<TmsCarrierDO> getCarrierPage(TmsCarrierPageReqVO pageReqVO);

    /**
     * 校验承运商是否存在
     *
     * @param id 编号
     */
    void validateCarrierExists(Long id);

}
