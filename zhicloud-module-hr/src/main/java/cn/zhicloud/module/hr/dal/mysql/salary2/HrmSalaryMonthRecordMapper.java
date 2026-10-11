package cn.zhicloud.module.hr.dal.mysql.salary2;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.salary2.HrmSalaryMonthRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 月度工资表 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmSalaryMonthRecordMapper extends BaseMapperX<HrmSalaryMonthRecordDO> {

    default List<HrmSalaryMonthRecordDO> selectListByYear(Integer year) {
        return selectList(new LambdaQueryWrapperX<HrmSalaryMonthRecordDO>()
                .eqIfPresent(HrmSalaryMonthRecordDO::getYear, year)
                .orderByDesc(HrmSalaryMonthRecordDO::getYear)
                .orderByDesc(HrmSalaryMonthRecordDO::getMonth));
    }

    default HrmSalaryMonthRecordDO selectByYearMonth(Integer year, Integer month) {
        return selectOne(new LambdaQueryWrapperX<HrmSalaryMonthRecordDO>()
                .eq(HrmSalaryMonthRecordDO::getYear, year)
                .eq(HrmSalaryMonthRecordDO::getMonth, month));
    }

    default HrmSalaryMonthRecordDO selectLast() {
        List<HrmSalaryMonthRecordDO> list = selectList(new LambdaQueryWrapperX<HrmSalaryMonthRecordDO>()
                .orderByDesc(HrmSalaryMonthRecordDO::getYear)
                .orderByDesc(HrmSalaryMonthRecordDO::getMonth));
        return list.isEmpty() ? null : list.get(0);
    }
}
