package cn.iocoder.yudao.module.hr.service.contract;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.hr.controller.admin.contract.vo.HrContractPageReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.contract.vo.HrContractRenewReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.contract.vo.HrContractSaveReqVO;
import cn.iocoder.yudao.module.hr.dal.dataobject.contract.HrContractDO;
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