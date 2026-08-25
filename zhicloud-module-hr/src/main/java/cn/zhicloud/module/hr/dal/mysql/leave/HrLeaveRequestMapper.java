package cn.zhicloud.module.hr.dal.mysql.leave;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.controller.admin.leave.vo.HrLeaveRequestPageReqVO;
import cn.zhicloud.module.hr.dal.dataobject.leave.HrLeaveRequestDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HrLeaveRequestMapper extends BaseMapperX<HrLeaveRequestDO> {

    default PageResult<HrLeaveRequestDO> selectPage(HrLeaveRequestPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrLeaveRequestDO>()
                .eqIfPresent(HrLeaveRequestDO::getEmployeeId, reqVO.getEmployeeId())
                .eqIfPresent(HrLeaveRequestDO::getLeaveTypeId, reqVO.getLeaveTypeId())
                .eqIfPresent(HrLeaveRequestDO::getStatus, reqVO.getStatus())
                .orderByDesc(HrLeaveRequestDO::getId));
    }

}