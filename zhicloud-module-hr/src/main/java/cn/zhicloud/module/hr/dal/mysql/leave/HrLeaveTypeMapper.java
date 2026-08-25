package cn.zhicloud.module.hr.dal.mysql.leave;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.leave.HrLeaveTypeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HrLeaveTypeMapper extends BaseMapperX<HrLeaveTypeDO> {

    default HrLeaveTypeDO selectByCode(String code) {
        return selectOne(HrLeaveTypeDO::getCode, code);
    }

    default List<HrLeaveTypeDO> selectListAll() {
        return selectList(new LambdaQueryWrapperX<HrLeaveTypeDO>()
                .orderByAsc(HrLeaveTypeDO::getId));
    }

}