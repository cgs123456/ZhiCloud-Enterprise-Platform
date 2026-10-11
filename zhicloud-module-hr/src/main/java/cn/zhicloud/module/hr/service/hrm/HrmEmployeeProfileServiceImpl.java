package cn.zhicloud.module.hr.service.hrm;

import cn.hutool.core.collection.CollUtil;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.security.core.util.SecurityFrameworkUtils;
import cn.zhicloud.framework.tenant.core.util.TenantUtils;
import cn.zhicloud.module.hr.controller.admin.hrm.vo.*;
import cn.zhicloud.module.hr.dal.dataobject.employee.HrEmployeeDO;
import cn.zhicloud.module.hr.dal.dataobject.hrm.*;
import cn.zhicloud.module.hr.dal.mysql.employee.HrEmployeeMapper;
import cn.zhicloud.module.hr.dal.mysql.hrm.*;
import cn.zhicloud.module.system.api.user.AdminUserApi;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.hr.enums.ErrorCodeConstants.HRM_EMPLOYEE_NOT_EXISTS;
import static cn.zhicloud.module.hr.enums.ErrorCodeConstants.HRM_EMPLOYEE_SUB_RECORD_NOT_EXISTS;

/**
 * HRM 员工档案子域 Service 实现类
 *
 * 写路径统一校验员工存在（租户隔离由 MP 租户插件兜底）；附件保存按
 * (employeeId, type) 整组替换；工资卡为员工单卡，落 hr_employee salary_card_* 五列。
 *
 * @author zhicloud
 */
@Service
@Validated
public class HrmEmployeeProfileServiceImpl implements HrmEmployeeProfileService {

    @Resource
    private HrEmployeeMapper employeeMapper;
    @Resource
    private HrmEmployeeCertificateMapper certificateMapper;
    @Resource
    private HrmEmployeeContactMapper contactMapper;
    @Resource
    private HrmEmployeeEducationExperienceMapper educationExperienceMapper;
    @Resource
    private HrmEmployeeWorkExperienceMapper workExperienceMapper;
    @Resource
    private HrmEmployeeTrainingExperienceMapper trainingExperienceMapper;
    @Resource
    private HrmEmployeeContractMapper contractMapper;
    @Resource
    private HrmEmployeeChangeRecordMapper changeRecordMapper;
    @Resource
    private HrmEmployeeQuitInfoMapper quitInfoMapper;
    @Resource
    private HrmEmployeeFileMapper hrmEmployeeFileMapper;
    @Resource
    private HrmEmployeeFieldConfigMapper fieldConfigMapper;
    @Resource
    private HrmEmployeePersonalNoteMapper personalNoteMapper;
    @Resource
    private AdminUserApi adminUserApi;

    // ========== 证书 ==========

    @Override
    public List<HrmEmployeeCertificateVO> getCertificateList(Long employeeId) {
        validateEmployeeExists(employeeId);
        return BeanUtils.toBean(certificateMapper.selectListByEmployeeId(employeeId), HrmEmployeeCertificateVO.class);
    }

    @Override
    public Long createCertificate(HrmEmployeeCertificateVO reqVO) {
        validateEmployeeExists(reqVO.getEmployeeId());
        HrmEmployeeCertificateDO cert = BeanUtils.toBean(reqVO, HrmEmployeeCertificateDO.class);
        certificateMapper.insert(cert);
        return cert.getId();
    }

    @Override
    public void updateCertificate(HrmEmployeeCertificateVO reqVO) {
        validateSubRecordExists(certificateMapper.selectById(reqVO.getId()));
        certificateMapper.updateById(BeanUtils.toBean(reqVO, HrmEmployeeCertificateDO.class));
    }

    @Override
    public void deleteCertificate(Long id) {
        validateSubRecordExists(certificateMapper.selectById(id));
        certificateMapper.deleteById(id);
    }

    // ========== 紧急联系人 ==========

