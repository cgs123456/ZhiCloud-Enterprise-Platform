package cn.zhicloud.module.hr.dal.mysql.insurance;

import cn.zhicloud.framework.common.pojo.PageParam;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.insurance.HrmInsuranceStandardTypeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 标准参保类型 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmInsuranceStandardTypeMapper extends BaseMapperX<HrmInsuranceStandardTypeDO> {

    default PageResult<HrmInsuranceStandardTypeDO> selectPage(PageParam pageParam, Long areaId) {
        return selectPage(pageParam, new LambdaQueryWrapperX<HrmInsuranceStandardTypeDO>()
                .eq(HrmInsuranceStandardTypeDO::getAreaId, areaId)
                .orderByAsc(HrmInsuranceStandardTypeDO::getId));
    }

    default HrmInsuranceStandardTypeDO selectByAreaIdAndCode(Long areaId, String code) {
        return selectOne(new LambdaQueryWrapperX<HrmInsuranceStandardTypeDO>()
                .eq(HrmInsuranceStandardTypeDO::getAreaId, areaId)
                .eq(HrmInsuranceStandardTypeDO::getCode, code));
    }

    default List<HrmInsuranceStandardTypeDO> selectListByAreaId(Long areaId) {
        return selectList(new LambdaQueryWrapperX<HrmInsuranceStandardTypeDO>()
                .eq(HrmInsuranceStandardTypeDO::getAreaId, areaId)
                .orderByAsc(HrmInsuranceStandardTypeDO::getId));
    }

}
