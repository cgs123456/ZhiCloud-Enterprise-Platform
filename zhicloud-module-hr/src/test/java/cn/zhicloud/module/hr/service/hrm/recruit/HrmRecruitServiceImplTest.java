package cn.zhicloud.module.hr.service.hrm.recruit;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.hr.controller.admin.hrm.recruit.vo.*;
import cn.zhicloud.module.hr.dal.dataobject.recruit.*;
import cn.zhicloud.module.hr.dal.mysql.recruit.*;
import cn.zhicloud.module.hr.service.hrm.HrmEmployeeService;
import cn.zhicloud.module.system.api.dept.DeptApi;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.List;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * {@link HrmRecruitServiceImpl} 单元测试
 *
 * @author zhicloud
 */
@Import(HrmRecruitServiceImpl.class)
public class HrmRecruitServiceImplTest extends BaseDbUnitTest {

    @Resource
    private HrmRecruitServiceImpl recruitService;

    @Resource
    private HrmRecruitPostMapper hrmRecruitPostMapper;
    @Resource
    private HrmRecruitChannelMapper hrmRecruitChannelMapper;
    @Resource
    private HrmRecruitCandidateMapper hrmRecruitCandidateMapper;
    @Resource
    private HrmRecruitInterviewMapper hrmRecruitInterviewMapper;
    @Resource
    private HrmRecruitConfigMapper hrmRecruitConfigMapper;

    @MockitoBean
    private DeptApi deptApi;
    @MockitoBean
    private HrmEmployeeService employeeService;

    @Test
    public void test_createPost_defaultStatusRecruiting() {
        HrmRecruitPostSaveReqVO reqVO = new HrmRecruitPostSaveReqVO();
        reqVO.setPostName("高级后端工程师");
        reqVO.setRecruitNum(3);
        Long id = recruitService.createPost(reqVO);
        HrmRecruitPostDO post = hrmRecruitPostMapper.selectById(id);
        assertNotNull(post);
        assertEquals("高级后端工程师", post.getPostName());
        assertEquals(Integer.valueOf(1), post.getStatus()); // 默认招聘中
    }

    @Test
    public void test_updatePostStatus_stop() {
        HrmRecruitPostSaveReqVO reqVO = new HrmRecruitPostSaveReqVO();
        reqVO.setPostName("待停职位");
        Long id = recruitService.createPost(reqVO);

        recruitService.updatePostStatus(id, 0, "已招满");

        HrmRecruitPostDO post = hrmRecruitPostMapper.selectById(id);
        assertEquals(Integer.valueOf(0), post.getStatus());
        assertEquals("已招满", post.getStopReason());
    }

    @Test
    public void test_createChannel_notSystemFlag() {
        HrmRecruitChannelSaveReqVO reqVO = new HrmRecruitChannelSaveReqVO();
        reqVO.setName("BOSS直聘");
        Long id = recruitService.createChannel(reqVO);
        HrmRecruitChannelDO channel = hrmRecruitChannelMapper.selectById(id);
        assertEquals(false, channel.getSystemFlag()); // 新建非内置
    }

    @Test
    public void test_deleteSystemChannel_requiresTransfer() {
        // 内置渠道
        HrmRecruitChannelDO builtin = randomPojo(HrmRecruitChannelDO.class, o -> {
            o.setName("系统内置渠道");
            o.setSystemFlag(true);
            o.setStatus(0);
            o.setSort(0);
        });
        hrmRecruitChannelMapper.insert(builtin);
        // 候选人引用该渠道
        HrmRecruitCandidateDO candidate = randomPojo(HrmRecruitCandidateDO.class, o -> {
            o.setName("候选人A");
            o.setMobile("13800000001");
            o.setChannelId(builtin.getId());
            o.setStatus(1);
        });
        hrmRecruitCandidateMapper.insert(candidate);

        // 未指定承接渠道 → 拒
        HrmRecruitChannelDeleteReqVO noTransfer = new HrmRecruitChannelDeleteReqVO();
        noTransfer.setId(builtin.getId());
        cn.zhicloud.framework.common.exception.ServiceException ex =
                assertThrows(cn.zhicloud.framework.common.exception.ServiceException.class,
                        () -> recruitService.deleteChannel(noTransfer));
        assertEquals(1_050_018_003, ex.getCode());

        // 指定承接渠道 → 候选人改指，渠道删除
        HrmRecruitChannelSaveReqVO transferVO = new HrmRecruitChannelSaveReqVO();
        transferVO.setName("承接渠道");
        Long transferId = recruitService.createChannel(transferVO);

        HrmRecruitChannelDeleteReqVO withTransfer = new HrmRecruitChannelDeleteReqVO();
        withTransfer.setId(builtin.getId());
        withTransfer.setTransferChannelId(transferId);
        recruitService.deleteChannel(withTransfer);

        assertNull(hrmRecruitChannelMapper.selectById(builtin.getId()));
        assertEquals(transferId, hrmRecruitCandidateMapper.selectById(candidate.getId()).getChannelId());
    }

