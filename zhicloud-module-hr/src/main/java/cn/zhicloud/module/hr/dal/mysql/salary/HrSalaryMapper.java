package cn.zhicloud.module.hr.dal.mysql.salary;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.controller.admin.salary.vo.HrSalaryPageReqVO;
import cn.zhicloud.module.hr.dal.dataobject.salary.HrSalaryDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * HR 薪资记录 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrSalaryMapper extends BaseMapperX<HrSalaryDO> {

    default PageResult<HrSalaryDO> selectPage(HrSalaryPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrSalaryDO>()
                .eqIfPresent(HrSalaryDO::getEmployeeId, reqVO.getEmployeeId())
                .eqIfPresent(HrSalaryDO::getSalaryMonth, reqVO.getSalaryMonth())
                .eqIfPresent(HrSalaryDO::getStatus, reqVO.getStatus())
                .orderByDesc(HrSalaryDO::getSalaryMonth));
    }

    default HrSalaryDO selectByEmployeeAndMonth(Long employeeId, String salaryMonth) {
        return selectOne(HrSalaryDO::getEmployeeId, employeeId,
                HrSalaryDO::getSalaryMonth, salaryMonth);
    }

}