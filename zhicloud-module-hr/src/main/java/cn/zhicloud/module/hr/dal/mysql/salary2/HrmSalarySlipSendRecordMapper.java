package cn.zhicloud.module.hr.dal.mysql.salary2;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.salary2.HrmSalarySlipSendRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 工资条发放记录 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmSalarySlipSendRecordMapper extends BaseMapperX<HrmSalarySlipSendRecordDO> {

    default List<HrmSalarySlipSendRecordDO> selectListByMonthRecordId(Long monthRecordId) {
        return selectList(new LambdaQueryWrapperX<HrmSalarySlipSendRecordDO>()
                .eq(HrmSalarySlipSendRecordDO::getMonthRecordId, monthRecordId)
                .orderByDesc(HrmSalarySlipSendRecordDO::getId));
    }
}
