package cn.iocoder.yudao.module.tms.service.carrier;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.tms.controller.admin.carrier.vo.TmsCarrierPageReqVO;
import cn.iocoder.yudao.module.tms.controller.admin.carrier.vo.TmsCarrierSaveReqVO;
import cn.iocoder.yudao.module.tms.dal.dataobject.carrier.TmsCarrierDO;
import cn.iocoder.yudao.module.tms.dal.mysql.carrier.TmsCarrierMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.tms.enums.ErrorCodeConstants.TMS_CARRIER_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.tms.enums.ErrorCodeConstants.TMS_CARRIER_NOT_EXISTS;

/**
 * TMS 承运商 Service 实现类
 *
 * @author yudao
 */
@Service
@Validated
public class TmsCarrierServiceImpl implements TmsCarrierService {

    @Resource
    private TmsCarrierMapper carrierMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCarrier(TmsCarrierSaveReqVO createReqVO) {
        validateCodeUnique(null, createReqVO.getCode());
        TmsCarrierDO carrier = BeanUtils.toBean(createReqVO, TmsCarrierDO.class);
        carrierMapper.insert(carrier);
        return carrier.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCarrier(TmsCarrierSaveReqVO updateReqVO) {
        validateCarrierExists(updateReqVO.getId());
        validateCodeUnique(updateReqVO.getId(), updateReqVO.getCode());
        TmsCarrierDO updateObj = BeanUtils.toBean(updateReqVO, TmsCarrierDO.class);
        carrierMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCarrier(Long id) {
        validateCarrierExists(id);
        carrierMapper.deleteById(id);
    }

    @Override
    public TmsCarrierDO getCarrier(Long id) {
        return carrierMapper.selectById(id);
    }

    @Override
    public PageResult<TmsCarrierDO> getCarrierPage(TmsCarrierPageReqVO pageReqVO) {
        return carrierMapper.selectPage(pageReqVO);
    }

    @Override
    public void validateCarrierExists(Long id) {
        if (carrierMapper.selectById(id) == null) {
            throw exception(TMS_CARRIER_NOT_EXISTS);
        }
    }

    private void validateCodeUnique(Long id, String code) {
        if (code == null) {
            return;
        }
        TmsCarrierDO carrier = carrierMapper.selectByCode(code);
        if (carrier == null) {
            return;
        }
        if (id == null || !carrier.getId().equals(id)) {
            throw exception(TMS_CARRIER_CODE_DUPLICATE);
        }
    }

}
