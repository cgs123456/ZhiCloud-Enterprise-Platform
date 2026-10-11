package cn.zhicloud.module.hr.dal.mysql.salary2;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.controller.admin.hrm.salary.vo.HrmSalaryGroupPageReqVO;
import cn.zhicloud.module.hr.dal.dataobject.salary2.HrmSalaryGroupDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * HRM 薪资组 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmSalaryGroupMapper extends BaseMapperX<HrmSalaryGroupDO> {

    default PageResult<HrmSalaryGroupDO> selectPage(HrmSalaryGroupPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrmSalaryGroupDO>()
                .likeIfPresent(HrmSalaryGroupDO::getName, reqVO.getName())
                .eqIfPresent(HrmSalaryGroupDO::getTaxRuleId, reqVO.getTaxRuleId())
                .orderByDesc(HrmSalaryGroupDO::getId));
    }

    default List<HrmSalaryGroupDO> selectListAll() {
        return selectList(new LambdaQueryWrapperX<HrmSalaryGroupDO>()
                .orderByDesc(HrmSalaryGroupDO::getId));
    }

    default HrmSalaryGroupDO selectByName(String name) {
        return selectOne(HrmSalaryGroupDO::getName, name);
    }

    default List<HrmSalaryGroupDO> selectListByIds(Collection<Long> ids) {
        return selectList(HrmSalaryGroupDO::getId, ids);
    }

    default Long selectCountByTaxRuleId(Long taxRuleId) {
        return selectCount(HrmSalaryGroupDO::getTaxRuleId, taxRuleId);
    }

}
