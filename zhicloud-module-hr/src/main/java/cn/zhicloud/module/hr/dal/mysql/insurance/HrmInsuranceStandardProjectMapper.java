package cn.zhicloud.module.hr.dal.mysql.insurance;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.insurance.HrmInsuranceStandardProjectDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 标准参保项目 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmInsuranceStandardProjectMapper extends BaseMapperX<HrmInsuranceStandardProjectDO> {

    default List<HrmInsuranceStandardProjectDO> selectListByTypeId(Long typeId) {
        return selectList(new LambdaQueryWrapperX<HrmInsuranceStandardProjectDO>()
                .eq(HrmInsuranceStandardProjectDO::getTypeId, typeId)
                .orderByAsc(HrmInsuranceStandardProjectDO::getId));
    }

}
