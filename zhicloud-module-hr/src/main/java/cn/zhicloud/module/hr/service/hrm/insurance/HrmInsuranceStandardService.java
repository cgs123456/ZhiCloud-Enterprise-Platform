package cn.zhicloud.module.hr.service.hrm.insurance;

import cn.zhicloud.module.hr.controller.admin.hrm.insurance.vo.*;

import java.util.List;

/**
 * HRM 保险域标准参保数据 Service 接口（参保地区/类型/项目）
 *
 * @author zhicloud
 */
public interface HrmInsuranceStandardService {

    /**
     * 查询标准参保类型列表（按地区 areaId）
     */
    List<InsuranceStandardTypeVO> getStandardTypeList(Long areaId);

    /**
     * 查询标准参保项目列表（按地区 areaId 和参保方案编码 typeCode）
     */
    List<InsuranceStandardProjectVO> getStandardProjectList(Long areaId, String typeCode);

}
