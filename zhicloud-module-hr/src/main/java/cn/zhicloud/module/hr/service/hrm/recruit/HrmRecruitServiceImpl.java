package cn.zhicloud.module.hr.service.hrm.recruit;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.zhicloud.framework.common.pojo.PageParam;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.ip.core.utils.AreaUtils;
import cn.zhicloud.module.hr.controller.admin.hrm.recruit.vo.*;
import cn.zhicloud.module.hr.controller.admin.hrm.vo.HrmEmployeeSaveReqVO;
import cn.zhicloud.module.hr.dal.dataobject.employee.HrEmployeeDO;
import cn.zhicloud.module.hr.dal.dataobject.recruit.*;
import cn.zhicloud.module.hr.dal.mysql.employee.HrEmployeeMapper;
import cn.zhicloud.module.hr.dal.mysql.recruit.*;
import cn.zhicloud.module.hr.service.hrm.HrmEmployeeService;
import cn.zhicloud.module.system.api.dept.DeptApi;
import cn.zhicloud.module.system.api.dept.dto.DeptRespDTO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.hr.enums.ErrorCodeConstants.*;

/**
 * HRM 招聘域 Service 实现类
 *
 * 口径（对齐前端 views/hrm/recruit）：
 * * 职位状态计数按 0停止/1招聘中 两档；候选人状态计数按 1-8 八档（全档返回，前端页签消费）。
 * * 招聘进度 = 已入职人数 / 招聘人数（百分比，招聘人数为 0 或空时置 0）。
 * * 候选人转员工：复用员工域 createEmployee（工号唯一/在职必填校验同口径），成功后候选人置已入职（8）并回填 employee_id。
 * * 渠道删除：系统内置（systemFlag=true）需 transferChannelId 承接——引用该渠道的候选人改指承接渠道；
 *   非内置渠道直接软删，候选人引用保留（历史可查）。
 *
 * @author zhicloud
 */
@Slf4j
@Service
@Validated
public class HrmRecruitServiceImpl implements HrmRecruitService {

    private static final int CANDIDATE_STATUS_JOINED = 8;
    private static final int CHANNEL_KIND_ELIMINATE_REASON = 1;

    @Resource
    private HrmRecruitPostTypeMapper postTypeMapper;
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
    @Resource
    private HrEmployeeMapper hrmEmployeeMapper;
    @Resource
    private HrmEmployeeService employeeService;
    @Resource
    private DeptApi deptApi;

    // ========== 职位类型 ==========

    @Override
    public List<HrmRecruitPostTypeVO> getPostTypeList(Integer status) {
        return BeanUtils.toBean(postTypeMapper.selectListByStatus(status), HrmRecruitPostTypeVO.class);
    }

    // ========== 招聘职位 ==========

    @Override
    public PageResult<HrmRecruitPostRespVO> getPostPage(HrmRecruitPostPageReqVO reqVO) {
        Collection<Long> deptIds = expandDeptIds(reqVO.getDeptIds());
        if (deptIds != null && deptIds.isEmpty()) {
            return PageResult.empty();
        }
        PageResult<HrmRecruitPostDO> pageResult = hrmRecruitPostMapper.selectPage(reqVO, deptIds);
        return new PageResult<>(enrichPosts(pageResult.getList()), pageResult.getTotal());
    }

    @Override
    public HrmRecruitPostRespVO getPost(Long id) {
        HrmRecruitPostDO post = validatePostExists(id);
        return enrichPosts(Collections.singletonList(post)).get(0);
    }

    @Override
    public List<HrmRecruitPostRespVO> getPostSimpleList(Integer status) {
        return enrichPosts(hrmRecruitPostMapper.selectListByStatus(status));
    }

    @Override
    public List<HrmRecruitPostStatusCountRespVO> getPostStatusCount() {
        List<HrmRecruitPostStatusCountRespVO> result = new ArrayList<>(2);
        result.add(buildPostStatusCount(0, hrmRecruitPostMapper.selectListByStatus(0).size()));
        result.add(buildPostStatusCount(1, hrmRecruitPostMapper.selectListByStatus(1).size()));
        return result;
    }

