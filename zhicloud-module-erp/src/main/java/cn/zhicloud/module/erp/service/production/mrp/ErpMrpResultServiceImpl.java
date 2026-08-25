package cn.zhicloud.module.erp.service.production.mrp;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.erp.controller.admin.production.mrp.vo.ErpMrpResultPageReqVO;
import cn.zhicloud.module.erp.dal.dataobject.production.mrp.ErpMrpResultDO;
import cn.zhicloud.module.erp.dal.mysql.production.mrp.ErpMrpResultMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * ERP 物料需求计划结果 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class ErpMrpResultServiceImpl implements ErpMrpResultService {

    @Resource
    private ErpMrpResultMapper mrpResultMapper;

    @Override
    public ErpMrpResultDO getMrpResult(Long id) {
        return mrpResultMapper.selectById(id);
    }

    @Override
    public PageResult<ErpMrpResultDO> getMrpResultPageByPlanId(ErpMrpResultPageReqVO pageReqVO) {
        return mrpResultMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpMrpResultDO> getMrpResultListByPlanId(Long planId) {
        return mrpResultMapper.selectListByPlanId(planId);
    }

}
