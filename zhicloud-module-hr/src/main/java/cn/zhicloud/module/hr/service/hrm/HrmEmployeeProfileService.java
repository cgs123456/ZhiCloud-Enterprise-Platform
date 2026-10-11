package cn.zhicloud.module.hr.service.hrm;

import cn.zhicloud.module.hr.controller.admin.hrm.vo.*;

import java.util.List;

/**
 * HRM 员工档案子域 Service 接口（证书/联系人/经历/合同/异动/离职/附件/工资卡/字段配置/备忘）
 *
 * @author zhicloud
 */
public interface HrmEmployeeProfileService {

    // ========== 证书 ==========
    List<HrmEmployeeCertificateVO> getCertificateList(Long employeeId);
    Long createCertificate(HrmEmployeeCertificateVO reqVO);
    void updateCertificate(HrmEmployeeCertificateVO reqVO);
    void deleteCertificate(Long id);

    // ========== 紧急联系人 ==========
    List<HrmEmployeeContactVO> getContactList(Long employeeId);
    Long createContact(HrmEmployeeContactVO reqVO);
    void updateContact(HrmEmployeeContactVO reqVO);
    void deleteContact(Long id);

    // ========== 教育经历 ==========
    List<HrmEmployeeEducationExperienceVO> getEducationExperienceList(Long employeeId);
    Long createEducationExperience(HrmEmployeeEducationExperienceVO reqVO);
    void updateEducationExperience(HrmEmployeeEducationExperienceVO reqVO);
    void deleteEducationExperience(Long id);

    // ========== 工作经历 ==========
    List<HrmEmployeeWorkExperienceVO> getWorkExperienceList(Long employeeId);
    Long createWorkExperience(HrmEmployeeWorkExperienceVO reqVO);
    void updateWorkExperience(HrmEmployeeWorkExperienceVO reqVO);
    void deleteWorkExperience(Long id);

    // ========== 培训经历 ==========
    List<HrmEmployeeTrainingExperienceVO> getTrainingExperienceList(Long employeeId);
    Long createTrainingExperience(HrmEmployeeTrainingExperienceVO reqVO);
    void updateTrainingExperience(HrmEmployeeTrainingExperienceVO reqVO);
    void deleteTrainingExperience(Long id);

    // ========== 合同 ==========
    List<HrmEmployeeContractVO> getContractList(Long employeeId);
    Long createContract(HrmEmployeeContractVO reqVO);
    void updateContract(HrmEmployeeContractVO reqVO);
    void deleteContract(Long id);

    // ========== 异动记录 / 离职信息 ==========
    List<HrmEmployeeChangeRecordRespVO> getChangeRecordList(Long employeeId);
    HrmEmployeeQuitInfoRespVO getQuitInfo(Long employeeId);

    // ========== 附件 ==========
    List<HrmEmployeeFileRespVO> getFileList(Long employeeId);
    void saveFiles(HrmEmployeeFileSaveReqVO reqVO);

    // ========== 工资卡（hr_employee 单卡五列） ==========
    HrmEmployeeSalaryCardVO getSalaryCard(Long employeeId);
    void saveSalaryCard(HrmEmployeeSalaryCardVO reqVO);
    void deleteSalaryCard(Long employeeId);

    // ========== 个人备忘（按登录用户隔离） ==========
    Long createPersonalNote(HrmEmployeePersonalNoteSaveReqVO reqVO);
    void deletePersonalNote(Long id);

    // ========== 字段显示配置 ==========
    List<HrmEmployeeFieldConfigRespVO> getFieldConfigList(Integer kind, Integer entryStatus);
    void saveCreateFieldConfig(HrmEmployeeCreateFieldConfigSaveReqVO reqVO);
    void saveArchiveFieldConfig(HrmEmployeeArchiveFieldConfigSaveReqVO reqVO);

}
