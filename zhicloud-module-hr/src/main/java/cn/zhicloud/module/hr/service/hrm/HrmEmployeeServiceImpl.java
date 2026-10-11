package cn.zhicloud.module.hr.service.hrm;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.hr.controller.admin.hrm.vo.*;
import cn.zhicloud.module.hr.dal.dataobject.employee.HrEmployeeDO;
import cn.zhicloud.module.hr.dal.dataobject.hrm.HrmEmployeeChangeRecordDO;
import cn.zhicloud.module.hr.dal.dataobject.hrm.HrmEmployeeQuitInfoDO;
import cn.zhicloud.module.hr.dal.mysql.employee.HrEmployeeMapper;
import cn.zhicloud.module.hr.dal.mysql.hrm.HrmEmployeeChangeRecordMapper;
import cn.zhicloud.module.hr.dal.mysql.hrm.HrmEmployeeQuitInfoMapper;
import cn.zhicloud.module.system.api.dept.DeptApi;
import cn.zhicloud.module.system.api.dept.dto.DeptRespDTO;
import cn.zhicloud.module.system.api.notify.NotifyMessageSendApi;
import cn.zhicloud.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import cn.zhicloud.module.system.api.user.AdminUserApi;
import cn.zhicloud.module.system.api.user.dto.AdminUserRespDTO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.hr.enums.ErrorCodeConstants.*;

/**
 * HRM 员工档案 Service 实现类（/hrm/employee 融合链路）
 *
 * 融合纪律（台账 §12.184）：
 * * 单一真相 = hr_employee 超集列（V161）；旧列（gender/birth_date/hire_date/leave_date/status/employment_type）
 *   在每次写路径同步回写，保证旧 /hr 页面退役前语义一致。
 * * jobNumber/mobile/idNumber 是 DO 别名 getter（映射 emp_no/phone/id_card），VO→DO 显式转换。
 * * 生命周期动作（转正/调岗/晋升/降级/转全职/再入职）落 hr_employee_change_record；
 *   离职落 hr_employee_quit_info。
 *
 * @author zhicloud
 */
@Slf4j
@Service
@Validated
public class HrmEmployeeServiceImpl implements HrmEmployeeService {

    /** 状态页签（对齐前端 HrmEmployeeStatusTab） */
    private static final int TAB_ACTIVE = 11;
    private static final int TAB_FULL_TIME = 12;
    private static final int TAB_PENDING_ENTRY = 13;
    private static final int TAB_PENDING_LEAVE = 14;
    private static final int TAB_LEFT = 15;

    @Resource
    private HrEmployeeMapper employeeMapper;
    @Resource
    private HrmEmployeeChangeRecordMapper changeRecordMapper;
    @Resource
    private HrmEmployeeQuitInfoMapper quitInfoMapper;
    @Resource
    private DeptApi deptApi;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private NotifyMessageSendApi notifyMessageSendApi;

    // ========== 查询 ==========

    @Override
    public PageResult<HrmEmployeeRespVO> getEmployeePage(HrmEmployeePageReqVO pageReqVO) {
        Collection<Long> deptIds = expandDeptIds(pageReqVO.getDeptId());
        PageResult<HrEmployeeDO> pageResult = employeeMapper.selectHrmPage(pageReqVO, deptIds);
        PageResult<HrmEmployeeRespVO> respPage = new PageResult<>();
        respPage.setTotal(pageResult.getTotal());
        respPage.setList(buildRespVOList(pageResult.getList()));
        return respPage;
    }