    @Override
    public List<HrmEmployeeContactVO> getContactList(Long employeeId) {
        validateEmployeeExists(employeeId);
        return BeanUtils.toBean(contactMapper.selectListByEmployeeId(employeeId), HrmEmployeeContactVO.class);
    }

    @Override
    public Long createContact(HrmEmployeeContactVO reqVO) {
        validateEmployeeExists(reqVO.getEmployeeId());
        HrmEmployeeContactDO contact = BeanUtils.toBean(reqVO, HrmEmployeeContactDO.class);
        contactMapper.insert(contact);
        return contact.getId();
    }

    @Override
    public void updateContact(HrmEmployeeContactVO reqVO) {
        validateSubRecordExists(contactMapper.selectById(reqVO.getId()));
        contactMapper.updateById(BeanUtils.toBean(reqVO, HrmEmployeeContactDO.class));
    }

    @Override
    public void deleteContact(Long id) {
        validateSubRecordExists(contactMapper.selectById(id));
        contactMapper.deleteById(id);
    }

    // ========== 教育经历 ==========

    @Override
    public List<HrmEmployeeEducationExperienceVO> getEducationExperienceList(Long employeeId) {
        validateEmployeeExists(employeeId);
        return BeanUtils.toBean(educationExperienceMapper.selectListByEmployeeId(employeeId),
                HrmEmployeeEducationExperienceVO.class);
    }

    @Override
    public Long createEducationExperience(HrmEmployeeEducationExperienceVO reqVO) {
        validateEmployeeExists(reqVO.getEmployeeId());
        HrmEmployeeEducationExperienceDO record = BeanUtils.toBean(reqVO, HrmEmployeeEducationExperienceDO.class);
        educationExperienceMapper.insert(record);
        return record.getId();
    }

    @Override
    public void updateEducationExperience(HrmEmployeeEducationExperienceVO reqVO) {
        validateSubRecordExists(educationExperienceMapper.selectById(reqVO.getId()));
        educationExperienceMapper.updateById(BeanUtils.toBean(reqVO, HrmEmployeeEducationExperienceDO.class));
    }

    @Override
    public void deleteEducationExperience(Long id) {
        validateSubRecordExists(educationExperienceMapper.selectById(id));
        educationExperienceMapper.deleteById(id);
    }

    // ========== 工作经历 ==========

    @Override
    public List<HrmEmployeeWorkExperienceVO> getWorkExperienceList(Long employeeId) {
        validateEmployeeExists(employeeId);
        return BeanUtils.toBean(workExperienceMapper.selectListByEmployeeId(employeeId),
                HrmEmployeeWorkExperienceVO.class);
    }

    @Override
    public Long createWorkExperience(HrmEmployeeWorkExperienceVO reqVO) {
        validateEmployeeExists(reqVO.getEmployeeId());
        HrmEmployeeWorkExperienceDO record = BeanUtils.toBean(reqVO, HrmEmployeeWorkExperienceDO.class);
        workExperienceMapper.insert(record);
        return record.getId();
    }

    @Override
    public void updateWorkExperience(HrmEmployeeWorkExperienceVO reqVO) {
        validateSubRecordExists(workExperienceMapper.selectById(reqVO.getId()));
        workExperienceMapper.updateById(BeanUtils.toBean(reqVO, HrmEmployeeWorkExperienceDO.class));
    }

    @Override
    public void deleteWorkExperience(Long id) {
        validateSubRecordExists(workExperienceMapper.selectById(id));
        workExperienceMapper.deleteById(id);
    }

    // ========== 培训经历 ==========

    @Override
    public List<HrmEmployeeTrainingExperienceVO> getTrainingExperienceList(Long employeeId) {
        validateEmployeeExists(employeeId);
        return BeanUtils.toBean(trainingExperienceMapper.selectListByEmployeeId(employeeId),
                HrmEmployeeTrainingExperienceVO.class);
    }

