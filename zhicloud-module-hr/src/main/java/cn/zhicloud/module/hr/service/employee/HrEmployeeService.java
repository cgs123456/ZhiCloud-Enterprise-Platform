package cn.zhicloud.module.hr.service.employee;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.hr.controller.admin.employee.vo.HrEmployeePageReqVO;
import cn.zhicloud.module.hr.controller.admin.employee.vo.HrEmployeeResignReqVO;
import cn.zhicloud.module.hr.controller.admin.employee.vo.HrEmployeeSaveReqVO;
import cn.zhicloud.module.hr.controller.admin.employee.vo.HrEmployeeTransferReqVO;
import cn.zhicloud.module.hr.dal.dataobject.employee.HrEmployeeDO;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * HR 员工档案 Service 接口
 *
 * @author zhicloud
 */
public interface HrEmployeeService {

    /**
     * 创建员工
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createEmployee(@Valid HrEmployeeSaveReqVO createReqVO);

    /**
     * 更新员工
     *
     * @param updateReqVO 更新信息
     */
    void updateEmployee(@Valid HrEmployeeSaveReqVO updateReqVO);

    /**
     * 删除员工
     *
     * @param id 编号
     */
    void deleteEmployee(Long id);

    /**
     * 获得员工
     *
     * @param id 编号
     * @return 员工
     */
    HrEmployeeDO getEmployee(Long id);

    /**
     * 获得员工列表
     *
     * @param ids 编号数组
     * @return 员工列表
     */
    List<HrEmployeeDO> getEmployeeList(Collection<Long> ids);

    /**
     * 获得员工分页
     *
     * @param pageReqVO 分页查询
     * @return 员工分页
     */
    PageResult<HrEmployeeDO> getEmployeePage(HrEmployeePageReqVO pageReqVO);

    /**
     * 员工离职：自动设置离职日期 + 状态为离职
     *
     * @param reqVO 离职信息
     */
    void resignEmployee(@Valid HrEmployeeResignReqVO reqVO);

    /**
     * 员工调动：变更部门 + 职位
     *
     * @param reqVO 调动信息
     */
    void transferEmployee(@Valid HrEmployeeTransferReqVO reqVO);

    /**
     * 获得指定日期范围入职的员工列表（薪资核算使用）
     *
     * @param deptIds 部门编号数组
     * @return 员工列表
     */
    List<HrEmployeeDO> getEmployeeListByDeptIds(Collection<Long> deptIds);

}