package cn.zhicloud.module.wms.service.billing;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.wms.controller.admin.billing.vo.contract.WmsBillingContractPageReqVO;
import cn.zhicloud.module.wms.controller.admin.billing.vo.contract.WmsBillingContractSaveReqVO;
import cn.zhicloud.module.wms.dal.dataobject.billing.WmsBillingContractDO;
import cn.zhicloud.module.wms.dal.dataobject.billing.WmsBillingContractItemDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * WMS 3PL 计费合同 Service 接口
 *
 * @author 智云
 */
public interface WmsBillingContractService {

    /**
     * 创建计费合同
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createBillingContract(@Valid WmsBillingContractSaveReqVO createReqVO);

    /**
     * 更新计费合同
     *
     * @param updateReqVO 更新信息
     */
    void updateBillingContract(@Valid WmsBillingContractSaveReqVO updateReqVO);

    /**
     * 删除计费合同
     *
     * @param id 编号
     */
    void deleteBillingContract(Long id);

    /**
     * 获得计费合同
     *
     * @param id 编号
     * @return 计费合同
     */
    WmsBillingContractDO getBillingContract(Long id);

    /**
     * 获得计费合同分页
     *
     * @param pageReqVO 分页查询
     * @return 计费合同分页
     */
    PageResult<WmsBillingContractDO> getBillingContractPage(WmsBillingContractPageReqVO pageReqVO);

    /**
     * 校验计费合同存在
     *
     * @param id 编号
     * @return 计费合同
     */
    WmsBillingContractDO validateBillingContractExists(Long id);

    /**
     * 获得计费合同条款列表
     *
     * @param contractId 合同编号
     * @return 条款列表
     */
    List<WmsBillingContractItemDO> getContractItemList(Long contractId);

}