    @Override
    public Long createTrainingExperience(HrmEmployeeTrainingExperienceVO reqVO) {
        validateEmployeeExists(reqVO.getEmployeeId());
        HrmEmployeeTrainingExperienceDO record = BeanUtils.toBean(reqVO, HrmEmployeeTrainingExperienceDO.class);
        trainingExperienceMapper.insert(record);
        return record.getId();
    }

    @Override
    public void updateTrainingExperience(HrmEmployeeTrainingExperienceVO reqVO) {
        validateSubRecordExists(trainingExperienceMapper.selectById(reqVO.getId()));
        trainingExperienceMapper.updateById(BeanUtils.toBean(reqVO, HrmEmployeeTrainingExperienceDO.class));
    }

    @Override
    public void deleteTrainingExperience(Long id) {
        validateSubRecordExists(trainingExperienceMapper.selectById(id));
        trainingExperienceMapper.deleteById(id);
    }

    // ========== 合同 ==========

    @Override
    public List<HrmEmployeeContractVO> getContractList(Long employeeId) {
        validateEmployeeExists(employeeId);
        return BeanUtils.toBean(contractMapper.selectListByEmployeeId(employeeId), HrmEmployeeContractVO.class);
    }

    @Override
    public Long createContract(HrmEmployeeContractVO reqVO) {
        validateEmployeeExists(reqVO.getEmployeeId());
        HrmEmployeeContractDO record = BeanUtils.toBean(reqVO, HrmEmployeeContractDO.class);
        if (record.getStatus() == null) {
            record.setStatus(0);
        }
        contractMapper.insert(record);
        return record.getId();
    }

    @Override
    public void updateContract(HrmEmployeeContractVO reqVO) {
        validateSubRecordExists(contractMapper.selectById(reqVO.getId()));
        contractMapper.updateById(BeanUtils.toBean(reqVO, HrmEmployeeContractDO.class));
    }

    @Override
    public void deleteContract(Long id) {
        validateSubRecordExists(contractMapper.selectById(id));
        contractMapper.deleteById(id);
    }

    // ========== 异动记录 / 离职信息 ==========

    @Override
    public List<HrmEmployeeChangeRecordRespVO> getChangeRecordList(Long employeeId) {
        validateEmployeeExists(employeeId);
        return BeanUtils.toBean(changeRecordMapper.selectListByEmployeeId(employeeId),
                HrmEmployeeChangeRecordRespVO.class);
    }

    @Override
    public HrmEmployeeQuitInfoRespVO getQuitInfo(Long employeeId) {
        validateEmployeeExists(employeeId);
        HrmEmployeeQuitInfoDO quitInfo = quitInfoMapper.selectByEmployeeId(employeeId);
        return BeanUtils.toBean(quitInfo, HrmEmployeeQuitInfoRespVO.class);
    }

    // ========== 附件 ==========

