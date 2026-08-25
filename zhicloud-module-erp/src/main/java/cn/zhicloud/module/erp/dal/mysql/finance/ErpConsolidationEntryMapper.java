package cn.zhicloud.module.erp.dal.mysql.finance;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpConsolidationEntryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 合并报表抵消分录 Mapper（P0-14）
 *
 * @author 智云
 */
@Mapper
public interface ErpConsolidationEntryMapper extends BaseMapperX<ErpConsolidationEntryDO> {

    default List<ErpConsolidationEntryDO> selectListByConsolidationNo(String consolidationNo) {
        return selectList(ErpConsolidationEntryDO::getConsolidationNo, consolidationNo);
    }

    default List<ErpConsolidationEntryDO> selectListByPeriodId(Long periodId) {
        return selectList(ErpConsolidationEntryDO::getPeriodId, periodId);
    }

    default List<ErpConsolidationEntryDO> selectListByPeriodCode(String periodCode) {
        return selectList(ErpConsolidationEntryDO::getPeriodCode, periodCode);
    }

    default List<ErpConsolidationEntryDO> selectListByPeriodIdAndStatus(Long periodId, Integer status) {
        return selectList(new LambdaQueryWrapperX<ErpConsolidationEntryDO>()
                .eq(ErpConsolidationEntryDO::getPeriodId, periodId)
                .eq(ErpConsolidationEntryDO::getStatus, status)
                .orderByAsc(ErpConsolidationEntryDO::getEliminationType)
                .orderByAsc(ErpConsolidationEntryDO::getId));
    }

    default void deleteByConsolidationNo(String consolidationNo) {
        delete(new LambdaQueryWrapperX<ErpConsolidationEntryDO>()
                .eq(ErpConsolidationEntryDO::getConsolidationNo, consolidationNo));
    }

}
