package cn.iocoder.yudao.module.erp.service.production.mps;

import cn.iocoder.yudao.module.erp.dal.dataobject.production.mps.ErpMpsPlanDetailDO;
import cn.iocoder.yudao.module.erp.dal.mysql.production.mps.ErpMpsPlanDetailMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.MPS_PLAN_DETAIL_NOT_EXISTS;

/**
 * ERP 主生产计划明细 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpMpsPlanDetailServiceImpl implements ErpMpsPlanDetailService {

    @Resource
    private ErpMpsPlanDetailMapper mpsPlanDetailMapper;

    @Override
    public List<ErpMpsPlanDetailDO> getMpsPlanDetailListByPlanId(Long planId) {
        return mpsPlanDetailMapper.selectListByPlanId(planId);
    }

    @Override
    public ErpMpsPlanDetailDO validateMpsPlanDetail(Long id) {
        ErpMpsPlanDetailDO detail = mpsPlanDetailMapper.selectById(id);
        if (detail == null) {
            throw exception(MPS_PLAN_DETAIL_NOT_EXISTS);
        }
        return detail;
    }

}