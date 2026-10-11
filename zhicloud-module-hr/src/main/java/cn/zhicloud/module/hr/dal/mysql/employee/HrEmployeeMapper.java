package cn.zhicloud.module.hr.dal.mysql.employee;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.controller.admin.employee.vo.HrEmployeePageReqVO;
import cn.zhicloud.module.hr.controller.admin.hrm.vo.HrmEmployeePageReqVO;
import cn.zhicloud.module.hr.dal.dataobject.employee.HrEmployeeDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * HR 员工档案 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrEmployeeMapper extends BaseMapperX<HrEmployeeDO> {

    default PageResult<HrEmployeeDO> selectPage(HrEmployeePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrEmployeeDO>()
                .likeIfPresent(HrEmployeeDO::getEmpNo, reqVO.getEmpNo())
                .likeIfPresent(HrEmployeeDO::getName, reqVO.getName())
                .eqIfPresent(HrEmployeeDO::getGender, reqVO.getGender())
                .eqIfPresent(HrEmployeeDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(HrEmployeeDO::getPositionId, reqVO.getPositionId())
                .eqIfPresent(HrEmployeeDO::getStatus, reqVO.getStatus())
                .eqIfPresent(HrEmployeeDO::getEmploymentType, reqVO.getEmploymentType())
                .orderByDesc(HrEmployeeDO::getId));
    }

    default HrEmployeeDO selectByEmpNo(String empNo) {
        return selectOne(HrEmployeeDO::getEmpNo, empNo);
    }

    default Long selectCountByDeptId(Long deptId) {
        return selectCount(HrEmployeeDO::getDeptId, deptId);
    }

    default Long selectCountByPositionId(Long positionId) {
        return selectCount(HrEmployeeDO::getPositionId, positionId);
    }

    default List<HrEmployeeDO> selectListByDeptIds(Collection<Long> deptIds) {
        return selectList(HrEmployeeDO::getDeptId, deptIds);
    }

    // ========== HRM 融合（V161 超集列；查询走新轴，旧列只做同步回写） ==========

    /**
     * 构建 HRM 列表通用过滤条件（不含状态页签；状态页签由调用方按 tab 语义叠加）
     */
    private LambdaQueryWrapperX<HrEmployeeDO> buildHrmFilter(HrmEmployeePageReqVO reqVO, Collection<Long> deptIds) {
        LambdaQueryWrapperX<HrEmployeeDO> wrapper = new LambdaQueryWrapperX<HrEmployeeDO>()
                .likeIfPresent(HrEmployeeDO::getName, reqVO.getName())
                .likeIfPresent(HrEmployeeDO::getPhone, reqVO.getMobile())
                .eqIfPresent(HrEmployeeDO::getSex, reqVO.getSex())
                .betweenIfPresent(HrEmployeeDO::getEntryTime, reqVO.getEntryTime())
                .likeIfPresent(HrEmployeeDO::getEmpNo, reqVO.getJobNumber())
                .inIfPresent(HrEmployeeDO::getDeptId, deptIds)
                .eqIfPresent(HrEmployeeDO::getLeaderEmployeeId, reqVO.getLeaderEmployeeId())
                .likeIfPresent(HrEmployeeDO::getPostName, reqVO.getPostName())
                .betweenIfPresent(HrEmployeeDO::getRegularTime, reqVO.getRegularTime())
                .likeIfPresent(HrEmployeeDO::getWorkAddress, reqVO.getWorkAddress())
                .eqIfPresent(HrEmployeeDO::getChannelId, reqVO.getChannelId())
                .eqIfPresent(HrEmployeeDO::getType, reqVO.getType())
                .eqIfPresent(HrEmployeeDO::getEntryStatus, reqVO.getEntryStatus())
                .eqIfPresent(HrEmployeeDO::getEmployeeStatus, reqVO.getStatus());
        // 状态页签：11在职 12全职 13待入职 14待离职 15已离职
        Integer tab = reqVO.getStatusCategory();
        if (tab != null) {
            if (tab == 11) {
                wrapper.in(HrEmployeeDO::getEntryStatus, 1, 2, 3);
            } else if (tab == 12) {
                wrapper.in(HrEmployeeDO::getEntryStatus, 1, 2, 3).eq(HrEmployeeDO::getType, 1);
            } else if (tab == 13) {
                wrapper.eq(HrEmployeeDO::getEntryStatus, 2);
            } else if (tab == 14) {
                wrapper.eq(HrEmployeeDO::getEntryStatus, 3);
            } else if (tab == 15) {
                wrapper.eq(HrEmployeeDO::getEntryStatus, 4);
            }
        }
        return wrapper;
    }

    default PageResult<HrEmployeeDO> selectHrmPage(HrmEmployeePageReqVO reqVO, Collection<Long> deptIds) {
        return selectPage(reqVO, buildHrmFilter(reqVO, deptIds).orderByDesc(HrEmployeeDO::getId));
    }

    default List<HrEmployeeDO> selectHrmListByEntryStatusIn(Collection<Integer> entryStatuses) {
        return selectList(new LambdaQueryWrapperX<HrEmployeeDO>()
                .in(HrEmployeeDO::getEntryStatus, entryStatuses));
    }

    /**
     * 按页签计数：在通用过滤之上叠加页签的 entryStatus/type 语义
     */
    default Long selectHrmCountByFilter(HrmEmployeePageReqVO reqVO, Collection<Long> deptIds,
                                        Collection<Integer> entryStatuses, Integer type) {
        return selectCount(buildHrmFilter(reqVO, deptIds)
                .in(HrEmployeeDO::getEntryStatus, entryStatuses)
                .eqIfPresent(HrEmployeeDO::getType, type));
    }

    default List<Long> selectBoundUserIdList() {
        List<HrEmployeeDO> list = selectList(new LambdaQueryWrapperX<HrEmployeeDO>()
                .isNotNull(HrEmployeeDO::getUserId)
                .select(HrEmployeeDO::getUserId));
        return list.stream().map(HrEmployeeDO::getUserId).distinct().toList();
    }

    default List<HrEmployeeDO> selectListByUserIds(Collection<Long> userIds) {
        return selectList(HrEmployeeDO::getUserId, userIds);
    }

}