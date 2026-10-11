package cn.zhicloud.module.hr.dal.mysql.hrm;

import cn.zhicloud.framework.common.pojo.PageParam;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.hrm.HrmAttendanceHolidayDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * HRM 考勤节假日 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmAttendanceHolidayMapper extends BaseMapperX<HrmAttendanceHolidayDO> {

    default PageResult<HrmAttendanceHolidayDO> selectPage(PageParam pageParam, LocalDate[] dates, Integer type) {
        return selectPage(pageParam, new LambdaQueryWrapperX<HrmAttendanceHolidayDO>()
                .betweenIfPresent(HrmAttendanceHolidayDO::getDate, dates)
                .eqIfPresent(HrmAttendanceHolidayDO::getType, type)
                .orderByDesc(HrmAttendanceHolidayDO::getDate));
    }

    default HrmAttendanceHolidayDO selectByDate(LocalDate date) {
        return selectOne(HrmAttendanceHolidayDO::getDate, date);
    }

    default List<HrmAttendanceHolidayDO> selectListByDates(Collection<LocalDate> dates) {
        return selectList(HrmAttendanceHolidayDO::getDate, dates);
    }

}
