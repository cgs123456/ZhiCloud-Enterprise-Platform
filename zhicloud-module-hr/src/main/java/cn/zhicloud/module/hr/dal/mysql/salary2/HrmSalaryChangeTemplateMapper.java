package cn.zhicloud.module.hr.dal.mysql.salary2;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.salary2.HrmSalaryChangeTemplateDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 调薪模板 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmSalaryChangeTemplateMapper extends BaseMapperX<HrmSalaryChangeTemplateDO> {

    default List<HrmSalaryChangeTemplateDO> selectListAll() {
        return selectList(new LambdaQueryWrapperX<HrmSalaryChangeTemplateDO>()
                .orderByDesc(HrmSalaryChangeTemplateDO::getDefaultStatus)
                .orderByDesc(HrmSalaryChangeTemplateDO::getId));
    }

    default HrmSalaryChangeTemplateDO selectByName(String name) {
        return selectOne(HrmSalaryChangeTemplateDO::getName, name);
    }

}
