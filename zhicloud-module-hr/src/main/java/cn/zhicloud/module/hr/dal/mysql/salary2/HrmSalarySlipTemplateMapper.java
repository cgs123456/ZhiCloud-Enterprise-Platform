package cn.zhicloud.module.hr.dal.mysql.salary2;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.salary2.HrmSalarySlipTemplateDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 工资条模板 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmSalarySlipTemplateMapper extends BaseMapperX<HrmSalarySlipTemplateDO> {

    default List<HrmSalarySlipTemplateDO> selectListAll() {
        return selectList(new LambdaQueryWrapperX<HrmSalarySlipTemplateDO>()
                .orderByDesc(HrmSalarySlipTemplateDO::getDefaultStatus)
                .orderByDesc(HrmSalarySlipTemplateDO::getId));
    }
}
