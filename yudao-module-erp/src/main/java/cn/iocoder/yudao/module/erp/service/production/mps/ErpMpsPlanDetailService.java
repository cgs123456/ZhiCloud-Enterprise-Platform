package cn.iocoder.yudao.module.erp.service.production.mps;

import cn.iocoder.yudao.module.erp.dal.dataobject.production.mps.ErpMpsPlanDetailDO;

import java.util.List;

/**
 * ERP 主生产计划明细 Service 接口
 *
 * @author 芋道源码
 */
public interface ErpMpsPlanDetailService {

    /**
     * 获得主生产计划明细列表
     *
     * @param planId 计划编号
     * @return 明细列表
     */
    List<ErpMpsPlanDetailDO> getMpsPlanDetailListByPlanId(Long planId);

    /**
     * 校验主生产计划明细存在
     *
     * @param id 编号
     * @return 明细
     */
    ErpMpsPlanDetailDO validateMpsPlanDetail(Long id);

}