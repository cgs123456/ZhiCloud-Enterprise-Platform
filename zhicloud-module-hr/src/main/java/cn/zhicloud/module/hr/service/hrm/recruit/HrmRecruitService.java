package cn.zhicloud.module.hr.service.hrm.recruit;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.hr.controller.admin.hrm.recruit.vo.*;

import java.util.Collection;
import java.util.List;

/**
 * HRM 招聘域 Service 接口（职位类型/职位/渠道/候选人/面试/配置）
 *
 * @author zhicloud
 */
public interface HrmRecruitService {

    // ========== 职位类型 ==========

    List<HrmRecruitPostTypeVO> getPostTypeList(Integer status);

    // ========== 招聘职位 ==========

    PageResult<HrmRecruitPostRespVO> getPostPage(HrmRecruitPostPageReqVO reqVO);

    HrmRecruitPostRespVO getPost(Long id);

    List<HrmRecruitPostRespVO> getPostSimpleList(Integer status);

    List<HrmRecruitPostStatusCountRespVO> getPostStatusCount();

    Long createPost(HrmRecruitPostSaveReqVO reqVO);

    void updatePost(HrmRecruitPostSaveReqVO reqVO);

    void updatePostStatus(Long id, Integer status, String stopReason);

    // ========== 招聘渠道 ==========

    PageResult<HrmRecruitChannelVO> getChannelPage(Integer pageNo, Integer pageSize, String name);

    HrmRecruitChannelVO getChannel(Long id);

    List<HrmRecruitChannelVO> getChannelSimpleList();

    Long createChannel(HrmRecruitChannelSaveReqVO reqVO);

    void updateChannel(HrmRecruitChannelSaveReqVO reqVO);

    void updateChannelStatus(HrmRecruitChannelUpdateStatusReqVO reqVO);

    /**
     * 删除渠道：内置渠道（systemFlag）需 transferChannelId 承接；候选人引用需改指或拒绝
     */
    void deleteChannel(HrmRecruitChannelDeleteReqVO reqVO);

    // ========== 候选人 ==========

    PageResult<HrmRecruitCandidateRespVO> getCandidatePage(HrmRecruitCandidatePageReqVO reqVO);

    HrmRecruitCandidateRespVO getCandidate(Long id);

    List<HrmRecruitCandidateStatusCountRespVO> getCandidateStatusCount();

    Long createCandidate(HrmRecruitCandidateSaveReqVO reqVO);

    void updateCandidate(HrmRecruitCandidateSaveReqVO reqVO);

    void deleteCandidate(Long id);

    void updateCandidateStatus(HrmRecruitCandidateUpdateStatusReqVO reqVO);

    void eliminateCandidate(HrmRecruitCandidateEliminateReqVO reqVO);

    void updateCandidateChannel(HrmRecruitCandidateUpdateChannelReqVO reqVO);

    void updateCandidatePost(HrmRecruitCandidateUpdatePostReqVO reqVO);

    /**
     * 候选人转员工（建档 + 候选人置已入职 + 回填 employee_id）
     */
    Long convertCandidateToEmployee(HrmRecruitCandidateConvertEmployeeReqVO reqVO);

    /**
     * 清理候选人编号（导入/测试残留，按 ids 软删）
     */
    void cleanCandidateIds(Collection<Long> ids);

    // ========== 面试 ==========

    List<HrmRecruitInterviewRespVO> getInterviewListByCandidate(Long candidateId);

    HrmRecruitInterviewRespVO getInterview(Long id);

    Long createInterview(HrmRecruitInterviewSaveReqVO reqVO);

    void updateInterview(HrmRecruitInterviewSaveReqVO reqVO);

    void updateInterviewResult(HrmRecruitInterviewResultReqVO reqVO);

    void deleteInterview(Long id);

    // ========== 配置 ==========

    List<String> getEliminateReasonList();

    void saveEliminateReason(HrmRecruitEliminateReasonSaveReqVO reqVO);

}
