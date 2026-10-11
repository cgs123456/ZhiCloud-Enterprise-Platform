package cn.zhicloud.module.hr.service.hrm.insurance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.hr.controller.admin.hrm.insurance.vo.*;

import java.util.List;

/**
 * HRM 保险域月度社保 Service 接口（月度社保表 + 员工月度社保记录）
 *
 * @author zhicloud
 */
public interface HrmInsuranceMonthService {

    // ========== 月度社保表 ==========

    List<InsuranceMonthRecordVO> getMonthRecordList(Integer year);

    InsuranceMonthRecordVO getMonthRecord(Long id);

    InsuranceMonthRecordVO getLastMonthRecord();

    /**
     * 创建首月社保表（指定年月）
     */
    Long createFirstMonthRecord(InsuranceMonthRecordCreateReqVO reqVO);

    /**
     * 新建次月社保表（在最近一个月基础上顺延）
     */
    Long createNextMonthRecord();

    void deleteMonthRecord(Long id);

    // ========== 员工月度社保记录 ==========

    PageResult<InsuranceMonthEmployeeRecordVO> getMonthEmployeeRecordPage(Integer pageNo, Integer pageSize, Long monthRecordId);

    InsuranceMonthEmployeeRecordVO getMonthEmployeeRecord(Long id);

    /**
     * 添加参保人员（按员工薪资方案生成月度记录；幂等跳过已存在）
     */
    Long createMonthEmployeeRecordList(InsuranceMonthEmployeeCreateListReqVO reqVO);

    /**
     * 修改员工月度参保项目
     */
    void updateMonthEmployeeRecord(InsuranceMonthEmployeeUpdateReqVO reqVO);

    /**
     * 批量停止参保（status → 0）
     */
    void stopMonthEmployeeRecordList(InsuranceMonthEmployeeStopListReqVO reqVO);

    /**
     * 查询本月未参保员工
     */
    List<Long> getUninsuredEmployeeIdList(Long monthRecordId);

}
