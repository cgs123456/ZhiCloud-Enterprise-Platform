package cn.zhicloud.module.hr.service.hrm.insurance;

import cn.zhicloud.module.hr.controller.admin.hrm.insurance.vo.*;

import java.util.List;

/**
 * HRM 社保方案 Service 接口（前端 api/hrm/insurance/scheme 契约）
 *
 * @author zhicloud
 */
public interface HrmInsuranceSchemeService {

    /**
     * 获得社保方案列表（含项目拆分/areaName/useCount 等 enrich）
     */
    List<InsuranceSchemeVO> getSchemeList();

    /**
     * 获得社保方案精简列表（下拉消费）
     */
    List<InsuranceSchemeVO> getSchemeSimpleList();

    /**
     * 获得社保方案详情
     */
    InsuranceSchemeVO getScheme(Long id);

    /**
     * 创建社保方案（项目整组落行；比例模式金额=基数×比例，金额模式取行金额）
     */
    Long createScheme(InsuranceSchemeVO reqVO);

    /**
     * 修改社保方案（项目整组替换；不影响已建档员工——前端确认文案同口径）
     */
    void updateScheme(InsuranceSchemeVO reqVO);

    /**
     * 删除社保方案（被员工参保引用时拒绝）
     */
    void deleteScheme(Long id);

}
