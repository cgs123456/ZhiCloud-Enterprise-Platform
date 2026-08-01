package cn.iocoder.yudao.module.hr.service.recruitment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.hr.controller.admin.recruitment.vo.HrInterviewPageReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.recruitment.vo.HrInterviewResultReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.recruitment.vo.HrInterviewSaveReqVO;
import cn.iocoder.yudao.module.hr.dal.dataobject.recruitment.HrInterviewDO;
import cn.iocoder.yudao.module.hr.dal.dataobject.recruitment.HrResumeDO;
import cn.iocoder.yudao.module.hr.dal.mysql.recruitment.HrInterviewMapper;
import cn.iocoder.yudao.module.hr.dal.mysql.recruitment.HrResumeMapper;
import cn.iocoder.yudao.module.hr.enums.recruitment.HrResumeStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hr.enums.ErrorCodeConstants.*;

@Service
@Validated
public class HrInterviewServiceImpl implements HrInterviewService {

    @Resource
    private HrInterviewMapper interviewMapper;
    @Resource
    private HrResumeMapper resumeMapper;

    @Override
    public Long createInterview(HrInterviewSaveReqVO createReqVO) {
        HrInterviewDO interview = BeanUtils.toBean(createReqVO, HrInterviewDO.class);
        if (interview.getStatus() == null) {
            interview.setStatus(0);
        }
        if (interview.getInterviewRound() == null) {
            interview.setInterviewRound(1);
        }
        interviewMapper.insert(interview);
        return interview.getId();
    }

    @Override
    public void updateInterview(HrInterviewSaveReqVO updateReqVO) {
        validateExists(updateReqVO.getId());
        HrInterviewDO updateObj = BeanUtils.toBean(updateReqVO, HrInterviewDO.class);
        interviewMapper.updateById(updateObj);
    }

    @Override
    public void deleteInterview(Long id) {
        validateExists(id);
        interviewMapper.deleteById(id);
    }

    private HrInterviewDO validateExists(Long id) {
        HrInterviewDO interview = interviewMapper.selectById(id);
        if (interview == null) {
            throw exception(HR_INTERVIEW_NOT_EXISTS);
        }
        return interview;
    }

    @Override
    public HrInterviewDO getInterview(Long id) {
        return interviewMapper.selectById(id);
    }

    @Override
    public PageResult<HrInterviewDO> getInterviewPage(HrInterviewPageReqVO pageReqVO) {
        return interviewMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long scheduleInterview(HrInterviewSaveReqVO reqVO) {
        // 校验简历存在
        HrResumeDO resume = resumeMapper.selectById(reqVO.getResumeId());
        if (resume == null) {
            throw exception(HR_RESUME_NOT_EXISTS);
        }
        // 计算面试轮次：取该简历已有面试数 + 1
        List<HrInterviewDO> existing = interviewMapper.selectListByResumeId(reqVO.getResumeId());
        int round = existing.size() + 1;
        HrInterviewDO interview = BeanUtils.toBean(reqVO, HrInterviewDO.class);
        interview.setInterviewRound(round);
        interview.setStatus(0);
        interviewMapper.insert(interview);
        // 简历状态变更为已面试
        HrResumeDO updateResume = new HrResumeDO();
        updateResume.setId(resume.getId());
        updateResume.setStatus(HrResumeStatusEnum.INTERVIEWED.getStatus());
        resumeMapper.updateById(updateResume);
        return interview.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordResult(HrInterviewResultReqVO reqVO) {
        HrInterviewDO interview = validateExists(reqVO.getId());
        HrInterviewDO updateObj = new HrInterviewDO();
        updateObj.setId(reqVO.getId());
        updateObj.setResult(reqVO.getResult());
        updateObj.setComment(reqVO.getComment());
        updateObj.setStatus(1);
        interviewMapper.updateById(updateObj);
        // 若通过，更新简历状态为已面试（保持已面试状态，等待 offer）
        if (reqVO.getResult() != null && reqVO.getResult() == 1) {
            HrResumeDO updateResume = new HrResumeDO();
            updateResume.setId(interview.getResumeId());
            updateResume.setStatus(HrResumeStatusEnum.INTERVIEWED.getStatus());
            resumeMapper.updateById(updateResume);
        }
    }

}