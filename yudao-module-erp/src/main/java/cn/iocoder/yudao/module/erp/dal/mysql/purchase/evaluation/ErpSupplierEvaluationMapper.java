package cn.iocoder.yudao.module.erp.dal.mysql.purchase.evaluation;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.evaluation.vo.ErpSupplierEvaluationPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.evaluation.ErpSupplierEvaluationDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 供应商评估 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpSupplierEvaluationMapper extends BaseMapperX<ErpSupplierEvaluationDO> {

    default PageResult<ErpSupplierEvaluationDO> selectPage(ErpSupplierEvaluationPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpSupplierEvaluationDO>()
                .eqIfPresent(ErpSupplierEvaluationDO::getSupplierId, reqVO.getSupplierId())
                .eqIfPresent(ErpSupplierEvaluationDO::getEvaluationPeriod, reqVO.getEvaluationPeriod())
                .eqIfPresent(ErpSupplierEvaluationDO::getGrade, reqVO.getGrade())
                .likeIfPresent(ErpSupplierEvaluationDO::getEvaluator, reqVO.getEvaluator())
                .betweenIfPresent(ErpSupplierEvaluationDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ErpSupplierEvaluationDO::getId));
    }

    default ErpSupplierEvaluationDO selectBySupplierAndPeriod(Long supplierId, String evaluationPeriod) {
        return selectOne(ErpSupplierEvaluationDO::getSupplierId, supplierId,
                ErpSupplierEvaluationDO::getEvaluationPeriod, evaluationPeriod);
    }

}
