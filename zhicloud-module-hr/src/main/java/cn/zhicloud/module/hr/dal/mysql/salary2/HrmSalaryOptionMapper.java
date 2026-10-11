package cn.zhicloud.module.hr.dal.mysql.salary2;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.salary2.HrmSalaryOptionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 薪资项 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmSalaryOptionMapper extends BaseMapperX<HrmSalaryOptionDO> {

    default List<HrmSalaryOptionDO> selectListAll() {
        return selectList(new LambdaQueryWrapperX<HrmSalaryOptionDO>()
                .orderByAsc(HrmSalaryOptionDO::getSort)
                .orderByAsc(HrmSalaryOptionDO::getCode));
    }

    default HrmSalaryOptionDO selectByCode(Integer code) {
        return selectOne(HrmSalaryOptionDO::getCode, code);
    }

}
