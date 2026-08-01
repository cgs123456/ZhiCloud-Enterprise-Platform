package cn.iocoder.yudao.module.hr.service.recruitment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.hr.controller.admin.employee.vo.HrEmployeeSaveReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.recruitment.vo.HrResumePageReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.recruitment.vo.HrResumeSaveReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.recruitment.vo.HrResumeScreenReqVO;
import cn.iocoder.yudao.module.hr.dal.dataobject.recruitment.HrResumeDO;
import cn.iocoder.yudao.module.hr.dal.mysql.recruitment.HrResumeMapper;
import cn.iocoder.yudao.module.hr.enums.employee.HrEmployeeStatusEnum;
import cn.iocoder.yudao.module.hr.enums.employee.HrEmploymentTypeEnum;
import cn.iocoder.yudao.module.hr.enums.employee.HrGenderEnum;
import cn.iocoder.yudao.module.hr.enums.recruitment.HrResumeStatusEnum;
import cn.iocoder.yudao.module.hr.service.employee.HrEmployeeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hr.enums.ErrorCodeConstants.*;

@Service
@Validated
public class HrResumeServiceImpl implements HrResumeService {

    @Resource
    private HrResumeMapper resumeMapper;
    @Resource
    private HrEmployeeService employeeService;

    @Override
    public Long createResume(HrResumeSaveReqVO createReqVO) {
        HrResumeDO resume = BeanUtils.toBean(createReqVO, HrResumeDO.class);
        resume.setStatus(HrResumeStatusEnum.PENDING.getStatus());
        resumeMapper.insert(resume);
        return resume.getId();
    }

    @Override
    public void updateResume(HrResumeSaveReqVO updateReqVO) {
        validateExists(updateReqVO.getId());
        HrResumeDO updateObj = BeanUtils.toBean(updateReqVO, HrResumeDO.class);
        resumeMapper.updateById(updateObj);
    }

    @Override
    public void deleteResume(Long id) {
        validateExists(id);
        resumeMapper.deleteById(id);
    }

    private HrResumeDO validateExists(Long id) {
        HrResumeDO resume = resumeMapper.selectById(id);
        if (resume == null) {
            throw exception(HR_RESUME_NOT_EXISTS);
        }
        return resume;
    }

    @Override
    public HrResumeDO getResume(Long id) {
        return resumeMapper.selectById(id);
    }

    @Override
    public PageResult<HrResumeDO> getResumePage(HrResumePageReqVO pageReqVO) {
        return resumeMapper.selectPage(pageReqVO);
    }

    @Override
    public void screenResume(HrResumeScreenReqVO reqVO) {
        HrResumeDO resume = validateExists(reqVO.getId());
        if (!HrResumeStatusEnum.PENDING.getStatus().equals(resume.getStatus())) {
            throw exception(HR_RESUME_STATUS_INVALID);
        }
        HrResumeDO updateObj = new HrResumeDO();
        updateObj.setId(reqVO.getId());
        if (Boolean.TRUE.equals(reqVO.getPassed())) {
            updateObj.setStatus(HrResumeStatusEnum.SCREEN_PASSED.getStatus());
        } else {
            updateObj.setStatus(HrResumeStatusEnum.ELIMINATED.getStatus());
        }
        updateObj.setRemark(reqVO.getRemark());
        resumeMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long offerResume(Long id) {
        HrResumeDO resume = validateExists(id);
        // 简历状态变更为已录用
        HrResumeDO updateObj = new HrResumeDO();
        updateObj.setId(id);
        updateObj.setStatus(HrResumeStatusEnum.OFFERED.getStatus());
        resumeMapper.updateById(updateObj);
        // 创建员工档案
        HrEmployeeSaveReqVO employeeReqVO = new HrEmployeeSaveReqVO();
        employeeReqVO.setEmpNo("EMP" + System.currentTimeMillis());
        employeeReqVO.setName(resume.getCandidateName());
        employeeReqVO.setGender(HrGenderEnum.MALE.getGender());
        employeeReqVO.setPhone(resume.getPhone());
        employeeReqVO.setEmail(resume.getEmail());
        employeeReqVO.setHireDate(java.time.LocalDate.now());
        employeeReqVO.setStatus(HrEmployeeStatusEnum.ACTIVE.getStatus());
        employeeReqVO.setEmploymentType(HrEmploymentTypeEnum.FULL_TIME.getType());
        return employeeService.createEmployee(employeeReqVO);
    }

    public List<HrResumeDO> getResumeListByJobPostingId(Long jobPostingId) {
        return resumeMapper.selectListByJobPostingId(jobPostingId);
    }

}