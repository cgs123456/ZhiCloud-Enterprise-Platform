package cn.zhicloud.module.hr.dal.mysql.insurance;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.insurance.HrmInsuranceSchemeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 社保方案 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmInsuranceSchemeMapper extends BaseMapperX<HrmInsuranceSchemeDO> {

    default List<HrmInsuranceSchemeDO> selectListAll() {
        return selectList(new LambdaQueryWrapperX<HrmInsuranceSchemeDO>()
                .orderByDesc(HrmInsuranceSchemeDO::getId));
    }

    default HrmInsuranceSchemeDO selectByName(String name) {
        return selectOne(HrmInsuranceSchemeDO::getName, name);
    }

    default List<HrmInsuranceSchemeDO> selectListByIds(java.util.Collection<Long> ids) {
        return selectList(HrmInsuranceSchemeDO::getId, ids);
    }

}
