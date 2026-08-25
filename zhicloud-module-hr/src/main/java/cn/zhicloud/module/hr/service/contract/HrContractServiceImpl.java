package cn.zhicloud.module.hr.service.contract;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.hr.controller.admin.contract.vo.HrContractPageReqVO;
import cn.zhicloud.module.hr.controller.admin.contract.vo.HrContractRenewReqVO;
import cn.zhicloud.module.hr.controller.admin.contract.vo.HrContractSaveReqVO;
import cn.zhicloud.module.hr.dal.dataobject.contract.HrContractDO;
import cn.zhicloud.module.hr.dal.mysql.contract.HrContractMapper;
import cn.zhicloud.module.hr.enums.contract.HrContractStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.hr.enums.ErrorCodeConstants.*;

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

    /**
     * 续签合同：将旧合同置为「已续签」并插入新合同。
     *
     * <p>两次写操作必须同事务——否则旧合同已改状态而新合同插入失败时，
     * 员工会出现「无有效合同」的脏数据且无法自动修复。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
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