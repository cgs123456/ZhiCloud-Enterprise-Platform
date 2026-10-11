package cn.zhicloud.module.hr.dal.mysql.insurance;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.insurance.HrmInsuranceSchemeProjectDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * HRM 社保方案项目 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmInsuranceSchemeProjectMapper extends BaseMapperX<HrmInsuranceSchemeProjectDO> {

    default List<HrmInsuranceSchemeProjectDO> selectListBySchemeId(Long schemeId) {
        return selectList(new LambdaQueryWrapperX<HrmInsuranceSchemeProjectDO>()
                .eq(HrmInsuranceSchemeProjectDO::getSchemeId, schemeId)
                .orderByAsc(HrmInsuranceSchemeProjectDO::getId));
    }

    default List<HrmInsuranceSchemeProjectDO> selectListBySchemeIds(Collection<Long> schemeIds) {
        return selectList(new LambdaQueryWrapperX<HrmInsuranceSchemeProjectDO>()
                .in(HrmInsuranceSchemeProjectDO::getSchemeId, schemeIds)
                .orderByAsc(HrmInsuranceSchemeProjectDO::getId));
    }

    default void deleteBySchemeId(Long schemeId) {
        delete(new LambdaQueryWrapperX<HrmInsuranceSchemeProjectDO>()
                .eq(HrmInsuranceSchemeProjectDO::getSchemeId, schemeId));
    }

}
