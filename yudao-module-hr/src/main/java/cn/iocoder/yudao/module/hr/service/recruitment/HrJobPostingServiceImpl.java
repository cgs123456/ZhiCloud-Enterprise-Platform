package cn.iocoder.yudao.module.hr.service.recruitment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.hr.controller.admin.recruitment.vo.HrJobPostingPageReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.recruitment.vo.HrJobPostingSaveReqVO;
import cn.iocoder.yudao.module.hr.dal.dataobject.recruitment.HrJobPostingDO;
import cn.iocoder.yudao.module.hr.dal.mysql.recruitment.HrJobPostingMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hr.enums.ErrorCodeConstants.*;

@Service
@Validated
public class HrJobPostingServiceImpl implements HrJobPostingService {

    @Resource
    private HrJobPostingMapper jobPostingMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createJobPosting(HrJobPostingSaveReqVO createReqVO) {
        HrJobPostingDO jobPosting = BeanUtils.toBean(createReqVO, HrJobPostingDO.class);
        if (jobPosting.getStatus() == null) {
            jobPosting.setStatus(0);
        }
        jobPostingMapper.insert(jobPosting);
        return jobPosting.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateJobPosting(HrJobPostingSaveReqVO updateReqVO) {
        validateExists(updateReqVO.getId());
        HrJobPostingDO updateObj = BeanUtils.toBean(updateReqVO, HrJobPostingDO.class);
        jobPostingMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteJobPosting(Long id) {
        validateExists(id);
        jobPostingMapper.deleteById(id);
    }

    private HrJobPostingDO validateExists(Long id) {
        HrJobPostingDO jobPosting = jobPostingMapper.selectById(id);
        if (jobPosting == null) {
            throw exception(HR_JOB_POSTING_NOT_EXISTS);
        }
        return jobPosting;
    }

    @Override
    public HrJobPostingDO getJobPosting(Long id) {
        return jobPostingMapper.selectById(id);
    }

    @Override
    public PageResult<HrJobPostingDO> getJobPostingPage(HrJobPostingPageReqVO pageReqVO) {
        return jobPostingMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishJobPosting(Long id) {
        validateExists(id);
        HrJobPostingDO updateObj = new HrJobPostingDO();
        updateObj.setId(id);
        updateObj.setStatus(0);
        updateObj.setPublishDate(LocalDate.now());
        jobPostingMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeJobPosting(Long id) {
        validateExists(id);
        HrJobPostingDO updateObj = new HrJobPostingDO();
        updateObj.setId(id);
        updateObj.setStatus(2);
        updateObj.setCloseDate(LocalDate.now());
        jobPostingMapper.updateById(updateObj);
    }

}