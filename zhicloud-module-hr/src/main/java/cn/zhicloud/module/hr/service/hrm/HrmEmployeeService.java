package cn.zhicloud.module.hr.service.hrm;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.hr.controller.admin.hrm.vo.*;

import java.util.Collection;
import java.util.List;

/**
 * HRM 员工档案 Service 接口（/hrm/employee 融合链路）
 *
 * @author zhicloud
 */
public interface HrmEmployeeService {

    /**
     * 获得员工档案分页（含部门名称/直属上级姓名等 enrich）
     */
    PageResult<HrmEmployeeRespVO> getEmployeePage(HrmEmployeePageReqVO pageReqVO);

    /**
     * 获得员工档案列表（enrich）
     */
    List<HrmEmployeeRespVO> getEmployeeList(Collection<Long> ids);

    /**
     * 获得员工精简列表（enrich，不拼子表）
     */
    List<HrmEmployeeRespVO> getEmployeeSimpleList(Collection<Long> ids);

    /**
     * 获得员工档案详情（enrich）
     */
    HrmEmployeeRespVO getEmployee(Long id);

    /**
     * 创建员工档案（校验在职必填工号 + 工号唯一；同步旧列）
     */
    Long createEmployee(HrmEmployeeSaveReqVO createReqVO);

    /**
     * 修改员工档案（离职不可编辑；同步旧列）
     */
    void updateEmployee(HrmEmployeeSaveReqVO updateReqVO);

    /**
     * 删除员工档案（仅离职员工可删）
     */
    void deleteEmployee(Long id);

    /**
     * 批量删除员工档案（仅离职员工可删，存在不可删项时报错）
     */
    void deleteEmployeeList(Collection<Long> ids);

    /**
     * 确认入职：待入职 → 在职
     */
    void confirmEntry(HrmEmployeeSaveReqVO reqVO);

    /**
     * 再入职：离职 → 在职（异动类型 9）
     */
    void rehireEmployee(HrmEmployeeRehireReqVO reqVO);

    /**
     * 转正：试用 → 正式（异动类型 4）
     */
    void regularEmployee(HrmEmployeeChangeReqVO reqVO);

    /**
     * 调岗（异动类型 5）
     */
    void transferEmployee(HrmEmployeeChangeReqVO reqVO);

    /**
     * 晋升（异动类型 6）
     */
    void promoteEmployee(HrmEmployeeChangeReqVO reqVO);

    /**
     * 降级（异动类型 7）
     */
    void demoteEmployee(HrmEmployeeChangeReqVO reqVO);

    /**
     * 转为全职（异动类型 8）
     */
    void convertToFullTimeEmployee(HrmEmployeeChangeReqVO reqVO);

    /**
     * 办理离职：entry_status → 4，写离职信息表（同步旧列）
     */
    void quitEmployee(HrmEmployeeQuitReqVO reqVO);

    /**
     * 取消离职：离职 → 在职，清空离职时间
     */
    void cancelQuitEmployee(HrmEmployeeCancelQuitReqVO reqVO);

    /**
     * 状态页签计数（11在职 12全职 13待入职 14待离职 15已离职）
     */
    List<HrmEmployeeStatusCountRespVO> getEmployeeStatusCount(HrmEmployeePageReqVO reqVO);

    /**
     * 部门维度员工统计（在职/全职/非全职）
     */
    List<HrmEmployeeDeptStatisticsRespVO> getEmployeeDeptStatistics();

    /**
     * 已建档后台用户编号列表
     */
    List<Long> getBoundUserIdList();

    /**
     * 从后台用户批量创建员工档案
     */
    List<Long> createEmployeeListFromUser(List<HrmEmployeeCreateFromUserReqVO> list);

    /**
     * 发送填写档案通知（站内信，模板 hrm_employee_profile_fill）
     */
    HrmEmployeeNotifyRespVO sendProfileFillMessage(Collection<Long> ids);

    /**
     * 导入员工档案（duplicateStrategy：1跳过 2覆盖 3判失败）
     */
    HrmEmployeeImportRespVO importEmployeeList(List<HrmEmployeeImportExcelVO> list, Integer duplicateStrategy);

}
