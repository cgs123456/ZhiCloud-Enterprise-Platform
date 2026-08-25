package cn.zhicloud.module.erp.dal.mysql.finance.cost;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.controller.admin.finance.vo.standardcost.ErpStandardCostPageReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.cost.ErpStandardCostDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ErpStandardCostMapper extends BaseMapperX<ErpStandardCostDO> {

    default ErpStandardCostDO selectByProductAndCostItem(Long productId, Long costItemId, LocalDate effectiveDate) {
        return selectOne(new LambdaQueryWrapperX<ErpStandardCostDO>()
                .eq(ErpStandardCostDO::getProductId, productId)
                .eq(ErpStandardCostDO::getCostItemId, costItemId)
                .eq(ErpStandardCostDO::getEffectiveDate, effectiveDate));
    }

    default List<ErpStandardCostDO> selectListByProduct(Long productId) {
        return selectList(ErpStandardCostDO::getProductId, productId);
    }

    default List<ErpStandardCostDO> selectEffectiveListByProduct(Long productId, LocalDate date) {
        return selectList(new LambdaQueryWrapperX<ErpStandardCostDO>()
                .eq(ErpStandardCostDO::getProductId, productId)
                .eq(ErpStandardCostDO::getStatus, 20)
                .le(ErpStandardCostDO::getEffectiveDate, date)
                .and(w -> w.isNull(ErpStandardCostDO::getExpiryDate).or().ge(ErpStandardCostDO::getExpiryDate, date)));
    }

    default PageResult<ErpStandardCostDO> selectPage(ErpStandardCostPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpStandardCostDO>()
                .eqIfPresent(ErpStandardCostDO::getProductId, reqVO.getProductId())
                .eqIfPresent(ErpStandardCostDO::getCostItemId, reqVO.getCostItemId())
                .eqIfPresent(ErpStandardCostDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpStandardCostDO::getProductCode, reqVO.getProductCode())
                .orderByDesc(ErpStandardCostDO::getEffectiveDate));
    }

}
