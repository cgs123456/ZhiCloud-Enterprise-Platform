package cn.iocoder.yudao.module.hr.service.contract;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.hr.controller.admin.contract.vo.HrContractPageReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.contract.vo.HrContractRenewReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.contract.vo.HrContractSaveReqVO;
import cn.iocoder.yudao.module.hr.dal.dataobject.contract.HrContractDO;
import cn.iocoder.yudao.module.hr.dal.mysql.contract.HrContractMapper;
import cn.iocoder.yudao.module.hr.enums.contract.HrContractStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hr.enums.ErrorCodeConstants.*;

@Service
@Validated
public class HrContractServiceImpl implements HrContractService {

    @Resource
    private HrContractMapper contractMapper;

    @Override
    public Long createContract(HrContractSaveReqVO createReqVO) {
        validateContractNoUnique(null, createReqVO.getContractNo());
        HrContractDO contract = BeanUtils.toBean(createReqVO, HrContractDO.class);
        if (contract.getStatus() == null) {
            contract.setStatus(HrContractStatusEnum.EFFECTIVE.getStatus());
        }
        contractMapper.insert(contract);
        return contract.getId();
    }

    @Override
    public void updateContract(HrContractSaveReqVO updateReqVO) {
        validateContractExists(updateReqVO.getId());
        validateContractNoUnique(updateReqVO.getId(), updateReqVO.getContractNo());
        HrContractDO updateObj = BeanUtils.toBean(updateReqVO, HrContractDO.class);
        contractMapper.updateById(updateObj);
    }

    @Override
    public void deleteContract(Long id) {
        validateContractExists(id);
        contractMapper.deleteById(id);
    }

    private HrContractDO validateContractExists(Long id) {
        HrContractDO contract = contractMapper.selectById(id);
        if (contract == null) {
            throw exception(HR_CONTRACT_NOT_EXISTS);
        }
        return contract;
    }

    private void validateContractNoUnique(Long id, String contractNo) {
        HrContractDO contract = contractMapper.selectByContractNo(contractNo);
        if (contract == null) {
            return;
        }
        if (id == null || !contract.getId().equals(id)) {
            throw exception(HR_CONTRACT_NO_EXISTS);
        }
    }

    @Override
    public HrContractDO getContract(Long id) {
        return contractMapper.selectById(id);
    }

    @Override
    public List<HrContractDO> getContractList(Collection<Long> ids) {
        return contractMapper.selectByIds(ids);
    }

    @Override
    public PageResult<HrContractDO> getContractPage(HrContractPageReqVO pageReqVO) {
        return contractMapper.selectPage(pageReqVO);
    }

    @Override
    public Long renewContract(HrContractRenewReqVO reqVO) {
        HrContractDO oldContract = validateContractExists(reqVO.getId());
        HrContractDO updateOld = new HrContractDO();
        updateOld.setId(oldContract.getId());
        updateOld.setStatus(HrContractStatusEnum.RENEWED.getStatus());
        contractMapper.updateById(updateOld);
        HrContractDO newContract = new HrContractDO();
        newContract.setEmployeeId(oldContract.getEmployeeId());
        newContract.setContractNo(oldContract.getContractNo() + "-R");
        newContract.setContractType(oldContract.getContractType());
        newContract.setStartDate(reqVO.getNewStartDate());
        newContract.setEndDate(reqVO.getNewEndDate());
        newContract.setSignDate(LocalDate.now());
        newContract.setProbationEndDate(oldContract.getProbationEndDate());
        newContract.setPositionId(oldContract.getPositionId());
        newContract.setDepartmentId(oldContract.getDepartmentId());
        newContract.setSalary(reqVO.getNewSalary() != null ? reqVO.getNewSalary() : oldContract.getSalary());
        newContract.setStatus(HrContractStatusEnum.EFFECTIVE.getStatus());
        newContract.setFileUrl(reqVO.getNewFileUrl());
        newContract.setRemark(reqVO.getRemark());
        contractMapper.insert(newContract);
        return newContract.getId();
    }

    @Override
    public void terminateContract(Long id) {
        HrContractDO contract = validateContractExists(id);
        if (HrContractStatusEnum.TERMINATED.getStatus().equals(contract.getStatus())) {
            throw exception(HR_CONTRACT_ALREADY_TERMINATED);
        }
        HrContractDO updateObj = new HrContractDO();
        updateObj.setId(id);
        updateObj.setStatus(HrContractStatusEnum.TERMINATED.getStatus());
        contractMapper.updateById(updateObj);
    }

    @Override
    public List<HrContractDO> getExpiringContracts(int days) {
        LocalDate deadline = LocalDate.now().plusDays(days);
        return contractMapper.selectListByExpiring(deadline);
    }

    @Override
    public void markExpiring(Long id) {
        HrContractDO contract = validateContractExists(id);
        if (!HrContractStatusEnum.EFFECTIVE.getStatus().equals(contract.getStatus())) {
            return;
        }
        HrContractDO updateObj = new HrContractDO();
        updateObj.setId(id);
        updateObj.setStatus(HrContractStatusEnum.EXPIRING.getStatus());
        contractMapper.updateById(updateObj);
    }

}