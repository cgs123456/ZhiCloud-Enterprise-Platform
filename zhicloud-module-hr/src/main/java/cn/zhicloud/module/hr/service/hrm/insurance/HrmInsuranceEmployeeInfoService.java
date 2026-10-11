package cn.zhicloud.module.hr.service.hrm.insurance;

import cn.zhicloud.module.hr.controller.admin.hrm.insurance.vo.HrmInsuranceEmployeeInfoVO;
import cn.zhicloud.module.hr.controller.admin.hrm.insurance.vo.HrmInsuranceEmployeeUpdateSchemeReqVO;

/**
 * HRM 员工参保信息 Service 接口（前端 api/hrm/insurance/employee-info 契约）
 *
 * @author zhicloud
 */
public interface HrmInsuranceEmployeeInfoService {

    /**
     * 获得员工参保信息（未建档返回 null；socialSecurityNumber/accumulationFundNumber/schemeName enrich）
     */
    HrmInsuranceEmployeeInfoVO getEmployeeInfo(Long employeeId);

    /**
     * 保存员工参保信息（upsert；社保/公积金账号双写镜像 hr_employee 两列，单一真相）
     */
    Long saveEmployeeInfo(HrmInsuranceEmployeeInfoVO reqVO);

    /**
     * 更新员工参保方案（员工列表「设置参保方案」入口）
     */
    void updateEmployeeScheme(HrmInsuranceEmployeeUpdateSchemeReqVO reqVO);

}
