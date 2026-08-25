package cn.zhicloud.module.erp.service.production.mrp;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.erp.controller.admin.production.mrp.vo.ErpMrpResultPageReqVO;
import cn.zhicloud.module.erp.dal.dataobject.production.mrp.ErpMrpResultDO;

import java.util.List;

/**
 * ERP 物料需求计划结果 Service 接口
 *
 * @author 智云
 */
public interface ErpMrpResultService {

    /**
     * 获得 MRP 结果
     *
     * @param id 编号
     * @return MRP 结果
     */
    ErpMrpResultDO getMrpResult(Long id);

    /**
     * 根据 MRP 计划编号分页查询结果
     *
     * @param pageReqVO 分页查询
     * @return MRP 结果分页
     */
    PageResult<ErpMrpResultDO> getMrpResultPageByPlanId(ErpMrpResultPageReqVO pageReqVO);

    /**
     * 根据 MRP 计划编号查询结果列表
     *
     * @param planId MRP 计划编号
     * @return MRP 结果列表
     */
    List<ErpMrpResultDO> getMrpResultListByPlanId(Long planId);

}
