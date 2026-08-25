package cn.zhicloud.module.erp.dal.mysql.finance.cost;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.controller.admin.finance.vo.workordercost.ErpWorkOrderCostPageReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.cost.ErpWorkOrderCostDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpWorkOrderCostMapper extends BaseMapperX<ErpWorkOrderCostDO> {

    default ErpWorkOrderCostDO selectByWorkOrderId(Long workOrderId) {
        return selectOne(ErpWorkOrderCostDO::getWorkOrderId, workOrderId);
    }

    default List<ErpWorkOrderCostDO> selectListByProductAndPeriod(Long productId, String costPeriod) {
        return selectList(new LambdaQueryWrapperX<ErpWorkOrderCostDO>()
                .eq(ErpWorkOrderCostDO::getProductId, productId)
                .eq(ErpWorkOrderCostDO::getCostPeriod, costPeriod));
    }

    default PageResult<ErpWorkOrderCostDO> selectPage(ErpWorkOrderCostPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpWorkOrderCostDO>()
                .eqIfPresent(ErpWorkOrderCostDO::getWorkOrderId, reqVO.getWorkOrderId())
                .eqIfPresent(ErpWorkOrderCostDO::getProductId, reqVO.getProductId())
                .eqIfPresent(ErpWorkOrderCostDO::getCostPeriod, reqVO.getCostPeriod())
                .likeIfPresent(ErpWorkOrderCostDO::getWorkOrderCode, reqVO.getWorkOrderCode())
                .orderByDesc(ErpWorkOrderCostDO::getCostPeriod));
    }

}
