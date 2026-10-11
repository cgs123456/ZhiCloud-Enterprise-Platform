package cn.zhicloud.module.hr.dal.mysql.insurance;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.insurance.HrmInsuranceMonthRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 月度社保表 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmInsuranceMonthRecordMapper extends BaseMapperX<HrmInsuranceMonthRecordDO> {

    default List<HrmInsuranceMonthRecordDO> selectListByYear(Integer year) {
        return selectList(new LambdaQueryWrapperX<HrmInsuranceMonthRecordDO>()
                .eqIfPresent(HrmInsuranceMonthRecordDO::getYear, year)
                .orderByDesc(HrmInsuranceMonthRecordDO::getYear)
                .orderByDesc(HrmInsuranceMonthRecordDO::getMonth));
    }

    default HrmInsuranceMonthRecordDO selectByYearMonth(Integer year, Integer month) {
        return selectOne(new LambdaQueryWrapperX<HrmInsuranceMonthRecordDO>()
                .eq(HrmInsuranceMonthRecordDO::getYear, year)
                .eq(HrmInsuranceMonthRecordDO::getMonth, month));
    }

    default HrmInsuranceMonthRecordDO selectLast() {
        List<HrmInsuranceMonthRecordDO> list = selectList(new LambdaQueryWrapperX<HrmInsuranceMonthRecordDO>()
                .orderByDesc(HrmInsuranceMonthRecordDO::getYear)
                .orderByDesc(HrmInsuranceMonthRecordDO::getMonth));
        return list.isEmpty() ? null : list.get(0);
    }
}