    @Test
    public void test_getCandidateStatusCount_eightTabs() {
        HrmRecruitCandidateDO c1 = randomPojo(HrmRecruitCandidateDO.class, o -> {
            o.setName("候选人1"); o.setMobile("13800000011"); o.setStatus(1);
        });
        HrmRecruitCandidateDO c2 = randomPojo(HrmRecruitCandidateDO.class, o -> {
            o.setName("候选人2"); o.setMobile("13800000012"); o.setStatus(1);
        });
        HrmRecruitCandidateDO c3 = randomPojo(HrmRecruitCandidateDO.class, o -> {
            o.setName("候选人3"); o.setMobile("13800000013"); o.setStatus(3);
        });
        hrmRecruitCandidateMapper.insert(c1);
        hrmRecruitCandidateMapper.insert(c2);
        hrmRecruitCandidateMapper.insert(c3);

        List<HrmRecruitCandidateStatusCountRespVO> counts = recruitService.getCandidateStatusCount();
        assertEquals(8, counts.size()); // 全 8 档
        java.util.Map<Integer, Long> map = counts.stream()
                .collect(java.util.stream.Collectors.toMap(HrmRecruitCandidateStatusCountRespVO::getStatus,
                        HrmRecruitCandidateStatusCountRespVO::getCount));
        assertEquals(2L, map.get(1));
        assertEquals(1L, map.get(3));
        assertEquals(0L, map.get(8));
    }

    @Test
    public void test_createInterview_advancesCandidateStatus() {
        HrmRecruitCandidateDO candidate = randomPojo(HrmRecruitCandidateDO.class, o -> {
            o.setName("待面试候选人"); o.setMobile("13800000021"); o.setStatus(2); // 初选通过
        });
        hrmRecruitCandidateMapper.insert(candidate);

        HrmRecruitInterviewSaveReqVO reqVO = new HrmRecruitInterviewSaveReqVO();
        reqVO.setCandidateId(candidate.getId());
        reqVO.setType(1);
        reqVO.setStageNumber(1);
        Long interviewId = recruitService.createInterview(reqVO);

        assertNotNull(hrmRecruitInterviewMapper.selectById(interviewId));
        // 候选人推进到「安排面试」(3)
        HrmRecruitCandidateDO updated = hrmRecruitCandidateMapper.selectById(candidate.getId());
        assertEquals(Integer.valueOf(3), updated.getStatus());
        assertEquals(Integer.valueOf(1), updated.getStageNumber());
    }

    @Test
    public void test_updateInterviewResult_passAdvancesCandidate() {
        HrmRecruitCandidateDO candidate = randomPojo(HrmRecruitCandidateDO.class, o -> {
            o.setName("面试中候选人"); o.setMobile("13800000031"); o.setStatus(3);
        });
        hrmRecruitCandidateMapper.insert(candidate);
        HrmRecruitInterviewDO interview = randomPojo(HrmRecruitInterviewDO.class, o -> {
            o.setCandidateId(candidate.getId()); o.setResult(1); o.setStageNumber(1);
        });
        hrmRecruitInterviewMapper.insert(interview);

        HrmRecruitInterviewResultReqVO reqVO = new HrmRecruitInterviewResultReqVO();
        reqVO.setId(interview.getId());
        reqVO.setResult(2); // 通过
        reqVO.setEvaluate("表现优秀");
        recruitService.updateInterviewResult(reqVO);

        assertEquals(Integer.valueOf(2), hrmRecruitInterviewMapper.selectById(interview.getId()).getResult());
        assertEquals(Integer.valueOf(4), hrmRecruitCandidateMapper.selectById(candidate.getId()).getStatus()); // 面试通过
    }

    @Test
    public void test_eliminateReason_saveAndRead() {
        HrmRecruitEliminateReasonSaveReqVO reqVO = new HrmRecruitEliminateReasonSaveReqVO();
        reqVO.setReasons(Arrays.asList("薪资不匹配", "经验不足"));
        recruitService.saveEliminateReason(reqVO);

        List<String> reasons = recruitService.getEliminateReasonList();
        assertEquals(Arrays.asList("薪资不匹配", "经验不足"), reasons);
        assertNotNull(hrmRecruitConfigMapper.selectByKind(1));
    }

    @Test
    public void test_convertCandidateToEmployee_setsJoinedAndBackfills() {
        HrmRecruitCandidateDO candidate = randomPojo(HrmRecruitCandidateDO.class, o -> {
            o.setName("转正候选人"); o.setMobile("13800000041"); o.setStatus(6); o.setDeptId(100L);
        });
        hrmRecruitCandidateMapper.insert(candidate);
        when(employeeService.createEmployee(any())).thenReturn(9999L);

        HrmRecruitCandidateConvertEmployeeReqVO reqVO = new HrmRecruitCandidateConvertEmployeeReqVO();
        reqVO.setId(candidate.getId());
        reqVO.setJobNumber("EMP-NEW-001");
        Long employeeId = recruitService.convertCandidateToEmployee(reqVO);

        assertEquals(9999L, employeeId);
        HrmRecruitCandidateDO updated = hrmRecruitCandidateMapper.selectById(candidate.getId());
        assertEquals(Integer.valueOf(8), updated.getStatus()); // 已入职
        assertEquals(9999L, updated.getEmployeeId());
    }

}
