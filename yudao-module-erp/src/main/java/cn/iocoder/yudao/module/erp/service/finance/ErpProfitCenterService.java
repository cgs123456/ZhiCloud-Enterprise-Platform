package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.profitcenter.ErpProfitCenterPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.profitcenter.ErpProfitCenterSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpProfitCenterDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * ERP 利润中心 Service 接口
 *
 * @author 芋道源码
 */
public interface ErpProfitCenterService {

    /**
     * 创建利润中心
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createProfitCenter(@Valid ErpProfitCenterSaveReqVO createReqVO);

    /**
     * 更新利润中心
     *
     * @param updateReqVO 更新信息
     */
    void updateProfitCenter(@Valid ErpProfitCenterSaveReqVO updateReqVO);

    /**
     * 删除利润中心
     *
     * @param id 编号
     */
    void deleteProfitCenter(Long id);

    /**
     * 获得利润中心
     *
     * @param id 编号
     * @return 利润中心
     */
    ErpProfitCenterDO getProfitCenter(Long id);

    /**
     * 获得利润中心分页
     *
     * @param pageReqVO 分页查询
     * @return 利润中心分页
     */
    PageResult<ErpProfitCenterDO> getProfitCenterPage(ErpProfitCenterPageReqVO pageReqVO);

    /**
     * 获得所有利润中心列表
     *
     * @return 利润中心列表
     */
    List<ErpProfitCenterDO> getProfitCenterList();

}
