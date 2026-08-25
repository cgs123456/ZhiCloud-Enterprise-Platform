package cn.zhicloud.module.hr.dal.mysql.socialinsurance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.controller.admin.socialinsurance.vo.HrSocialInsurancePageReqVO;
import cn.zhicloud.module.hr.dal.dataobject.socialinsurance.HrSocialInsuranceBaseDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HrSocialInsuranceBaseMapper extends BaseMapperX<HrSocialInsuranceBaseDO> {

    default PageResult<HrSocialInsuranceBaseDO> selectPage(HrSocialInsurancePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrSocialInsuranceBaseDO>()
                .eqIfPresent(HrSocialInsuranceBaseDO::getEmployeeId, reqVO.getEmployeeId())
                .eqIfPresent(HrSocialInsuranceBaseDO::getYear, reqVO.getYear())
                .eqIfPresent(HrSocialInsuranceBaseDO::getStatus, reqVO.getStatus())
                .orderByDesc(HrSocialInsuranceBaseDO::getId));
    }

    default HrSocialInsuranceBaseDO selectByEmployeeAndYear(Long employeeId, Integer year) {
        return selectOne(new LambdaQueryWrapperX<HrSocialInsuranceBaseDO>()
                .eq(HrSocialInsuranceBaseDO::getEmployeeId, employeeId)
                .eq(HrSocialInsuranceBaseDO::getYear, year));
    }

}