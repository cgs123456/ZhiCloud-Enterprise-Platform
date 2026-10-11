package cn.zhicloud.module.hr.controller.admin.hrm;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.module.hr.controller.admin.hrm.vo.*;
import cn.zhicloud.module.hr.dal.dataobject.hrm.HrmEmployeeFieldConfigDO;
import cn.zhicloud.module.hr.service.hrm.HrmEmployeeProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * HRM 员工档案子域 Controller（前端 views/hrm/employee/detail、config 与 home 日历契约）
 *
 * @author zhicloud
 */
@Tag(name = "管理后台 - HRM 员工档案子域")
@RestController
@RequestMapping("/hrm/employee")
@Validated
public class HrmEmployeeProfileController {

    @Resource
    private HrmEmployeeProfileService profileService;

    // ========== 证书 ==========

    @GetMapping("/certificate/list")
    @Operation(summary = "获得员工证书列表")
    @Parameter(name = "employeeId", description = "员工编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:employee:query')")
    public CommonResult<List<HrmEmployeeCertificateVO>> getCertificateList(@RequestParam("employeeId") Long employeeId) {
        return success(profileService.getCertificateList(employeeId));
    }

    @PostMapping("/certificate/create")
    @Operation(summary = "创建员工证书")
    @PreAuthorize("@ss.hasPermission('hrm:employee:update')")
    public CommonResult<Long> createCertificate(@Valid @RequestBody HrmEmployeeCertificateVO reqVO) {
        return success(profileService.createCertificate(reqVO));
    }

    @PutMapping("/certificate/update")
    @Operation(summary = "修改员工证书")
    @PreAuthorize("@ss.hasPermission('hrm:employee:update')")
    public CommonResult<Boolean> updateCertificate(@Valid @RequestBody HrmEmployeeCertificateVO reqVO) {
        profileService.updateCertificate(reqVO);
        return success(true);
    }

    @DeleteMapping("/certificate/delete")
    @Operation(summary = "删除员工证书")
    @Parameter(name = "id", description = "证书编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:employee:delete')")
    public CommonResult<Boolean> deleteCertificate(@RequestParam("id") Long id) {
        profileService.deleteCertificate(id);
        return success(true);
    }

    // ========== 紧急联系人 ==========

    @GetMapping("/contact/list")
    @Operation(summary = "获得员工紧急联系人列表")
    @Parameter(name = "employeeId", description = "员工编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:employee:query')")
    public CommonResult<List<HrmEmployeeContactVO>> getContactList(@RequestParam("employeeId") Long employeeId) {
        return success(profileService.getContactList(employeeId));
    }

    @PostMapping("/contact/create")
    @Operation(summary = "创建员工紧急联系人")
    @PreAuthorize("@ss.hasPermission('hrm:employee:update')")
    public CommonResult<Long> createContact(@Valid @RequestBody HrmEmployeeContactVO reqVO) {
        return success(profileService.createContact(reqVO));
    }

    @PutMapping("/contact/update")
    @Operation(summary = "修改员工紧急联系人")
    @PreAuthorize("@ss.hasPermission('hrm:employee:update')")
    public CommonResult<Boolean> updateContact(@Valid @RequestBody HrmEmployeeContactVO reqVO) {
        profileService.updateContact(reqVO);
        return success(true);
    }

    @DeleteMapping("/contact/delete")
    @Operation(summary = "删除员工紧急联系人")
    @Parameter(name = "id", description = "联系人编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:employee:delete')")
    public CommonResult<Boolean> deleteContact(@RequestParam("id") Long id) {
        profileService.deleteContact(id);
        return success(true);
    }

    // ========== 教育经历 ==========

    @GetMapping("/education-experience/list")
    @Operation(summary = "获得员工教育经历列表")
    @Parameter(name = "employeeId", description = "员工编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:employee:query')")
    public CommonResult<List<HrmEmployeeEducationExperienceVO>> getEducationExperienceList(
            @RequestParam("employeeId") Long employeeId) {
        return success(profileService.getEducationExperienceList(employeeId));
    }

    @PostMapping("/education-experience/create")
    @Operation(summary = "创建员工教育经历")
    @PreAuthorize("@ss.hasPermission('hrm:employee:update')")
    public CommonResult<Long> createEducationExperience(@Valid @RequestBody HrmEmployeeEducationExperienceVO reqVO) {
        return success(profileService.createEducationExperience(reqVO));
    }

    @PutMapping("/education-experience/update")
    @Operation(summary = "修改员工教育经历")
    @PreAuthorize("@ss.hasPermission('hrm:employee:update')")
    public CommonResult<Boolean> updateEducationExperience(@Valid @RequestBody HrmEmployeeEducationExperienceVO reqVO) {
        profileService.updateEducationExperience(reqVO);
        return success(true);
    }

    @DeleteMapping("/education-experience/delete")
    @Operation(summary = "删除员工教育经历")
    @Parameter(name = "id", description = "教育经历编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:employee:delete')")
    public CommonResult<Boolean> deleteEducationExperience(@RequestParam("id") Long id) {
        profileService.deleteEducationExperience(id);
        return success(true);
    }

