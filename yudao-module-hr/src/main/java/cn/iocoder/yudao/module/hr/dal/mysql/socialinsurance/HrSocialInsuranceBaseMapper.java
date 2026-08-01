package cn.iocoder.yudao.module.hr.dal.mysql.socialinsurance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hr.controller.admin.socialinsurance.vo.HrSocialInsurancePageReqVO;
import cn.iocoder.yudao.module.hr.dal.dataobject.socialinsurance.HrSocialInsuranceBaseDO;
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