    @Override
    public List<HrmEmployeeRespVO> getEmployeeList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return buildRespVOList(employeeMapper.selectByIds(ids));
    }

    @Override
    public List<HrmEmployeeRespVO> getEmployeeSimpleList(Collection<Long> ids) {
        return getEmployeeList(ids);
    }

    @Override
    public HrmEmployeeRespVO getEmployee(Long id) {
        HrEmployeeDO employee = validateEmployeeExists(id);
        return buildRespVOList(Collections.singletonList(employee)).get(0);
    }

    // ========== CRUD ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEmployee(HrmEmployeeSaveReqVO createReqVO) {
        HrEmployeeDO employee = convertToDO(createReqVO);
        // 1. 默认值
        if (employee.getEntryStatus() == null) {
            employee.setEntryStatus(1);
        }
        if (employee.getType() == null) {
            employee.setType(1);
        }
        // 2. 在职必填工号
        validateJobNumberRequired(employee);
        // 3. 工号唯一
        validateJobNumberUnique(null, employee.getEmpNo());
        // 4. 绑定用户唯一
        if (employee.getUserId() != null && !employeeMapper.selectListByUserIds(Collections.singleton(employee.getUserId())).isEmpty()) {
            throw exception(HRM_EMPLOYEE_USER_BOUND);
        }
        // 5. 直属上级存在性
        validateLeaderEmployeeId(employee.getLeaderEmployeeId());
        // 6. 正式员工转正时间兜底 + 司龄起点
        applyRegularAndCompanyAge(employee);
        // 7. 同步旧列 + 插入
        syncLegacyColumns(employee);
        employeeMapper.insert(employee);
        return employee.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEmployee(HrmEmployeeSaveReqVO updateReqVO) {
        HrEmployeeDO employee = validateEmployeeExists(updateReqVO.getId());
        // 1. 离职员工不可编辑
        if (Integer.valueOf(4).equals(employee.getEntryStatus())) {
            throw exception(HRM_EMPLOYEE_ALREADY_LEFT);
        }
        // 2. 工号唯一（排除自身）+ 在职必填
        HrEmployeeDO updateObj = convertToDO(updateReqVO);
        if (updateObj.getEntryStatus() == null) {
            updateObj.setEntryStatus(employee.getEntryStatus());
        }
        if (StrUtil.isEmpty(updateObj.getEmpNo()) && Integer.valueOf(1).equals(updateObj.getEntryStatus())) {
            throw exception(HRM_EMPLOYEE_JOB_NUMBER_REQUIRED);
        }
        if (StrUtil.isNotEmpty(updateObj.getEmpNo())) {
            validateJobNumberUnique(updateReqVO.getId(), updateObj.getEmpNo());
        }
        // 3. 换绑用户唯一
        if (updateObj.getUserId() != null && !updateObj.getUserId().equals(employee.getUserId())
                && !employeeMapper.selectListByUserIds(Collections.singleton(updateObj.getUserId())).isEmpty()) {
            throw exception(HRM_EMPLOYEE_USER_BOUND);
        }
        validateLeaderEmployeeId(updateObj.getLeaderEmployeeId());
        // 4. 正式员工转正时间兜底 + 司龄起点 + 同步旧列
        applyRegularAndCompanyAge(updateObj);
        syncLegacyColumns(updateObj);
        employeeMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEmployee(Long id) {
        HrEmployeeDO employee = validateEmployeeExists(id);
        if (!Integer.valueOf(4).equals(employee.getEntryStatus())) {
            throw exception(HRM_EMPLOYEE_STATUS_INVALID);
        }
        // 软删前释放 uk_emp_no 物理唯一槽位与后台用户绑定（历史行不再占用工号）
        releaseUniqueSlots(id);
        employeeMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEmployeeList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        List<HrEmployeeDO> employees = employeeMapper.selectByIds(ids);
        for (HrEmployeeDO employee : employees) {
            if (!Integer.valueOf(4).equals(employee.getEntryStatus())) {
                throw exception(HRM_EMPLOYEE_STATUS_INVALID);
            }
        }
        for (Long id : ids) {
            releaseUniqueSlots(id);
        }
        employeeMapper.deleteByIds(ids);
    }

    private void releaseUniqueSlots(Long id) {
        LambdaUpdateWrapper<HrEmployeeDO> release = new LambdaUpdateWrapper<HrEmployeeDO>()
                .eq(HrEmployeeDO::getId, id)
                .set(HrEmployeeDO::getEmpNo, null)
                .set(HrEmployeeDO::getUserId, null);
        employeeMapper.update(null, release);
    }

    // ========== 生命周期 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmEntry(HrmEmployeeSaveReqVO reqVO) {
        HrEmployeeDO employee = validateEmployeeExists(reqVO.getId());
        if (!Integer.valueOf(2).equals(employee.getEntryStatus())) {
            throw exception(HRM_EMPLOYEE_NOT_PENDING_ENTRY);
        }
        LocalDateTime entryTime = reqVO.getEntryTime() != null ? reqVO.getEntryTime() : LocalDateTime.now();
        LambdaUpdateWrapper<HrEmployeeDO> update = new LambdaUpdateWrapper<HrEmployeeDO>()
                .eq(HrEmployeeDO::getId, employee.getId())
                .set(HrEmployeeDO::getEntryStatus, 1)
                .set(HrEmployeeDO::getEntryTime, entryTime)
                .set(HrEmployeeDO::getCompanyAgeStartTime, entryTime)
                .set(HrEmployeeDO::getHireDate, entryTime.toLocalDate())
                .set(HrEmployeeDO::getStatus, 10);
        // 正式员工补转正时间
        Integer type = reqVO.getType() != null ? reqVO.getType() : employee.getType();
        if (Integer.valueOf(1).equals(type) && Integer.valueOf(1).equals(
                reqVO.getStatus() != null ? reqVO.getStatus() : employee.getEmployeeStatus())) {
            update.set(HrEmployeeDO::getRegularTime, entryTime);
        }
        if (reqVO.getProbation() != null) {
            update.set(HrEmployeeDO::getProbation, reqVO.getProbation());
        }
        employeeMapper.update(null, update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rehireEmployee(HrmEmployeeRehireReqVO reqVO) {
        HrEmployeeDO employee = validateEmployeeExists(reqVO.getEmployeeId());
        if (!Integer.valueOf(4).equals(employee.getEntryStatus())) {
            throw exception(HRM_EMPLOYEE_NOT_LEFT);
        }
        // 1. 快照旧值
        HrmEmployeeChangeRecordDO record = buildChangeRecord(employee, HrmEmployeeChangeRecordDO.TYPE_REHIRE, null,
                null, reqVO.getRemark());
        record.setOldDeptId(employee.getDeptId());
        record.setNewDeptId(reqVO.getDeptId());
        // 2. 更新员工
        HrEmployeeDO updateObj = convertToDO(reqVO);
        updateObj.setId(employee.getId());
        updateObj.setEntryStatus(1);
        if (updateObj.getEntryTime() == null) {
            updateObj.setEntryTime(LocalDateTime.now());
        }
        updateObj.setCompanyAgeStartTime(updateObj.getEntryTime());
        applyRegularAndCompanyAge(updateObj);
        syncLegacyColumns(updateObj);
        employeeMapper.updateById(updateObj);
        LambdaUpdateWrapper<HrEmployeeDO> clearLeave = new LambdaUpdateWrapper<HrEmployeeDO>()
                .eq(HrEmployeeDO::getId, employee.getId())
                .set(HrEmployeeDO::getLeaveTime, null)
                .set(HrEmployeeDO::getLeaveDate, null);
        employeeMapper.update(null, clearLeave);
        // 3. 异动记录（生效时间 = 再入职时间）
        record.setEffectTime(updateObj.getEntryTime());
        record.setNewDeptName(resolveDeptName(reqVO.getDeptId()));
        record.setOldPostName(employee.getPostName());
        record.setNewPostName(reqVO.getPostName());
        record.setOldLeaderEmployeeId(employee.getLeaderEmployeeId());
        record.setOldLeaderEmployeeName(resolveEmployeeName(employee.getLeaderEmployeeId()));
        record.setNewLeaderEmployeeId(reqVO.getLeaderEmployeeId());
        record.setNewLeaderEmployeeName(resolveEmployeeName(reqVO.getLeaderEmployeeId()));
        changeRecordMapper.insert(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void regularEmployee(HrmEmployeeChangeReqVO reqVO) {
        HrEmployeeDO employee = validateInService(reqVO.getEmployeeId());
        // 转正：试用(2) → 正式(1)，补转正时间
        HrmEmployeeChangeRecordDO record = applyChange(employee, HrmEmployeeChangeRecordDO.TYPE_REGULAR, reqVO);
        LambdaUpdateWrapper<HrEmployeeDO> update = baseChangeUpdate(employee, reqVO)
                .set(HrEmployeeDO::getEmployeeStatus, 1)
                .set(HrEmployeeDO::getRegularTime,
                        reqVO.getEffectTime() != null ? reqVO.getEffectTime() : LocalDateTime.now());
        employeeMapper.update(null, update);
        changeRecordMapper.insert(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferEmployee(HrmEmployeeChangeReqVO reqVO) {
        HrEmployeeDO employee = validateInService(reqVO.getEmployeeId());
        HrmEmployeeChangeRecordDO record = applyChange(employee, HrmEmployeeChangeRecordDO.TYPE_TRANSFER, reqVO);
        employeeMapper.update(null, baseChangeUpdate(employee, reqVO));
        changeRecordMapper.insert(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void promoteEmployee(HrmEmployeeChangeReqVO reqVO) {
        HrEmployeeDO employee = validateInService(reqVO.getEmployeeId());
        HrmEmployeeChangeRecordDO record = applyChange(employee, HrmEmployeeChangeRecordDO.TYPE_PROMOTE, reqVO);
        employeeMapper.update(null, baseChangeUpdate(employee, reqVO));
        changeRecordMapper.insert(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void demoteEmployee(HrmEmployeeChangeReqVO reqVO) {
        HrEmployeeDO employee = validateInService(reqVO.getEmployeeId());
        HrmEmployeeChangeRecordDO record = applyChange(employee, HrmEmployeeChangeRecordDO.TYPE_DEMOTE, reqVO);
        employeeMapper.update(null, baseChangeUpdate(employee, reqVO));
        changeRecordMapper.insert(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void convertToFullTimeEmployee(HrmEmployeeChangeReqVO reqVO) {
        HrEmployeeDO employee = validateInService(reqVO.getEmployeeId());
        // 转全职：type → 1；带试用期则员工状态 → 试用(2)，否则 → 正式(1)
        HrmEmployeeChangeRecordDO record = applyChange(employee, HrmEmployeeChangeRecordDO.TYPE_CONVERT_FULL_TIME, reqVO);
        record.setProbation(reqVO.getProbation());
        LambdaUpdateWrapper<HrEmployeeDO> update = baseChangeUpdate(employee, reqVO)
                .set(HrEmployeeDO::getType, 1)
                .set(HrEmployeeDO::getEmploymentType, 10); // 全职对应旧口径 10 全职
        if (reqVO.getProbation() != null) {
            update.set(HrEmployeeDO::getEmployeeStatus, 2)
                    .set(HrEmployeeDO::getProbation, reqVO.getProbation());
        } else {
            update.set(HrEmployeeDO::getEmployeeStatus, 1);
        }
        employeeMapper.update(null, update);
        changeRecordMapper.insert(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void quitEmployee(HrmEmployeeQuitReqVO reqVO) {
        HrEmployeeDO employee = validateEmployeeExists(reqVO.getEmployeeId());
        if (Integer.valueOf(4).equals(employee.getEntryStatus())) {
            throw exception(HRM_EMPLOYEE_ALREADY_LEFT);
        }
        LocalDateTime leaveTime = reqVO.getPlanQuitTime() != null ? reqVO.getPlanQuitTime() : LocalDateTime.now();
        // 1. 员工主表：离职态 + 同步旧列
        LambdaUpdateWrapper<HrEmployeeDO> update = new LambdaUpdateWrapper<HrEmployeeDO>()
                .eq(HrEmployeeDO::getId, employee.getId())
                .set(HrEmployeeDO::getEntryStatus, 4)
                .set(HrEmployeeDO::getLeaveTime, leaveTime)
                .set(HrEmployeeDO::getLeaveDate, leaveTime.toLocalDate())
                .set(HrEmployeeDO::getStatus, 20);
        employeeMapper.update(null, update);
        // 2. 离职信息表（一员工一条：先删旧，保最新）
        HrmEmployeeQuitInfoDO quitInfo = quitInfoMapper.selectByEmployeeId(employee.getId());
        if (quitInfo == null) {
            quitInfo = HrmEmployeeQuitInfoDO.builder().employeeId(employee.getId()).build();
        }
        quitInfo.setPlanQuitTime(reqVO.getPlanQuitTime());
        quitInfo.setApplyQuitTime(reqVO.getApplyQuitTime());
        quitInfo.setSalarySettlementTime(reqVO.getSalarySettlementTime());
        quitInfo.setType(reqVO.getType());
        quitInfo.setReason(reqVO.getReason());
        quitInfo.setOldEmployeeStatus(employee.getEmployeeStatus());
        quitInfo.setRemark(reqVO.getRemark());
        quitInfo.setDeleted(false);
        if (quitInfo.getId() == null) {
            quitInfoMapper.insert(quitInfo);
        } else {
            quitInfoMapper.updateById(quitInfo);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelQuitEmployee(HrmEmployeeCancelQuitReqVO reqVO) {
        HrEmployeeDO employee = validateEmployeeExists(reqVO.getEmployeeId());
        if (!Integer.valueOf(4).equals(employee.getEntryStatus())) {
            throw exception(HRM_EMPLOYEE_NOT_LEFT);
        }
        LambdaUpdateWrapper<HrEmployeeDO> update = new LambdaUpdateWrapper<HrEmployeeDO>()
                .eq(HrEmployeeDO::getId, employee.getId())
                .set(HrEmployeeDO::getEntryStatus, 1)
                .set(HrEmployeeDO::getLeaveTime, null)
                .set(HrEmployeeDO::getLeaveDate, null)
                .set(HrEmployeeDO::getStatus, 10);
        employeeMapper.update(null, update);
    }

    // ========== 统计 ==========

    @Override
    public List<HrmEmployeeStatusCountRespVO> getEmployeeStatusCount(HrmEmployeePageReqVO reqVO) {
        // 复用页签过滤语义逐 tab 计数（忽略请求里自带的 statusCategory，其余筛选照常生效）
        reqVO.setStatusCategory(null);
        Collection<Long> deptIds = expandDeptIds(reqVO.getDeptId());
        List<HrmEmployeeStatusCountRespVO> result = new ArrayList<>(5);
        result.add(buildStatusCount(TAB_ACTIVE, employeeMapper.selectHrmCountByFilter(reqVO, deptIds, Arrays.asList(1, 2, 3), null)));
        result.add(buildStatusCount(TAB_FULL_TIME, employeeMapper.selectHrmCountByFilter(reqVO, deptIds, Arrays.asList(1, 2, 3), 1)));
        result.add(buildStatusCount(TAB_PENDING_ENTRY, employeeMapper.selectHrmCountByFilter(reqVO, deptIds, Collections.singletonList(2), null)));
        result.add(buildStatusCount(TAB_PENDING_LEAVE, employeeMapper.selectHrmCountByFilter(reqVO, deptIds, Collections.singletonList(3), null)));
        result.add(buildStatusCount(TAB_LEFT, employeeMapper.selectHrmCountByFilter(reqVO, deptIds, Collections.singletonList(4), null)));
        return result;
    }

    private HrmEmployeeStatusCountRespVO buildStatusCount(int tab, Long count) {
        HrmEmployeeStatusCountRespVO vo = new HrmEmployeeStatusCountRespVO();
        vo.setStatus(tab);
        vo.setCount(count != null ? count : 0L);
        return vo;
    }

    @Override
    public List<HrmEmployeeDeptStatisticsRespVO> getEmployeeDeptStatistics() {
        List<HrEmployeeDO> actives = employeeMapper.selectHrmListByEntryStatusIn(Arrays.asList(1, 2, 3));
        Map<Long, List<HrEmployeeDO>> byDept = actives.stream()
                .filter(e -> e.getDeptId() != null)
                .collect(Collectors.groupingBy(HrEmployeeDO::getDeptId));
        List<HrmEmployeeDeptStatisticsRespVO> result = new ArrayList<>(byDept.size());
        for (Map.Entry<Long, List<HrEmployeeDO>> entry : byDept.entrySet()) {
            HrmEmployeeDeptStatisticsRespVO vo = new HrmEmployeeDeptStatisticsRespVO();
            vo.setDeptId(entry.getKey());
            vo.setActiveCount((long) entry.getValue().size());
            vo.setFullTimeCount(entry.getValue().stream().filter(e -> Integer.valueOf(1).equals(e.getType())).count());
            vo.setNonFullTimeCount(vo.getActiveCount() - vo.getFullTimeCount());
            result.add(vo);
        }
        return result;
    }

    // ========== 建档 / 通知 / 导入 ==========

    @Override
    public List<Long> getBoundUserIdList() {
        return employeeMapper.selectBoundUserIdList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> createEmployeeListFromUser(List<HrmEmployeeCreateFromUserReqVO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 1. 批量校验用户存在 + 未建档
        Set<Long> userIds = list.stream().map(HrmEmployeeCreateFromUserReqVO::getUserId).collect(Collectors.toSet());
        List<AdminUserRespDTO> users = adminUserApi.getUserList(userIds);
        Map<Long, AdminUserRespDTO> userMap = users.stream()
                .collect(Collectors.toMap(AdminUserRespDTO::getId, u -> u));
        Set<Long> boundUserIds = new HashSet<>(employeeMapper.selectBoundUserIdList());
        // 2. 工号查重
        Set<String> jobNumbers = new HashSet<>();
        for (HrmEmployeeCreateFromUserReqVO req : list) {
            jobNumbers.add(req.getJobNumber());
        }
        for (String jobNumber : jobNumbers) {
            if (employeeMapper.selectByEmpNo(jobNumber) != null) {
                throw exception(HRM_EMPLOYEE_JOB_NUMBER_DUPLICATE);
            }
        }
        // 3. 逐个建档
        List<Long> result = new ArrayList<>(list.size());
        for (HrmEmployeeCreateFromUserReqVO req : list) {
            AdminUserRespDTO user = userMap.get(req.getUserId());
            if (user == null) {
                throw exception(HRM_EMPLOYEE_USER_BOUND);
            }
            if (boundUserIds.contains(req.getUserId())) {
                throw exception(HRM_EMPLOYEE_USER_BOUND);
            }
            HrEmployeeDO employee = new HrEmployeeDO();
            employee.setUserId(req.getUserId());
            employee.setName(user.getNickname());
            employee.setEmpNo(req.getJobNumber());
            employee.setPhone(req.getMobile());
            employee.setDeptId(req.getDeptId());
            employee.setLeaderEmployeeId(req.getLeaderEmployeeId());
            employee.setType(req.getType());
            employee.setEntryStatus(1);
            // 员工状态：正式 → 1；非正式 → 请求里的状态（默认试用 2）
            if (Integer.valueOf(1).equals(req.getType())) {
                employee.setEmployeeStatus(1);
                employee.setRegularTime(req.getEntryTime());
            } else {
                employee.setEmployeeStatus(req.getStatus() != null ? req.getStatus() : 2);
            }
            employee.setEntryTime(req.getEntryTime());
            employee.setCompanyAgeStartTime(req.getEntryTime());
            employee.setProbation(req.getProbation());
            employee.setPostName(req.getPostName());
            employee.setPostLevel(req.getPostLevel());
            employee.setWorkCity(req.getWorkCity());
            employee.setWorkAddress(req.getWorkAddress());
            employee.setRemark(req.getRemark());
            employee.setEmploymentType(legacyEmploymentType(req.getType(), employee.getEmployeeStatus()));
            employee.setStatus(10);
            employee.setHireDate(req.getEntryTime() != null ? req.getEntryTime().toLocalDate() : null);
            employeeMapper.insert(employee);
            boundUserIds.add(req.getUserId());
            result.add(employee.getId());
        }
        return result;
    }

    @Override
    public HrmEmployeeNotifyRespVO sendProfileFillMessage(Collection<Long> ids) {
        List<HrEmployeeDO> employees = employeeMapper.selectByIds(ids);
        HrmEmployeeNotifyRespVO resp = new HrmEmployeeNotifyRespVO();
        resp.setSuccessCount(0);
        resp.setSkippedCount(0);
        resp.setFailureCount(0);
        for (HrEmployeeDO employee : employees) {
            if (employee.getUserId() == null) {
                resp.setSkippedCount(resp.getSkippedCount() + 1);
                continue;
            }
            try {
                NotifySendSingleToUserReqDTO reqDTO = new NotifySendSingleToUserReqDTO();
                reqDTO.setUserId(employee.getUserId());
                reqDTO.setTemplateCode("hrm_employee_profile_fill");
                Map<String, Object> params = new HashMap<>(2);
                params.put("name", employee.getName());
                reqDTO.setTemplateParams(params);
                notifyMessageSendApi.sendSingleMessageToAdmin(reqDTO);
                resp.setSuccessCount(resp.getSuccessCount() + 1);
            } catch (Exception ex) {
                log.warn("[sendProfileFillMessage] 员工[{}]档案填写通知发送失败", employee.getId(), ex);
                resp.setFailureCount(resp.getFailureCount() + 1);
            }
        }
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HrmEmployeeImportRespVO importEmployeeList(List<HrmEmployeeImportExcelVO> list, Integer duplicateStrategy) {
        HrmEmployeeImportRespVO resp = new HrmEmployeeImportRespVO();
        resp.setCreateJobNumbers(new ArrayList<>());
        resp.setUpdateJobNumbers(new ArrayList<>());
        resp.setSkipJobNumbers(new ArrayList<>());
        resp.setFailureJobNumbers(new HashMap<>());
        for (HrmEmployeeImportExcelVO row : list) {
            if (StrUtil.isEmpty(row.getName())) {
                continue;
            }
            String jobNumber = StrUtil.emptyIfNull(row.getJobNumber());
            HrEmployeeDO exist = StrUtil.isNotEmpty(jobNumber) ? employeeMapper.selectByEmpNo(jobNumber) : null;
            if (exist != null) {
                // 重复工号按策略处理：1 跳过 2 覆盖 3 判失败
                if (Integer.valueOf(1).equals(duplicateStrategy)) {
                    resp.getSkipJobNumbers().add(jobNumber);
                    continue;
                }
                if (Integer.valueOf(3).equals(duplicateStrategy)) {
                    resp.getFailureJobNumbers().put(jobNumber, "工号已存在");
                    continue;
                }
                // 覆盖
                HrEmployeeDO updateObj = new HrEmployeeDO();
                updateObj.setId(exist.getId());
                applyImportRow(updateObj, row);
                syncLegacyColumns(updateObj);
                employeeMapper.updateById(updateObj);
                resp.getUpdateJobNumbers().add(jobNumber);
                continue;
            }
            HrEmployeeDO employee = new HrEmployeeDO();
            employee.setName(row.getName());
            employee.setEntryStatus(1);
            employee.setType(row.getType() != null ? row.getType() : 1);
            applyImportRow(employee, row);
            if (StrUtil.isEmpty(employee.getEmpNo())) {
                resp.getFailureJobNumbers().put(jobNumber, "在职员工工号不能为空");
                continue;
            }
            if (employeeMapper.selectByEmpNo(employee.getEmpNo()) != null) {
                resp.getFailureJobNumbers().put(employee.getEmpNo(), "工号已存在");
                continue;
            }
            applyRegularAndCompanyAge(employee);
            syncLegacyColumns(employee);
            employeeMapper.insert(employee);
            resp.getCreateJobNumbers().add(employee.getEmpNo());
        }
        return resp;
    }

    private void applyImportRow(HrEmployeeDO employee, HrmEmployeeImportExcelVO row) {
        employee.setEmpNo(StrUtil.isNotEmpty(row.getJobNumber()) ? row.getJobNumber() : employee.getEmpNo());
        employee.setPhone(row.getMobile());
        employee.setSex(row.getSex());
        employee.setEmail(row.getEmail());
        employee.setDeptId(row.getDeptId());
        employee.setPostName(row.getPostName());
        if (row.getType() != null) {
            employee.setType(row.getType());
        }
        employee.setEmployeeStatus(row.getStatus());
        employee.setEntryTime(row.getEntryTime());
        employee.setIdCard(row.getIdNumber());
        employee.setRemark(row.getRemark());
    }

    // ========== 私有：转换 / 校验 / 同步 / enrich ==========

    /**
     * VO → DO（别名字段显式映射：jobNumber→emp_no、mobile→phone、idNumber→id_card；
     * status 轴迁移：VO 的 status 是员工状态轴（employee_status），须防同名旧列误拷）
     */
    private HrEmployeeDO convertToDO(HrmEmployeeSaveReqVO reqVO) {
        HrEmployeeDO employee = BeanUtils.toBean(reqVO, HrEmployeeDO.class);
        employee.setEmpNo(reqVO.getJobNumber());
        employee.setPhone(reqVO.getMobile());
        employee.setIdCard(reqVO.getIdNumber());
        if (reqVO.getStatus() != null) {
            employee.setEmployeeStatus(reqVO.getStatus());
            employee.setStatus(null); // 旧列由 syncLegacyColumns 统一回写
        }
        return employee;
    }

    private void validateJobNumberRequired(HrEmployeeDO employee) {
        if (Integer.valueOf(1).equals(employee.getEntryStatus()) && StrUtil.isEmpty(employee.getEmpNo())) {
            throw exception(HRM_EMPLOYEE_JOB_NUMBER_REQUIRED);
        }
    }

    private void validateJobNumberUnique(Long id, String empNo) {
        if (StrUtil.isEmpty(empNo)) {
            return;
        }
        HrEmployeeDO exist = employeeMapper.selectByEmpNo(empNo);
        if (exist != null && (id == null || !exist.getId().equals(id))) {
            throw exception(HRM_EMPLOYEE_JOB_NUMBER_DUPLICATE);
        }
    }

    private void validateLeaderEmployeeId(Long leaderEmployeeId) {
        if (leaderEmployeeId != null && employeeMapper.selectById(leaderEmployeeId) == null) {
            throw exception(HRM_EMPLOYEE_NOT_EXISTS);
        }
    }

    private HrEmployeeDO validateEmployeeExists(Long id) {
        HrEmployeeDO employee = employeeMapper.selectById(id);
        if (employee == null) {
            throw exception(HRM_EMPLOYEE_NOT_EXISTS);
        }
        return employee;
    }

    private HrEmployeeDO validateInService(Long id) {
        HrEmployeeDO employee = validateEmployeeExists(id);
        if (Integer.valueOf(4).equals(employee.getEntryStatus())) {
            throw exception(HRM_EMPLOYEE_ALREADY_LEFT);
        }
        return employee;
    }

    /**
     * 正式员工转正时间兜底（regularTime = entryTime）+ 司龄起点（companyAgeStartTime = entryTime）
     */
    private void applyRegularAndCompanyAge(HrEmployeeDO employee) {
        if (employee.getEntryTime() != null && employee.getCompanyAgeStartTime() == null) {
            employee.setCompanyAgeStartTime(employee.getEntryTime());
        }
        if (Integer.valueOf(1).equals(employee.getType()) && Integer.valueOf(1).equals(employee.getEmployeeStatus())
                && employee.getRegularTime() == null) {
            employee.setRegularTime(employee.getEntryTime());
        }
    }

    /**
     * 新列 → 旧列同步（updateById 非 null 才更新，旧页面语义保持）
     */
    private void syncLegacyColumns(HrEmployeeDO employee) {
        if (employee.getSex() != null) {
            employee.setGender(employee.getSex() == 1 ? 10 : 20);
        }
        if (employee.getBirthday() != null) {
            employee.setBirthDate(employee.getBirthday().toLocalDate());
        }
        if (employee.getEntryTime() != null) {
            employee.setHireDate(employee.getEntryTime().toLocalDate());
        }
        if (employee.getLeaveTime() != null) {
            employee.setLeaveDate(employee.getLeaveTime().toLocalDate());
        }
        if (employee.getType() != null) {
            employee.setEmploymentType(legacyEmploymentType(employee.getType(), employee.getEmployeeStatus()));
        }
        if (employee.getEntryStatus() != null) {
            employee.setStatus(employee.getEntryStatus() == 4 ? 20 : 10);
        }
    }

    /**
     * 新员工状态轴 → 旧 employment_type：1正式/2试用/6顾问/7返聘→10全职；4兼职/5劳务→20兼职；3实习→30实习；8外包→40外包
     */
    private Integer legacyEmploymentType(Integer type, Integer employeeStatus) {
        if (type == null || !Integer.valueOf(1).equals(type)) {
            // 非正式：按员工状态折叠
            if (employeeStatus == null) {
                return 20;
            }
            switch (employeeStatus) {
                case 3:
                    return 30;
                case 8:
                    return 40;
                default:
                    return 20;
            }
        }
        return 10;
    }

    /**
     * 部门子树展开（含自身）
     */
    private Collection<Long> expandDeptIds(Long deptId) {
        if (deptId == null) {
            return Collections.emptyList();
        }
        Set<Long> deptIds = new HashSet<>();
        deptIds.add(deptId);
        deptApi.getChildDeptList(deptId).forEach(dept -> deptIds.add(dept.getId()));
        return deptIds;
    }

    private String resolveDeptName(Long deptId) {
        if (deptId == null) {
            return null;
        }
        DeptRespDTO dept = deptApi.getDept(deptId);
        return dept != null ? dept.getName() : null;
    }

    private String resolveEmployeeName(Long employeeId) {
        if (employeeId == null) {
            return null;
        }
        HrEmployeeDO employee = employeeMapper.selectById(employeeId);
        return employee != null ? employee.getName() : null;
    }

    /**
     * 构建异动记录骨架（含旧值快照；新值由调用方补充）
     */
    private HrmEmployeeChangeRecordDO buildChangeRecord(HrEmployeeDO employee, int type, Integer reason,
                                                        LocalDateTime effectTime, String remark) {
        HrmEmployeeChangeRecordDO record = HrmEmployeeChangeRecordDO.builder()
                .employeeId(employee.getId())
                .type(type)
                .reason(reason)
                .effectTime(effectTime)
                .remark(remark)
                .build();
        record.setOldDeptId(employee.getDeptId());
        record.setOldDeptName(resolveDeptName(employee.getDeptId()));
        record.setOldPostName(employee.getPostName());
        record.setOldPostLevel(employee.getPostLevel());
        record.setOldWorkAddress(employee.getWorkAddress());
        record.setOldLeaderEmployeeId(employee.getLeaderEmployeeId());
        record.setOldLeaderEmployeeName(resolveEmployeeName(employee.getLeaderEmployeeId()));
        return record;
    }

    /**
     * 异动公共逻辑：快照 + 新值填充（deptName/leaderName 快照在 insert 前解析）
     */
    private HrmEmployeeChangeRecordDO applyChange(HrEmployeeDO employee, int type, HrmEmployeeChangeReqVO reqVO) {
        HrmEmployeeChangeRecordDO record = buildChangeRecord(employee, type, reqVO.getReason(),
                reqVO.getEffectTime(), reqVO.getRemark());
        record.setNewDeptId(reqVO.getNewDeptId());
        record.setNewDeptName(resolveDeptName(reqVO.getNewDeptId()));
        record.setNewPostName(reqVO.getNewPostName());
        record.setNewPostLevel(reqVO.getNewPostLevel());
        record.setNewWorkAddress(reqVO.getNewWorkAddress());
        record.setNewLeaderEmployeeId(reqVO.getNewLeaderEmployeeId());
        record.setNewLeaderEmployeeName(resolveEmployeeName(reqVO.getNewLeaderEmployeeId()));
        return record;
    }

    /**
     * 异动公共 UPDATE：仅应用请求里非空的字段
     */
    private LambdaUpdateWrapper<HrEmployeeDO> baseChangeUpdate(HrEmployeeDO employee, HrmEmployeeChangeReqVO reqVO) {
        LambdaUpdateWrapper<HrEmployeeDO> update = new LambdaUpdateWrapper<HrEmployeeDO>()
                .eq(HrEmployeeDO::getId, employee.getId());
        if (reqVO.getNewDeptId() != null) {
            update.set(HrEmployeeDO::getDeptId, reqVO.getNewDeptId());
        }
        if (StrUtil.isNotEmpty(reqVO.getNewPostName())) {
            update.set(HrEmployeeDO::getPostName, reqVO.getNewPostName());
        }
        if (StrUtil.isNotEmpty(reqVO.getNewPostLevel())) {
            update.set(HrEmployeeDO::getPostLevel, reqVO.getNewPostLevel());
        }
        if (StrUtil.isNotEmpty(reqVO.getNewWorkAddress())) {
            update.set(HrEmployeeDO::getWorkAddress, reqVO.getNewWorkAddress());
        }
        if (reqVO.getNewLeaderEmployeeId() != null) {
            update.set(HrEmployeeDO::getLeaderEmployeeId, reqVO.getNewLeaderEmployeeId());
        }
        return update;
    }

    /**
     * DO → RespVO 批量 enrich（deptName/leaderEmployeeName/userNickname/age/companyAge）
     */
    private List<HrmEmployeeRespVO> buildRespVOList(List<HrEmployeeDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        List<HrmEmployeeRespVO> voList = BeanUtils.toBean(list, HrmEmployeeRespVO.class);
        // 1. 部门名称
        Set<Long> deptIds = new HashSet<>();
        for (HrEmployeeDO employee : list) {
            if (employee.getDeptId() != null) {
                deptIds.add(employee.getDeptId());
            }
        }
        Map<Long, DeptRespDTO> deptMap = deptIds.isEmpty() ? Collections.emptyMap()
                : deptApi.getDeptMap(deptIds);
        // 2. 直属上级姓名
        Set<Long> leaderIds = new HashSet<>();
        for (HrEmployeeDO employee : list) {
            if (employee.getLeaderEmployeeId() != null) {
                leaderIds.add(employee.getLeaderEmployeeId());
            }
        }
        Map<Long, String> leaderNameMap = new HashMap<>();
        if (!leaderIds.isEmpty()) {
            employeeMapper.selectByIds(leaderIds).forEach(emp -> leaderNameMap.put(emp.getId(), emp.getName()));
        }
        // 3. 后台用户昵称
        Set<Long> userIds = new HashSet<>();
        for (HrEmployeeDO employee : list) {
            if (employee.getUserId() != null) {
                userIds.add(employee.getUserId());
            }
        }
        Map<Long, AdminUserRespDTO> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : adminUserApi.getUserList(userIds).stream()
                        .collect(Collectors.toMap(AdminUserRespDTO::getId, u -> u));
        // 4. 逐个填充
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < list.size(); i++) {
            HrEmployeeDO employee = list.get(i);
            HrmEmployeeRespVO vo = voList.get(i);
            // status 轴迁移：RespVO 的 status 是员工状态轴（employee_status），防同名旧列误拷
            vo.setStatus(employee.getEmployeeStatus());
            // 别名字段显式映射（hutool 按 getter 拷贝不可靠，单一物理列三别名逐一赋值）
            vo.setJobNumber(employee.getEmpNo());
            vo.setMobile(employee.getPhone());
            vo.setIdNumber(employee.getIdCard());
            DeptRespDTO dept = deptMap.get(employee.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getName());
            }
            vo.setLeaderEmployeeName(leaderNameMap.get(employee.getLeaderEmployeeId()));
            AdminUserRespDTO user = userMap.get(employee.getUserId());
            if (user != null) {
                vo.setUserNickname(user.getNickname());
            }
            if (employee.getBirthday() != null) {
                vo.setAge(Period.between(employee.getBirthday().toLocalDate(), now.toLocalDate()).getYears());
            }
            if (employee.getCompanyAgeStartTime() != null) {
                vo.setCompanyAge((int) ChronoUnit.YEARS.between(employee.getCompanyAgeStartTime(), now));
            }
            // channelName：招聘渠道域未接线，暂缺（不伪造）
        }
        return voList;
    }

}