    // ========== 工作经历 ==========

    @GetMapping("/work-experience/list")
    @Operation(summary = "获得员工工作经历列表")
    @Parameter(name = "employeeId", description = "员工编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:employee:query')")
    public CommonResult<List<HrmEmployeeWorkExperienceVO>> getWorkExperienceList(
            @RequestParam("employeeId") Long employeeId) {
        return success(profileService.getWorkExperienceList(employeeId));
    }

    @PostMapping("/work-experience/create")
    @Operation(summary = "创建员工工作经历")
    @PreAuthorize("@ss.hasPermission('hrm:employee:update')")
    public CommonResult<Long> createWorkExperience(@Valid @RequestBody HrmEmployeeWorkExperienceVO reqVO) {
        return success(profileService.createWorkExperience(reqVO));
    }

    @PutMapping("/work-experience/update")
    @Operation(summary = "修改员工工作经历")
    @PreAuthorize("@ss.hasPermission('hrm:employee:update')")
    public CommonResult<Boolean> updateWorkExperience(@Valid @RequestBody HrmEmployeeWorkExperienceVO reqVO) {
        profileService.updateWorkExperience(reqVO);
        return success(true);
    }

    @DeleteMapping("/work-experience/delete")
    @Operation(summary = "删除员工工作经历")
    @Parameter(name = "id", description = "工作经历编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:employee:delete')")
    public CommonResult<Boolean> deleteWorkExperience(@RequestParam("id") Long id) {
        profileService.deleteWorkExperience(id);
        return success(true);
    }

    // ========== 培训经历 ==========

    @GetMapping("/training-experience/list")
    @Operation(summary = "获得员工培训经历列表")
    @Parameter(name = "employeeId", description = "员工编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:employee:query')")
    public CommonResult<List<HrmEmployeeTrainingExperienceVO>> getTrainingExperienceList(
            @RequestParam("employeeId") Long employeeId) {
        return success(profileService.getTrainingExperienceList(employeeId));
    }

    @PostMapping("/training-experience/create")
    @Operation(summary = "创建员工培训经历")
    @PreAuthorize("@ss.hasPermission('hrm:employee:update')")
    public CommonResult<Long> createTrainingExperience(@Valid @RequestBody HrmEmployeeTrainingExperienceVO reqVO) {
        return success(profileService.createTrainingExperience(reqVO));
    }

    @PutMapping("/training-experience/update")
    @Operation(summary = "修改员工培训经历")
    @PreAuthorize("@ss.hasPermission('hrm:employee:update')")
    public CommonResult<Boolean> updateTrainingExperience(@Valid @RequestBody HrmEmployeeTrainingExperienceVO reqVO) {
        profileService.updateTrainingExperience(reqVO);
        return success(true);
    }

    @DeleteMapping("/training-experience/delete")
    @Operation(summary = "删除员工培训经历")
    @Parameter(name = "id", description = "培训经历编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:employee:delete')")
    public CommonResult<Boolean> deleteTrainingExperience(@RequestParam("id") Long id) {
        profileService.deleteTrainingExperience(id);
        return success(true);
    }

    // ========== 合同 ==========

    @GetMapping("/contract/list")
    @Operation(summary = "获得员工合同列表")
    @Parameter(name = "employeeId", description = "员工编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:employee:query')")
    public CommonResult<List<HrmEmployeeContractVO>> getContractList(@RequestParam("employeeId") Long employeeId) {
        return success(profileService.getContractList(employeeId));
    }

    @PostMapping("/contract/create")
    @Operation(summary = "创建员工合同")
    @PreAuthorize("@ss.hasPermission('hrm:employee:update')")
    public CommonResult<Long> createContract(@Valid @RequestBody HrmEmployeeContractVO reqVO) {
        return success(profileService.createContract(reqVO));
    }

    @PutMapping("/contract/update")
    @Operation(summary = "修改员工合同")
    @PreAuthorize("@ss.hasPermission('hrm:employee:update')")
    public CommonResult<Boolean> updateContract(@Valid @RequestBody HrmEmployeeContractVO reqVO) {
        profileService.updateContract(reqVO);
        return success(true);
    }

    @DeleteMapping("/contract/delete")
    @Operation(summary = "删除员工合同")
    @Parameter(name = "id", description = "合同编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:employee:delete')")
    public CommonResult<Boolean> deleteContract(@RequestParam("id") Long id) {
        profileService.deleteContract(id);
        return success(true);
    }

    // ========== 异动记录 / 离职信息 ==========

    @GetMapping("/change-record/list")
    @Operation(summary = "获得员工异动记录列表")
    @Parameter(name = "employeeId", description = "员工编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:employee:query')")
    public CommonResult<List<HrmEmployeeChangeRecordRespVO>> getChangeRecordList(
            @RequestParam("employeeId") Long employeeId) {
        return success(profileService.getChangeRecordList(employeeId));
    }

