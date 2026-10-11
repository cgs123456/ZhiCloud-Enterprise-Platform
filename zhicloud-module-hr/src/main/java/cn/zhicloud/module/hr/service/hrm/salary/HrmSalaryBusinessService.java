package cn.zhicloud.module.hr.service.hrm.salary;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.hr.controller.admin.hrm.salary.vo.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * HRM 薪资业务域 Service 接口（员工薪资档案/调薪记录/月度工资表/员工月度工资/工资条）
 *
 * 口径（对齐前端 views/hrm/salary）：
 * * 员工薪资档案：按员工唯一（employeeId），记录转正/试用期工资项；调薪写 hr_salary_employee_change_record。
 * * 月度工资表：表头聚合 + 员工月度工资明细；核算后 status=11。
 * * 工资条：发放记录 + 每员工一条工资条；查看状态 readStatus 0未查看 1已查看。
 *
 * @author zhicloud
 */
public interface HrmSalaryBusinessService {

    // ========== 员工薪资档案 ==========

    /**
     * 获得员工薪资档案分页（join 员工档案 enrich）
     */
    PageResult<HrmSalaryEmployeeInfoVO> getEmployeeInfoPage(HrmSalaryEmployeeInfoPageReqVO reqVO);

    /**
     * 获得员工薪资档案详情（按员工编号）
     */
    HrmSalaryEmployeeInfoVO getEmployeeInfo(Long employeeId);

    /**
     * 按入职状态（1在职 2待入职 3待离职 4离职）统计员工数量
     */
    List<HrmSalaryEmployeeStatusCountRespVO> getEmployeeStatusCount(HrmSalaryEmployeeInfoPageReqVO reqVO);

    /**
     * 定薪/调薪：upsert 员工薪资档案，并写一条调薪记录（recordType=1 定薪）
     */
    void updateEmployeeInfo(HrmSalaryEmployeeInfoUpdateReqVO reqVO);

    /**
     * 批量定薪/调薪
     */
    void updateEmployeeInfoList(List<HrmSalaryEmployeeInfoUpdateReqVO> list);

    /**
     * 获得最早未生效的调薪生效时间
     */
    LocalDateTime getAdjustmentMinEffectDate();

    // ========== 调薪记录 ==========

    /**
     * 获得员工调薪记录列表
     */
    List<HrmSalaryEmployeeChangeRecordVO> getChangeRecordList(Long employeeId);

    /**
     * 获得调薪记录详情
     */
    HrmSalaryEmployeeChangeRecordVO getChangeRecord(Long id);

    /**
     * 取消调薪记录（status=2 已取消）
     */
    void cancelChangeRecord(Long id);

    /**
     * 删除调薪记录（软删除）
     */
    void deleteChangeRecord(Long id);

    // ========== 月度工资表 ==========

    /**
     * 获得月度工资表分页
     */
    PageResult<HrmSalaryMonthRecordVO> getMonthRecordPage(HrmSalaryMonthRecordPageReqVO reqVO);

    /**
     * 获得月度工资表详情
     */
    HrmSalaryMonthRecordVO getMonthRecord(Long id);

    /**
     * 获得最近一期月度工资表
     */
    HrmSalaryMonthRecordVO getLastMonthRecord();

    /**
     * 创建下月工资表（status=5 未核算）
     */
    Long createNextMonthRecord();

    /**
     * 删除月度工资表（表头 + 员工月度工资明细）
     */
    void deleteMonthRecord(Long id);

    /**
     * 获得薪资核算就绪状态
     */
    HrmSalaryPayrollReadinessRespVO getPayrollReadiness(Long monthRecordId);

    /**
     * 获得月度工资薪资项汇总（按 code 聚合求和）
     */
    List<SalaryOptionValueVO> getMonthOptionSummary(Long monthRecordId);

    // ========== 员工月度工资 ==========

    /**
     * 获得员工月度工资分页
     */
    PageResult<HrmSalaryMonthEmployeeRecordVO> getMonthEmployeeRecordPage(HrmSalaryMonthEmployeeRecordPageReqVO reqVO);

    /**
     * 获得指定员工的月度工资分页（同 {@link #getMonthEmployeeRecordPage}）
     */
    PageResult<HrmSalaryMonthEmployeeRecordVO> getMonthEmployeeRecordEmployeePage(HrmSalaryMonthEmployeeRecordPageReqVO reqVO);

    /**
     * 获得员工月度工资列表
     */
    List<HrmSalaryMonthEmployeeRecordVO> getMonthEmployeeRecordList(Long monthRecordId);

    /**
     * 批量修改员工月度工资
     */
    void updateMonthEmployeeRecordList(HrmSalaryMonthEmployeeUpdateListReqVO reqVO);

    /**
     * 获得月度员工异动数量（按调薪原因聚合）
     */
    List<HrmSalaryChangeCountRespVO> getMonthEmployeeChangeCount(Long monthRecordId);

    /**
     * 获得绩效系数列表
     */
    List<HrmSalaryPerformanceCoefficientVO> getPerformanceCoefficients(Long monthRecordId);

    /**
     * 核算月度工资表（重算表头聚合，status=11 已核算）
     */
    Long computeMonthRecord(Long monthRecordId);

    /**
     * 导入并核算月度工资表
     */
    void computeImport(Long monthRecordId);

    // ========== 工资条模板 ==========

    List<HrmSalarySlipTemplateVO> getSlipTemplateList();

    HrmSalarySlipTemplateVO getSlipTemplate(Long id);

    Long createSlipTemplate(HrmSalarySlipTemplateVO reqVO);

    void updateSlipTemplate(HrmSalarySlipTemplateVO reqVO);

    void deleteSlipTemplate(Long id);

    // ========== 工资条发放记录 ==========

    PageResult<HrmSalarySlipSendRecordVO> getSlipSendRecordPage(Integer pageNo, Integer pageSize, Long monthRecordId);

    HrmSalarySlipSendRecordVO getSlipSendRecord(Long id);

    /**
     * 发放工资条（生成发放记录 + 每员工一条工资条）
     */
    Long createSlipSendRecord(HrmSalarySlipSendRecordCreateReqVO reqVO);

    /**
     * 删除工资条发放记录（含其工资条）
     */
    void deleteSlipSendRecord(Long id);

    // ========== 工资条 ==========

    PageResult<HrmSalarySlipVO> getSlipPage(Integer pageNo, Integer pageSize, Long employeeId, Long sendRecordId);

    HrmSalarySlipVO getSlip(Long id);

    /**
     * 标记工资条已查看（readStatus=1）
     */
    void remarkSlip(Long id);

}
