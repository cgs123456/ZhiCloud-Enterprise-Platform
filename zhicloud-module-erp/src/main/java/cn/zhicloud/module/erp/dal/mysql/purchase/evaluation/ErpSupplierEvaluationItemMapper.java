package cn.zhicloud.module.erp.dal.mysql.purchase.evaluation;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.dal.dataobject.purchase.evaluation.ErpSupplierEvaluationItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 供应商评估指标项 Mapper
 *
 * @author 智云
 */
@Mapper
public interface ErpSupplierEvaluationItemMapper extends BaseMapperX<ErpSupplierEvaluationItemDO> {

    default List<ErpSupplierEvaluationItemDO> selectListByEvaluationId(Long evaluationId) {
        return selectList(new LambdaQueryWrapperX<ErpSupplierEvaluationItemDO>()
                .eqIfPresent(ErpSupplierEvaluationItemDO::getEvaluationId, evaluationId)
                .orderByAsc(ErpSupplierEvaluationItemDO::getId));
    }

    default int deleteByEvaluationId(Long evaluationId) {
        return delete(new LambdaQueryWrapperX<ErpSupplierEvaluationItemDO>()
                .eqIfPresent(ErpSupplierEvaluationItemDO::getEvaluationId, evaluationId));
    }

}
