package cn.zhicloud.module.hr.service.contract;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.hr.controller.admin.contract.vo.HrContractPageReqVO;
import cn.zhicloud.module.hr.controller.admin.contract.vo.HrContractRenewReqVO;
import cn.zhicloud.module.hr.controller.admin.contract.vo.HrContractSaveReqVO;
import cn.zhicloud.module.hr.dal.dataobject.contract.HrContractDO;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;

public interface HrContractService {

    Long createContract(@Valid HrContractSaveReqVO createReqVO);

    void updateContract(@Valid HrContractSaveReqVO updateReqVO);

    void deleteContract(Long id);

    HrContractDO getContract(Long id);

    List<HrContractDO> getContractList(Collection<Long> ids);

    PageResult<HrContractDO> getContractPage(HrContractPageReqVO pageReqVO);

    Long renewContract(@Valid HrContractRenewReqVO reqVO);

    void terminateContract(Long id);

    List<HrContractDO> getExpiringContracts(int days);

    void markExpiring(Long id);

}