    private HrmRecruitPostStatusCountRespVO buildPostStatusCount(int status, long count) {
        HrmRecruitPostStatusCountRespVO vo = new HrmRecruitPostStatusCountRespVO();
        vo.setStatus(status);
        vo.setCount(count);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPost(HrmRecruitPostSaveReqVO reqVO) {
        HrmRecruitPostDO post = BeanUtils.toBean(reqVO, HrmRecruitPostDO.class);
        if (post.getStatus() == null) {
            post.setStatus(1); // 默认招聘中
        }
        hrmRecruitPostMapper.insert(post);
        return post.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePost(HrmRecruitPostSaveReqVO reqVO) {
        validatePostExists(reqVO.getId());
        hrmRecruitPostMapper.updateById(BeanUtils.toBean(reqVO, HrmRecruitPostDO.class));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePostStatus(Long id, Integer status, String stopReason) {
        validatePostExists(id);
        hrmRecruitPostMapper.update(null, new LambdaUpdateWrapper<HrmRecruitPostDO>()
                .eq(HrmRecruitPostDO::getId, id)
                .set(HrmRecruitPostDO::getStatus, status)
                .set(HrmRecruitPostDO::getStopReason, stopReason));
    }

    // ========== 招聘渠道 ==========

    @Override
    public PageResult<HrmRecruitChannelVO> getChannelPage(Integer pageNo, Integer pageSize, String name) {
        PageParam pageParam = new PageParam();
        pageParam.setPageNo(pageNo != null ? pageNo : 1);
        pageParam.setPageSize(pageSize != null ? pageSize : 10);
        PageResult<HrmRecruitChannelDO> pageResult = hrmRecruitChannelMapper.selectPage(pageParam, name);
        return new PageResult<>(BeanUtils.toBean(pageResult.getList(), HrmRecruitChannelVO.class), pageResult.getTotal());
    }

    @Override
    public HrmRecruitChannelVO getChannel(Long id) {
        return BeanUtils.toBean(validateChannelExists(id), HrmRecruitChannelVO.class);
    }

    @Override
    public List<HrmRecruitChannelVO> getChannelSimpleList() {
        return BeanUtils.toBean(hrmRecruitChannelMapper.selectListAll(), HrmRecruitChannelVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createChannel(HrmRecruitChannelSaveReqVO reqVO) {
        validateChannelNameUnique(null, reqVO.getName());
        HrmRecruitChannelDO channel = BeanUtils.toBean(reqVO, HrmRecruitChannelDO.class);
        channel.setSystemFlag(false); // 新建渠道非内置
        if (channel.getStatus() == null) {
            channel.setStatus(0);
        }
        if (channel.getSort() == null) {
            channel.setSort(0);
        }
        hrmRecruitChannelMapper.insert(channel);
        return channel.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateChannel(HrmRecruitChannelSaveReqVO reqVO) {
        validateChannelExists(reqVO.getId());
        validateChannelNameUnique(reqVO.getId(), reqVO.getName());
        HrmRecruitChannelDO updateObj = BeanUtils.toBean(reqVO, HrmRecruitChannelDO.class);
        updateObj.setSystemFlag(null); // 内置标志不可改
        hrmRecruitChannelMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateChannelStatus(HrmRecruitChannelUpdateStatusReqVO reqVO) {
        validateChannelExists(reqVO.getId());
        hrmRecruitChannelMapper.update(null, new LambdaUpdateWrapper<HrmRecruitChannelDO>()
                .eq(HrmRecruitChannelDO::getId, reqVO.getId())
                .set(HrmRecruitChannelDO::getStatus, reqVO.getStatus()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChannel(HrmRecruitChannelDeleteReqVO reqVO) {
        HrmRecruitChannelDO channel = validateChannelExists(reqVO.getId());
        if (Boolean.TRUE.equals(channel.getSystemFlag())) {
            // 内置渠道需承接：引用候选人改指承接渠道
            if (reqVO.getTransferChannelId() == null) {
                throw exception(HRM_RECRUIT_CHANNEL_TRANSFER_REQUIRED);
            }
            if (reqVO.getTransferChannelId().equals(reqVO.getId())) {
                throw exception(HRM_RECRUIT_CHANNEL_TRANSFER_INVALID);
            }
            validateChannelExists(reqVO.getTransferChannelId());
            hrmRecruitCandidateMapper.update(null, new LambdaUpdateWrapper<HrmRecruitCandidateDO>()
                    .eq(HrmRecruitCandidateDO::getChannelId, reqVO.getId())
                    .set(HrmRecruitCandidateDO::getChannelId, reqVO.getTransferChannelId()));
        }
        hrmRecruitChannelMapper.deleteById(reqVO.getId());
    }

    // ========== 候选人 ==========

    @Override
    public PageResult<HrmRecruitCandidateRespVO> getCandidatePage(HrmRecruitCandidatePageReqVO reqVO) {
        Collection<Long> postIds = reqVO.getPostIds() != null && reqVO.getPostIds().length > 0
                ? Arrays.asList(reqVO.getPostIds()) : null;
        Collection<Long> deptIds = expandDeptIds(reqVO.getDeptIds());
        if (deptIds != null && deptIds.isEmpty()) {
            return PageResult.empty();
        }
        PageResult<HrmRecruitCandidateDO> pageResult = hrmRecruitCandidateMapper.selectPage(reqVO, postIds, deptIds);
        return new PageResult<>(enrichCandidates(pageResult.getList()), pageResult.getTotal());
    }

    @Override
    public HrmRecruitCandidateRespVO getCandidate(Long id) {
        HrmRecruitCandidateDO candidate = validateCandidateExists(id);
        return enrichCandidates(Collections.singletonList(candidate)).get(0);
    }

    @Override
    public List<HrmRecruitCandidateStatusCountRespVO> getCandidateStatusCount() {
        List<HrmRecruitCandidateDO> all = hrmRecruitCandidateMapper.selectList(null);
        Map<Integer, Long> countMap = all.stream()
                .filter(c -> c.getStatus() != null)
                .collect(Collectors.groupingBy(HrmRecruitCandidateDO::getStatus, Collectors.counting()));
        List<HrmRecruitCandidateStatusCountRespVO> result = new ArrayList<>(8);
        for (int status = 1; status <= 8; status++) {
            HrmRecruitCandidateStatusCountRespVO vo = new HrmRecruitCandidateStatusCountRespVO();
            vo.setStatus(status);
            vo.setCount(countMap.getOrDefault(status, 0L));
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCandidate(HrmRecruitCandidateSaveReqVO reqVO) {
        HrmRecruitCandidateDO candidate = BeanUtils.toBean(reqVO, HrmRecruitCandidateDO.class);
        if (candidate.getStatus() == null) {
            candidate.setStatus(1); // 新候选人
        }
        candidate.setStatusUpdateTime(LocalDateTime.now());
        hrmRecruitCandidateMapper.insert(candidate);
        return candidate.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCandidate(HrmRecruitCandidateSaveReqVO reqVO) {
        validateCandidateExists(reqVO.getId());
        hrmRecruitCandidateMapper.updateById(BeanUtils.toBean(reqVO, HrmRecruitCandidateDO.class));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCandidate(Long id) {
        validateCandidateExists(id);
        // 连带软删其面试记录（避免悬挂）
        hrmRecruitInterviewMapper.selectListByCandidateId(id)
                .forEach(interview -> hrmRecruitInterviewMapper.deleteById(interview.getId()));
        hrmRecruitCandidateMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCandidateStatus(HrmRecruitCandidateUpdateStatusReqVO reqVO) {
        validateCandidateExists(reqVO.getId());
        hrmRecruitCandidateMapper.update(null, new LambdaUpdateWrapper<HrmRecruitCandidateDO>()
                .eq(HrmRecruitCandidateDO::getId, reqVO.getId())
                .set(HrmRecruitCandidateDO::getStatus, reqVO.getStatus())
                .set(HrmRecruitCandidateDO::getStatusUpdateTime, LocalDateTime.now()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void eliminateCandidate(HrmRecruitCandidateEliminateReqVO reqVO) {
        validateCandidateExists(reqVO.getId());
        hrmRecruitCandidateMapper.update(null, new LambdaUpdateWrapper<HrmRecruitCandidateDO>()
                .eq(HrmRecruitCandidateDO::getId, reqVO.getId())
                .set(HrmRecruitCandidateDO::getStatus, 7) // 已淘汰
                .set(HrmRecruitCandidateDO::getEliminate, reqVO.getEliminate())
                .set(HrmRecruitCandidateDO::getStatusUpdateTime, LocalDateTime.now()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCandidateChannel(HrmRecruitCandidateUpdateChannelReqVO reqVO) {
        validateCandidateExists(reqVO.getId());
        validateChannelExists(reqVO.getChannelId());
        hrmRecruitCandidateMapper.update(null, new LambdaUpdateWrapper<HrmRecruitCandidateDO>()
                .eq(HrmRecruitCandidateDO::getId, reqVO.getId())
                .set(HrmRecruitCandidateDO::getChannelId, reqVO.getChannelId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCandidatePost(HrmRecruitCandidateUpdatePostReqVO reqVO) {
        validateCandidateExists(reqVO.getId());
        validatePostExists(reqVO.getPostId());
        hrmRecruitCandidateMapper.update(null, new LambdaUpdateWrapper<HrmRecruitCandidateDO>()
                .eq(HrmRecruitCandidateDO::getId, reqVO.getId())
                .set(HrmRecruitCandidateDO::getPostId, reqVO.getPostId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long convertCandidateToEmployee(HrmRecruitCandidateConvertEmployeeReqVO reqVO) {
        HrmRecruitCandidateDO candidate = validateCandidateExists(reqVO.getId());
        // 1. 复用员工域建档（工号唯一/在职必填校验同口径）
        HrmEmployeeSaveReqVO employeeReq = new HrmEmployeeSaveReqVO();
        employeeReq.setName(candidate.getName());
        employeeReq.setJobNumber(reqVO.getJobNumber());
        employeeReq.setMobile(candidate.getMobile());
        employeeReq.setSex(candidate.getSex());
        employeeReq.setEmail(candidate.getEmail());
        employeeReq.setIdNumber(null);
        employeeReq.setDeptId(reqVO.getDeptId() != null ? reqVO.getDeptId() : candidate.getDeptId());
        employeeReq.setLeaderEmployeeId(reqVO.getLeaderEmployeeId());
        employeeReq.setType(reqVO.getType() != null ? reqVO.getType() : 1);
        employeeReq.setStatus(reqVO.getStatus() != null ? reqVO.getStatus() : 1);
        employeeReq.setEntryStatus(1);
        employeeReq.setEntryTime(reqVO.getEntryTime() != null ? reqVO.getEntryTime() : LocalDateTime.now());
        employeeReq.setProbation(reqVO.getProbation());
        employeeReq.setPostName(reqVO.getPostName());
        employeeReq.setPostLevel(reqVO.getPostLevel());
        employeeReq.setWorkCity(reqVO.getWorkCity());
        employeeReq.setWorkAddress(reqVO.getWorkAddress());
        employeeReq.setRemark(reqVO.getRemark());
        employeeReq.setCandidateId(candidate.getId());
        Long employeeId = employeeService.createEmployee(employeeReq);
        // 2. 候选人置已入职并回填员工编号
        hrmRecruitCandidateMapper.update(null, new LambdaUpdateWrapper<HrmRecruitCandidateDO>()
                .eq(HrmRecruitCandidateDO::getId, candidate.getId())
                .set(HrmRecruitCandidateDO::getStatus, CANDIDATE_STATUS_JOINED)
                .set(HrmRecruitCandidateDO::getEmployeeId, employeeId)
                .set(HrmRecruitCandidateDO::getEntryTime, employeeReq.getEntryTime())
                .set(HrmRecruitCandidateDO::getStatusUpdateTime, LocalDateTime.now()));
        return employeeId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cleanCandidateIds(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        hrmRecruitCandidateMapper.deleteByIds(ids);
    }

    // ========== 面试 ==========

    @Override
    public List<HrmRecruitInterviewRespVO> getInterviewListByCandidate(Long candidateId) {
        validateCandidateExists(candidateId);
        return enrichInterviews(hrmRecruitInterviewMapper.selectListByCandidateId(candidateId));
    }

    @Override
    public HrmRecruitInterviewRespVO getInterview(Long id) {
        HrmRecruitInterviewDO interview = validateInterviewExists(id);
        return enrichInterviews(Collections.singletonList(interview)).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createInterview(HrmRecruitInterviewSaveReqVO reqVO) {
        validateCandidateExists(reqVO.getCandidateId());
        HrmRecruitInterviewDO interview = BeanUtils.toBean(reqVO, HrmRecruitInterviewDO.class);
        if (interview.getResult() == null) {
            interview.setResult(1); // 未完成
        }
        hrmRecruitInterviewMapper.insert(interview);
        // 候选人状态推进到「安排面试」（仅当处于更早状态时）
        syncCandidateInterviewState(reqVO.getCandidateId(), interview);
        return interview.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInterview(HrmRecruitInterviewSaveReqVO reqVO) {
        validateInterviewExists(reqVO.getId());
        hrmRecruitInterviewMapper.updateById(BeanUtils.toBean(reqVO, HrmRecruitInterviewDO.class));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInterviewResult(HrmRecruitInterviewResultReqVO reqVO) {
        HrmRecruitInterviewDO interview = validateInterviewExists(reqVO.getId());
        hrmRecruitInterviewMapper.update(null, new LambdaUpdateWrapper<HrmRecruitInterviewDO>()
                .eq(HrmRecruitInterviewDO::getId, reqVO.getId())
                .set(HrmRecruitInterviewDO::getResult, reqVO.getResult())
                .set(HrmRecruitInterviewDO::getEvaluate, reqVO.getEvaluate())
                .set(HrmRecruitInterviewDO::getCancelReason, reqVO.getCancelReason()));
        // 面试通过 → 候选人推进到「面试通过」；取消 → 保持安排面试
        if (Integer.valueOf(2).equals(reqVO.getResult())) {
            hrmRecruitCandidateMapper.update(null, new LambdaUpdateWrapper<HrmRecruitCandidateDO>()
                    .eq(HrmRecruitCandidateDO::getId, interview.getCandidateId())
                    .set(HrmRecruitCandidateDO::getStatus, 4)
                    .set(HrmRecruitCandidateDO::getInterviewResult, reqVO.getResult())
                    .set(HrmRecruitCandidateDO::getStatusUpdateTime, LocalDateTime.now()));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteInterview(Long id) {
        validateInterviewExists(id);
        hrmRecruitInterviewMapper.deleteById(id);
    }

    // ========== 配置 ==========

    @Override
    public List<String> getEliminateReasonList() {
        HrmRecruitConfigDO config = hrmRecruitConfigMapper.selectByKind(CHANNEL_KIND_ELIMINATE_REASON);
        return config != null && config.getContent() != null ? config.getContent() : Collections.emptyList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveEliminateReason(HrmRecruitEliminateReasonSaveReqVO reqVO) {
        HrmRecruitConfigDO config = hrmRecruitConfigMapper.selectByKind(CHANNEL_KIND_ELIMINATE_REASON);
        if (config == null) {
            config = HrmRecruitConfigDO.builder()
                    .kind(CHANNEL_KIND_ELIMINATE_REASON)
                    .content(reqVO.getReasons())
                    .build();
            hrmRecruitConfigMapper.insert(config);
        } else {
            // json 列必须走 DO + updateById（JacksonTypeHandler 生效）；LambdaUpdateWrapper.set() 绕开 typeHandler
            HrmRecruitConfigDO updateObj = new HrmRecruitConfigDO();
            updateObj.setId(config.getId());
            updateObj.setContent(reqVO.getReasons());
            hrmRecruitConfigMapper.updateById(updateObj);
        }
    }

    // ========== 私有：候选人面试状态同步 ==========

    private void syncCandidateInterviewState(Long candidateId, HrmRecruitInterviewDO interview) {
        HrmRecruitCandidateDO candidate = hrmRecruitCandidateMapper.selectById(candidateId);
        if (candidate == null || candidate.getStatus() == null) {
            return;
        }
        // 仅当候选人状态早于「安排面试」（1/2）时推进，避免回退已通过的候选
        if (candidate.getStatus() <= 2) {
            hrmRecruitCandidateMapper.update(null, new LambdaUpdateWrapper<HrmRecruitCandidateDO>()
                    .eq(HrmRecruitCandidateDO::getId, candidateId)
                    .set(HrmRecruitCandidateDO::getStatus, 3)
                    .set(HrmRecruitCandidateDO::getStageNumber, interview.getStageNumber())
                    .set(HrmRecruitCandidateDO::getInterviewType, interview.getType())
                    .set(HrmRecruitCandidateDO::getInterviewEmployeeId, interview.getInterviewEmployeeId())
                    .set(HrmRecruitCandidateDO::getOtherInterviewEmployeeIds, interview.getOtherInterviewEmployeeIds())
                    .set(HrmRecruitCandidateDO::getInterviewTime, interview.getInterviewTime())
                    .set(HrmRecruitCandidateDO::getInterviewAddress, interview.getAddress())
                    .set(HrmRecruitCandidateDO::getStatusUpdateTime, LocalDateTime.now()));
        }
    }

    // ========== 私有：enrich ==========

    private List<HrmRecruitPostRespVO> enrichPosts(List<HrmRecruitPostDO> posts) {
        if (CollUtil.isEmpty(posts)) {
            return Collections.emptyList();
        }
        List<HrmRecruitPostRespVO> voList = BeanUtils.toBean(posts, HrmRecruitPostRespVO.class);
        // 部门名
        Set<Long> deptIds = posts.stream().map(HrmRecruitPostDO::getDeptId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, DeptRespDTO> deptMap = deptIds.isEmpty() ? Collections.emptyMap() : deptApi.getDeptMap(deptIds);
        // 职位类型名
        Set<Long> typeIds = posts.stream().map(HrmRecruitPostDO::getPostTypeId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> typeNameMap = typeIds.isEmpty() ? Collections.emptyMap()
                : postTypeMapper.selectByIds(typeIds).stream()
                        .collect(Collectors.toMap(HrmRecruitPostTypeDO::getId, HrmRecruitPostTypeDO::getName, (a, b) -> a));
        // 员工名（负责人 + 面试官）
        Set<Long> employeeIds = new HashSet<>();
        posts.forEach(p -> {
            if (p.getOwnerEmployeeId() != null) {
                employeeIds.add(p.getOwnerEmployeeId());
            }
            if (p.getInterviewEmployeeIds() != null) {
                employeeIds.addAll(p.getInterviewEmployeeIds());
            }
        });
        Map<Long, String> employeeNameMap = employeeIds.isEmpty() ? Collections.emptyMap()
                : hrmEmployeeMapper.selectByIds(employeeIds).stream()
                        .collect(Collectors.toMap(HrEmployeeDO::getId, HrEmployeeDO::getName, (a, b) -> a));
        for (int i = 0; i < posts.size(); i++) {
            HrmRecruitPostDO post = posts.get(i);
            HrmRecruitPostRespVO vo = voList.get(i);
            DeptRespDTO dept = deptMap.get(post.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getName());
            }
            vo.setAreaName(post.getAreaId() != null ? AreaUtils.format(post.getAreaId().intValue()) : null);
            vo.setPostTypeName(typeNameMap.get(post.getPostTypeId()));
            vo.setOwnerEmployeeName(employeeNameMap.get(post.getOwnerEmployeeId()));
            if (post.getInterviewEmployeeIds() != null) {
                vo.setInterviewEmployeeNames(post.getInterviewEmployeeIds().stream()
                        .map(id -> employeeNameMap.getOrDefault(id, String.valueOf(id)))
                        .collect(Collectors.toList()));
            }
            // 已入职人数 = 该职位下状态=8 的候选人数
            Long hasEntry = hrmRecruitCandidateMapper.selectList(null).stream()
                    .filter(c -> post.getId().equals(c.getPostId()) && Integer.valueOf(CANDIDATE_STATUS_JOINED).equals(c.getStatus()))
                    .count();
            vo.setHasEntryNum(hasEntry);
            // 招聘进度 = 已入职 / 招聘人数（百分比）
            if (post.getRecruitNum() != null && post.getRecruitNum() > 0) {
                vo.setRecruitSchedule((int) Math.min(hasEntry * 100 / post.getRecruitNum(), 100));
            } else {
                vo.setRecruitSchedule(0);
            }
        }
        return voList;
    }

    private List<HrmRecruitCandidateRespVO> enrichCandidates(List<HrmRecruitCandidateDO> candidates) {
        if (CollUtil.isEmpty(candidates)) {
            return Collections.emptyList();
        }
        List<HrmRecruitCandidateRespVO> voList = BeanUtils.toBean(candidates, HrmRecruitCandidateRespVO.class);
        // 职位（名称 + 状态）
        Set<Long> postIds = candidates.stream().map(HrmRecruitCandidateDO::getPostId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, HrmRecruitPostDO> postMap = postIds.isEmpty() ? Collections.emptyMap()
                : hrmRecruitPostMapper.selectListByIds(postIds).stream().collect(Collectors.toMap(HrmRecruitPostDO::getId, p -> p, (a, b) -> a));
        // 部门
        Set<Long> deptIds = candidates.stream().map(HrmRecruitCandidateDO::getDeptId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, DeptRespDTO> deptMap = deptIds.isEmpty() ? Collections.emptyMap() : deptApi.getDeptMap(deptIds);
        // 渠道
        Set<Long> channelIds = candidates.stream().map(HrmRecruitCandidateDO::getChannelId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> channelNameMap = channelIds.isEmpty() ? Collections.emptyMap()
                : hrmRecruitChannelMapper.selectListByIds(channelIds).stream()
                        .collect(Collectors.toMap(HrmRecruitChannelDO::getId, HrmRecruitChannelDO::getName, (a, b) -> a));
        // 员工名（负责人 + 面试官）
        Set<Long> employeeIds = new HashSet<>();
        candidates.forEach(c -> {
            if (c.getOwnerEmployeeId() != null) {
                employeeIds.add(c.getOwnerEmployeeId());
            }
            if (c.getInterviewEmployeeId() != null) {
                employeeIds.add(c.getInterviewEmployeeId());
            }
            if (c.getOtherInterviewEmployeeIds() != null) {
                employeeIds.addAll(c.getOtherInterviewEmployeeIds());
            }
        });
        Map<Long, String> employeeNameMap = employeeIds.isEmpty() ? Collections.emptyMap()
                : hrmEmployeeMapper.selectByIds(employeeIds).stream()
                        .collect(Collectors.toMap(HrEmployeeDO::getId, HrEmployeeDO::getName, (a, b) -> a));
        for (int i = 0; i < candidates.size(); i++) {
            HrmRecruitCandidateDO candidate = candidates.get(i);
            HrmRecruitCandidateRespVO vo = voList.get(i);
            HrmRecruitPostDO post = postMap.get(candidate.getPostId());
            if (post != null) {
                vo.setPostName(post.getPostName());
                vo.setPostStatus(post.getStatus());
            }
            DeptRespDTO dept = deptMap.get(candidate.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getName());
            }
            vo.setChannelName(channelNameMap.get(candidate.getChannelId()));
            vo.setOwnerEmployeeName(employeeNameMap.get(candidate.getOwnerEmployeeId()));
            vo.setInterviewEmployeeName(employeeNameMap.get(candidate.getInterviewEmployeeId()));
            if (candidate.getOtherInterviewEmployeeIds() != null) {
                vo.setOtherInterviewEmployeeNames(candidate.getOtherInterviewEmployeeIds().stream()
                        .map(id -> employeeNameMap.getOrDefault(id, String.valueOf(id)))
                        .collect(Collectors.toList()));
            }
            // 当前面试编号（最新一轮）
            List<HrmRecruitInterviewDO> interviews = hrmRecruitInterviewMapper.selectListByCandidateId(candidate.getId());
            if (CollUtil.isNotEmpty(interviews)) {
                vo.setInterviewId(interviews.get(0).getId());
            }
            vo.setCreatorName(employeeNameMap.get(null)); // 创建人名称域未接线，保持 null
        }
        return voList;
    }

    private List<HrmRecruitInterviewRespVO> enrichInterviews(List<HrmRecruitInterviewDO> interviews) {
        if (CollUtil.isEmpty(interviews)) {
            return Collections.emptyList();
        }
        List<HrmRecruitInterviewRespVO> voList = BeanUtils.toBean(interviews, HrmRecruitInterviewRespVO.class);
        Set<Long> employeeIds = new HashSet<>();
        interviews.forEach(iv -> {
            if (iv.getInterviewEmployeeId() != null) {
                employeeIds.add(iv.getInterviewEmployeeId());
            }
            if (iv.getOtherInterviewEmployeeIds() != null) {
                employeeIds.addAll(iv.getOtherInterviewEmployeeIds());
            }
        });
        Map<Long, String> employeeNameMap = employeeIds.isEmpty() ? Collections.emptyMap()
                : hrmEmployeeMapper.selectByIds(employeeIds).stream()
                        .collect(Collectors.toMap(HrEmployeeDO::getId, HrEmployeeDO::getName, (a, b) -> a));
        for (int i = 0; i < interviews.size(); i++) {
            HrmRecruitInterviewDO interview = interviews.get(i);
            HrmRecruitInterviewRespVO vo = voList.get(i);
            vo.setInterviewEmployeeName(employeeNameMap.get(interview.getInterviewEmployeeId()));
            if (interview.getOtherInterviewEmployeeIds() != null) {
                vo.setOtherInterviewEmployeeNames(interview.getOtherInterviewEmployeeIds().stream()
                        .map(id -> employeeNameMap.getOrDefault(id, String.valueOf(id)))
                        .collect(Collectors.toList()));
            }
        }
        return voList;
    }

    // ========== 私有：校验与小工具 ==========

    private HrmRecruitPostDO validatePostExists(Long id) {
        HrmRecruitPostDO post = hrmRecruitPostMapper.selectById(id);
        if (post == null) {
            throw exception(HRM_RECRUIT_POST_NOT_EXISTS);
        }
        return post;
    }

    private HrmRecruitChannelDO validateChannelExists(Long id) {
        HrmRecruitChannelDO channel = hrmRecruitChannelMapper.selectById(id);
        if (channel == null) {
            throw exception(HRM_RECRUIT_CHANNEL_NOT_EXISTS);
        }
        return channel;
    }

    private void validateChannelNameUnique(Long id, String name) {
        HrmRecruitChannelDO exist = hrmRecruitChannelMapper.selectByName(name);
        if (exist != null && (id == null || !exist.getId().equals(id))) {
            throw exception(HRM_RECRUIT_CHANNEL_NAME_DUPLICATE);
        }
    }

    private HrmRecruitCandidateDO validateCandidateExists(Long id) {
        HrmRecruitCandidateDO candidate = hrmRecruitCandidateMapper.selectById(id);
        if (candidate == null) {
            throw exception(HRM_RECRUIT_CANDIDATE_NOT_EXISTS);
        }
        return candidate;
    }

    private HrmRecruitInterviewDO validateInterviewExists(Long id) {
        HrmRecruitInterviewDO interview = hrmRecruitInterviewMapper.selectById(id);
        if (interview == null) {
            throw exception(HRM_RECRUIT_INTERVIEW_NOT_EXISTS);
        }
        return interview;
    }

    /**
     * 部门子树展开（含自身）；入参为空返回 null（不限制）
     */
    private Collection<Long> expandDeptIds(Long[] deptIds) {
        if (deptIds == null || deptIds.length == 0) {
            return null;
        }
        Set<Long> result = new HashSet<>();
        for (Long deptId : deptIds) {
            if (deptId == null) {
                continue;
            }
            result.add(deptId);
            deptApi.getChildDeptList(deptId).forEach(d -> result.add(d.getId()));
        }
        return result;
    }

}
