package cn.zhicloud.module.hr.service.hrm.insurance;

import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.tenant.core.util.TenantUtils;
import cn.zhicloud.module.hr.controller.admin.hrm.insurance.vo.*;
import cn.zhicloud.module.hr.dal.dataobject.insurance.HrmInsuranceStandardTypeDO;
import cn.zhicloud.module.hr.dal.dataobject.insurance.HrmInsuranceStandardProjectDO;
import cn.zhicloud.module.hr.dal.mysql.insurance.HrmInsuranceStandardProjectMapper;
import cn.zhicloud.module.hr.dal.mysql.insurance.HrmInsuranceStandardTypeMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.List;

import static cn.zhicloud.framework.common.util.collection.CollectionUtils.convertList;

/**
 * HRM 保险域标准参保数据 Service 实现类
 *
 * 标准参保类型/项目是平台级参考数据（种子在 tenant 1，见 V169），读取走 executeIgnore；
 * uk 不含 tenant（V168），与 hrm 字段配置的懒惰供给不同，本表各租户只读不维护。
 *
 * @author zhicloud
 */
@Service
@Validated
public class HrmInsuranceStandardServiceImpl implements HrmInsuranceStandardService {

    @Resource
    private HrmInsuranceStandardTypeMapper typeMapper;

    @Resource
    private HrmInsuranceStandardProjectMapper projectMapper;

    @Override
    public List<InsuranceStandardTypeVO> getStandardTypeList(Long areaId) {
        if (areaId == null) {
            return Collections.emptyList();
        }
        List<HrmInsuranceStandardTypeDO> types = TenantUtils.executeIgnore(
                () -> typeMapper.selectListByAreaId(areaId));
        return BeanUtils.toBean(types, InsuranceStandardTypeVO.class);
    }

    @Override
    public List<InsuranceStandardProjectVO> getStandardProjectList(Long areaId, String typeCode) {
        if (areaId == null || typeCode == null || typeCode.isEmpty()) {
            return Collections.emptyList();
        }
        // 先定位标准类型（拿 id），再取其项目
        List<HrmInsuranceStandardProjectDO> projects = TenantUtils.executeIgnore(() -> {
            HrmInsuranceStandardTypeDO type = typeMapper.selectByAreaIdAndCode(areaId, typeCode);
            if (type == null) {
                return Collections.<HrmInsuranceStandardProjectDO>emptyList();
            }
            return projectMapper.selectListByTypeId(type.getId());
        });
        return convertList(projects, project -> {
            InsuranceStandardProjectVO vo = BeanUtils.toBean(project, InsuranceStandardProjectVO.class);
            // 金额现算：基数 × 比例（前端可再改）
            if (project.getBaseAmount() != null && project.getCorporateRate() != null) {
                vo.setCorporateAmount(project.getBaseAmount().multiply(project.getCorporateRate()));
            }
            if (project.getBaseAmount() != null && project.getPersonalRate() != null) {
                vo.setPersonalAmount(project.getBaseAmount().multiply(project.getPersonalRate()));
            }
            return vo;
        });
    }

}
