package cn.zhicloud.module.erp.service.production.mps;

import cn.zhicloud.module.erp.dal.dataobject.production.mps.ErpMpsPlanDetailDO;
import cn.zhicloud.module.erp.dal.mysql.production.mps.ErpMpsPlanDetailMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.MPS_PLAN_DETAIL_NOT_EXISTS;

/**
 * ERP 主生产计划明细 Service 实现类
 *
 * @author 智云
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