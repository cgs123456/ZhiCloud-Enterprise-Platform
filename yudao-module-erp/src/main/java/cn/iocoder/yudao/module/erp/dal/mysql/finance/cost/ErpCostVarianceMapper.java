package cn.iocoder.yudao.module.erp.dal.mysql.finance.cost;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.costvariance.ErpCostVariancePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.cost.ErpCostVarianceDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpCostVarianceMapper extends BaseMapperX<ErpCostVarianceDO> {

    default List<ErpCostVarianceDO> selectListByProductAndPeriod(Long productId, String costPeriod) {
        return selectList(new LambdaQueryWrapperX<ErpCostVarianceDO>()
                .eq(ErpCostVarianceDO::getProductId, productId)
                .eq(ErpCostVarianceDO::getCostPeriod, costPeriod));
    }

    default PageResult<ErpCostVarianceDO> selectPage(ErpCostVariancePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpCostVarianceDO>()
                .eqIfPresent(ErpCostVarianceDO::getProductId, reqVO.getProductId())
                .eqIfPresent(ErpCostVarianceDO::getCostPeriod, reqVO.getCostPeriod())
                .eqIfPresent(ErpCostVarianceDO::getCostItemId, reqVO.getCostItemId())
                .eqIfPresent(ErpCostVarianceDO::getVarianceType, reqVO.getVarianceType())
                .orderByDesc(ErpCostVarianceDO::getCostPeriod));
    }

}
