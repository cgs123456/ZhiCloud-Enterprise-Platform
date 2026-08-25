package cn.zhicloud.module.erp.dal.mysql.finance.cost;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.controller.admin.finance.vo.actualcost.ErpActualCostPageReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.cost.ErpActualCostDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpActualCostMapper extends BaseMapperX<ErpActualCostDO> {

    default ErpActualCostDO selectByProductAndPeriod(Long productId, String costPeriod, Long costItemId) {
        return selectOne(new LambdaQueryWrapperX<ErpActualCostDO>()
                .eq(ErpActualCostDO::getProductId, productId)
                .eq(ErpActualCostDO::getCostPeriod, costPeriod)
                .eq(ErpActualCostDO::getCostItemId, costItemId));
    }

    default List<ErpActualCostDO> selectListByProductAndPeriod(Long productId, String costPeriod) {
        return selectList(new LambdaQueryWrapperX<ErpActualCostDO>()
                .eq(ErpActualCostDO::getProductId, productId)
                .eq(ErpActualCostDO::getCostPeriod, costPeriod));
    }

    default PageResult<ErpActualCostDO> selectPage(ErpActualCostPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpActualCostDO>()
                .eqIfPresent(ErpActualCostDO::getProductId, reqVO.getProductId())
                .eqIfPresent(ErpActualCostDO::getCostPeriod, reqVO.getCostPeriod())
                .eqIfPresent(ErpActualCostDO::getCostItemId, reqVO.getCostItemId())
                .likeIfPresent(ErpActualCostDO::getProductCode, reqVO.getProductCode())
                .orderByDesc(ErpActualCostDO::getCostPeriod));
    }

}