    @GetMapping("/quit-info/get")
    @Operation(summary = "获得员工离职信息")
    @Parameter(name = "employeeId", description = "员工编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:employee:query')")
    public CommonResult<HrmEmployeeQuitInfoRespVO> getQuitInfo(@RequestParam("employeeId") Long employeeId) {
        return success(profileService.getQuitInfo(employeeId));
    }

    // ========== 附件 ==========

    @GetMapping("/file/list")
    @Operation(summary = "获得员工附件列表")
    @Parameter(name = "employeeId", description = "员工编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:employee:query')")
    public CommonResult<List<HrmEmployeeFileRespVO>> getFileList(@RequestParam("employeeId") Long employeeId) {
        return success(profileService.getFileList(employeeId));
    }

    @PutMapping("/file/save")
    @Operation(summary = "保存员工附件（按类型整组替换）")
    @PreAuthorize("@ss.hasPermission('hrm:employee:update')")
    public CommonResult<Boolean> saveFiles(@Valid @RequestBody HrmEmployeeFileSaveReqVO reqVO) {
        profileService.saveFiles(reqVO);
        return success(true);
    }

    // ========== 工资卡 ==========

    @GetMapping("/salary-card/get")
    @Operation(summary = "获得员工工资卡")
    @Parameter(name = "employeeId", description = "员工编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:employee:query')")
    public CommonResult<HrmEmployeeSalaryCardVO> getSalaryCard(@RequestParam("employeeId") Long employeeId) {
        return success(profileService.getSalaryCard(employeeId));
    }

    @PutMapping("/salary-card/save")
    @Operation(summary = "保存员工工资卡")
    @PreAuthorize("@ss.hasPermission('hrm:employee:update')")
    public CommonResult<Boolean> saveSalaryCard(@Valid @RequestBody HrmEmployeeSalaryCardVO reqVO) {
        profileService.saveSalaryCard(reqVO);
        return success(true);
    }

    @DeleteMapping("/salary-card/delete")
    @Operation(summary = "删除员工工资卡")
    @Parameter(name = "employeeId", description = "员工编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:employee:update')")
    public CommonResult<Boolean> deleteSalaryCard(@RequestParam("employeeId") Long employeeId) {
        profileService.deleteSalaryCard(employeeId);
        return success(true);
    }

    // ========== 个人备忘 ==========

    @PostMapping("/personal-note/create")
    @Operation(summary = "创建个人备忘")
    @PreAuthorize("@ss.hasPermission('hrm:employee:personal-note:create')")
    public CommonResult<Long> createPersonalNote(@Valid @RequestBody HrmEmployeePersonalNoteSaveReqVO reqVO) {
        return success(profileService.createPersonalNote(reqVO));
    }

    @DeleteMapping("/personal-note/delete")
    @Operation(summary = "删除个人备忘")
    @Parameter(name = "id", description = "备忘编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:employee:personal-note:delete')")
    public CommonResult<Boolean> deletePersonalNote(@RequestParam("id") Long id) {
        profileService.deletePersonalNote(id);
        return success(true);
    }

    // ========== 字段显示配置 ==========

    @GetMapping("/config/create-field/list")
    @Operation(summary = "获得新建员工字段配置")
    @Parameter(name = "entryStatus", description = "入职状态：1在职 2待入职", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:employee:query')")
    public CommonResult<List<HrmEmployeeFieldConfigRespVO>> getCreateFieldConfigList(
            @RequestParam("entryStatus") Integer entryStatus) {
        return success(profileService.getFieldConfigList(
                HrmEmployeeFieldConfigDO.KIND_CREATE, entryStatus));
    }

    @PutMapping("/config/create-field/save")
    @Operation(summary = "保存新建员工字段配置")
    @PreAuthorize("@ss.hasPermission('hrm:employee:config:update')")
    public CommonResult<Boolean> saveCreateFieldConfig(@Valid @RequestBody HrmEmployeeCreateFieldConfigSaveReqVO reqVO) {
        profileService.saveCreateFieldConfig(reqVO);
        return success(true);
    }

    @GetMapping("/config/archive-field/list")
    @Operation(summary = "获得档案字段配置")
    @PreAuthorize("@ss.hasPermission('hrm:employee:query')")
    public CommonResult<List<HrmEmployeeFieldConfigRespVO>> getArchiveFieldConfigList() {
        return success(profileService.getFieldConfigList(HrmEmployeeFieldConfigDO.KIND_ARCHIVE, null));
    }

    @PutMapping("/config/archive-field/save")
    @Operation(summary = "保存档案字段配置")
    @PreAuthorize("@ss.hasPermission('hrm:employee:config:update')")
    public CommonResult<Boolean> saveArchiveFieldConfig(@Valid @RequestBody HrmEmployeeArchiveFieldConfigSaveReqVO reqVO) {
        profileService.saveArchiveFieldConfig(reqVO);
        return success(true);
    }

}
