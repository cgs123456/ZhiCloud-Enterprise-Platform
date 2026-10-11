package cn.zhicloud.module.hr.dal.mysql.hrm;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.hrm.HrmEmployeeContractDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 员工合同 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmEmployeeContractMapper extends BaseMapperX<HrmEmployeeContractDO> {

    default List<HrmEmployeeContractDO> selectListByEmployeeId(Long employeeId) {
        return selectList(new LambdaQueryWrapperX<HrmEmployeeContractDO>()
                .eq(HrmEmployeeContractDO::getEmployeeId, employeeId)
                .orderByAsc(HrmEmployeeContractDO::getSort)
                .orderByAsc(HrmEmployeeContractDO::getId));
    }

}
