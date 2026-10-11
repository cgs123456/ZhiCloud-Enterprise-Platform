package cn.zhicloud.module.hr.dal.mysql.performance;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.performance.HrmPerformanceProcessRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 绩效流程记录 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmPerformanceProcessRecordMapper extends BaseMapperX<HrmPerformanceProcessRecordDO> {

    default List<HrmPerformanceProcessRecordDO> selectListByBiz(Integer bizType, Long bizId) {
        return selectList(new LambdaQueryWrapperX<HrmPerformanceProcessRecordDO>()
                .eq(HrmPerformanceProcessRecordDO::getBizType, bizType)
                .eq(HrmPerformanceProcessRecordDO::getBizId, bizId)
                .orderByDesc(HrmPerformanceProcessRecordDO::getOperateTime)
                .orderByDesc(HrmPerformanceProcessRecordDO::getId));
    }

    default void deleteByBiz(Integer bizType, Long bizId) {
        delete(new LambdaQueryWrapperX<HrmPerformanceProcessRecordDO>()
                .eq(HrmPerformanceProcessRecordDO::getBizType, bizType)
                .eq(HrmPerformanceProcessRecordDO::getBizId, bizId));
    }

}