    @Override
    public List<HrmEmployeeFileRespVO> getFileList(Long employeeId) {
        validateEmployeeExists(employeeId);
        return BeanUtils.toBean(hrmEmployeeFileMapper.selectListByEmployeeId(employeeId), HrmEmployeeFileRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveFiles(HrmEmployeeFileSaveReqVO reqVO) {
        validateEmployeeExists(reqVO.getEmployeeId());
        // (employeeId, type) 整组替换：先软删旧组，再插新组
        hrmEmployeeFileMapper.delete(new LambdaUpdateWrapper<HrmEmployeeFileDO>()
                .eq(HrmEmployeeFileDO::getEmployeeId, reqVO.getEmployeeId())
                .eq(HrmEmployeeFileDO::getType, reqVO.getType()));
        for (String url : reqVO.getFileUrls()) {
            HrmEmployeeFileDO file = HrmEmployeeFileDO.builder()
                    .employeeId(reqVO.getEmployeeId())
                    .type(reqVO.getType())
                    .url(url)
                    .build();
            hrmEmployeeFileMapper.insert(file);
        }
    }

    // ========== 工资卡 ==========

    @Override
    public HrmEmployeeSalaryCardVO getSalaryCard(Long employeeId) {
        HrEmployeeDO employee = validateEmployeeExists(employeeId);
        HrmEmployeeSalaryCardVO vo = new HrmEmployeeSalaryCardVO();
        vo.setId(employee.getId());
        vo.setEmployeeId(employee.getId());
        vo.setBankCardNumber(employee.getSalaryCardNumber());
        vo.setBankAreaId(employee.getSalaryCardAreaId());
        vo.setBankAreaName(employee.getSalaryCardAreaName());
        vo.setBankName(employee.getSalaryCardBankName());
        vo.setBankBranchName(employee.getSalaryCardBankBranchName());
        return vo;
    }

    @Override
    public void saveSalaryCard(HrmEmployeeSalaryCardVO reqVO) {
        validateEmployeeExists(reqVO.getEmployeeId());
        LambdaUpdateWrapper<HrEmployeeDO> update = new LambdaUpdateWrapper<HrEmployeeDO>()
                .eq(HrEmployeeDO::getId, reqVO.getEmployeeId())
                .set(HrEmployeeDO::getSalaryCardNumber, reqVO.getBankCardNumber())
                .set(HrEmployeeDO::getSalaryCardAreaId, reqVO.getBankAreaId())
                .set(HrEmployeeDO::getSalaryCardAreaName, reqVO.getBankAreaName())
                .set(HrEmployeeDO::getSalaryCardBankName, reqVO.getBankName())
                .set(HrEmployeeDO::getSalaryCardBankBranchName, reqVO.getBankBranchName());
        employeeMapper.update(null, update);
    }

    @Override
    public void deleteSalaryCard(Long employeeId) {
        validateEmployeeExists(employeeId);
        LambdaUpdateWrapper<HrEmployeeDO> update = new LambdaUpdateWrapper<HrEmployeeDO>()
                .eq(HrEmployeeDO::getId, employeeId)
                .set(HrEmployeeDO::getSalaryCardNumber, null)
                .set(HrEmployeeDO::getSalaryCardAreaId, null)
                .set(HrEmployeeDO::getSalaryCardAreaName, null)
                .set(HrEmployeeDO::getSalaryCardBankName, null)
                .set(HrEmployeeDO::getSalaryCardBankBranchName, null);
        employeeMapper.update(null, update);
    }

    // ========== 个人备忘 ==========

    @Override
    public Long createPersonalNote(HrmEmployeePersonalNoteSaveReqVO reqVO) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        HrmEmployeePersonalNoteDO note = HrmEmployeePersonalNoteDO.builder()
                .userId(userId)
                .content(reqVO.getContent())
                .reminderTime(reqVO.getReminderTime())
                .build();
        personalNoteMapper.insert(note);
        return note.getId();
    }

    @Override
    public void deletePersonalNote(Long id) {
        HrmEmployeePersonalNoteDO note = personalNoteMapper.selectById(id);
        if (note == null) {
            throw exception(HRM_EMPLOYEE_SUB_RECORD_NOT_EXISTS);
        }
        // 只能删自己的备忘
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (!note.getUserId().equals(userId)) {
            throw exception(HRM_EMPLOYEE_SUB_RECORD_NOT_EXISTS);
        }
        personalNoteMapper.deleteById(id);
    }

    // ========== 字段显示配置 ==========

    @Override
    public List<HrmEmployeeFieldConfigRespVO> getFieldConfigList(Integer kind, Integer entryStatus) {
        List<HrmEmployeeFieldConfigDO> list = HrmEmployeeFieldConfigDO.KIND_CREATE == kind
                ? fieldConfigMapper.selectListByKindAndEntryStatus(kind, entryStatus)
                : fieldConfigMapper.selectListByKind(kind);
        // 懒惰供给：当前租户无配置行时，从种子租户(tenant 1)复制默认清单（V163 种子），
        // 使各租户可独立调整；save 写的是本租户行，不动种子。
        if (list.isEmpty()) {
            list = provisionFieldConfigFromSeed(kind, entryStatus);
        }
        return BeanUtils.toBean(list, HrmEmployeeFieldConfigRespVO.class);
    }

    private List<HrmEmployeeFieldConfigDO> provisionFieldConfigFromSeed(Integer kind, Integer entryStatus) {
        List<HrmEmployeeFieldConfigDO> defaults = TenantUtils.executeIgnore(() ->
                HrmEmployeeFieldConfigDO.KIND_CREATE == kind
                        ? fieldConfigMapper.selectListByKindAndEntryStatus(kind, entryStatus)
                        : fieldConfigMapper.selectListByKind(kind));
        if (defaults.isEmpty()) {
            return Collections.emptyList();
        }
        List<HrmEmployeeFieldConfigDO> copies = new ArrayList<>(defaults.size());
        for (HrmEmployeeFieldConfigDO seed : defaults) {
            HrmEmployeeFieldConfigDO copy = new HrmEmployeeFieldConfigDO();
            copy.setKind(seed.getKind());
            copy.setEntryStatus(seed.getEntryStatus());
            copy.setName(seed.getName());
            copy.setTitle(seed.getTitle());
            copy.setGroupName(seed.getGroupName());
            copy.setVisible(seed.getVisible());
            copy.setEditable(seed.getEditable());
            copy.setVisibleLocked(seed.getVisibleLocked());
            copy.setEditableLocked(seed.getEditableLocked());
            copy.setSort(seed.getSort());
            copies.add(copy);
        }
        // 正常租户上下文插入：tenant_id 自动落当前租户
        fieldConfigMapper.insertBatch(copies);
        return copies;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveCreateFieldConfig(HrmEmployeeCreateFieldConfigSaveReqVO reqVO) {
        saveFieldConfig(HrmEmployeeFieldConfigDO.KIND_CREATE, reqVO.getEntryStatus(), reqVO.getFields(), false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveArchiveFieldConfig(HrmEmployeeArchiveFieldConfigSaveReqVO reqVO) {
        saveFieldConfig(HrmEmployeeFieldConfigDO.KIND_ARCHIVE, null, reqVO.getFields(), true);
    }

    /**
     * 按字段名逐项更新显示开关；locked 字段不允许改（种子口径）。配置行不存在时忽略（不伪造）。
     */
    private void saveFieldConfig(int kind, Integer entryStatus, List<HrmEmployeeFieldVisibleSaveReqVO> fields,
                                 boolean applyEditable) {
        if (CollUtil.isEmpty(fields)) {
            return;
        }
        for (HrmEmployeeFieldVisibleSaveReqVO field : fields) {
            HrmEmployeeFieldConfigDO config = fieldConfigMapper.selectByConfigKey(kind, entryStatus, field.getName());
            if (config == null) {
                continue;
            }
            LambdaUpdateWrapper<HrmEmployeeFieldConfigDO> update = new LambdaUpdateWrapper<HrmEmployeeFieldConfigDO>()
                    .eq(HrmEmployeeFieldConfigDO::getId, config.getId());
            boolean hasChange = false;
            if (!Boolean.TRUE.equals(config.getVisibleLocked())) {
                update.set(HrmEmployeeFieldConfigDO::getVisible, field.getVisible());
                hasChange = true;
            }
            if (applyEditable && !Boolean.TRUE.equals(config.getEditableLocked()) && field.getEditable() != null) {
                update.set(HrmEmployeeFieldConfigDO::getEditable, field.getEditable());
                hasChange = true;
            }
            if (hasChange) {
                fieldConfigMapper.update(null, update);
            }
        }
    }

    private HrEmployeeDO validateEmployeeExists(Long id) {
        HrEmployeeDO employee = employeeMapper.selectById(id);
        if (employee == null) {
            throw exception(HRM_EMPLOYEE_NOT_EXISTS);
        }
        return employee;
    }

    private void validateSubRecordExists(Object record) {
        if (record == null) {
            throw exception(HRM_EMPLOYEE_SUB_RECORD_NOT_EXISTS);
        }
    }

}
