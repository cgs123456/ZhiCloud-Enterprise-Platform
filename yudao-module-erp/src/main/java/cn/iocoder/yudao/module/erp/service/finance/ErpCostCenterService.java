package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.costcenter.ErpCostCenterPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.costcenter.ErpCostCenterSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpCostCenterDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * ERP 成本中心 Service 接口
 *
 * @author 芋道源码
 */
public interface ErpCostCenterService {

    /**
     * 创建成本中心
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createCostCenter(@Valid ErpCostCenterSaveReqVO createReqVO);

    /**
     * 更新成本中心
     *
     * @param updateReqVO 更新信息
     */
    void updateCostCenter(@Valid ErpCostCenterSaveReqVO updateReqVO);

    /**
     * 删除成本中心
     *
     * @param id 编号
     */
    void deleteCostCenter(Long id);

    /**
     * 获得成本中心
     *
     * @param id 编号
     * @return 成本中心
     */
    ErpCostCenterDO getCostCenter(Long id);

    /**
     * 获得成本中心分页
     *
     * @param pageReqVO 分页查询
     * @return 成本中心分页
     */
    PageResult<ErpCostCenterDO> getCostCenterPage(ErpCostCenterPageReqVO pageReqVO);

    /**
     * 获得所有成本中心列表
     *
     * @return 成本中心列表
     */
    List<ErpCostCenterDO> getCostCenterList();

}
