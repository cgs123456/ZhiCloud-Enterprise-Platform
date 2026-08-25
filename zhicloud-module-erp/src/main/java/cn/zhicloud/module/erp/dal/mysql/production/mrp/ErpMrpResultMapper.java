package cn.zhicloud.module.erp.dal.mysql.production.mrp;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.controller.admin.production.mrp.vo.ErpMrpResultPageReqVO;
import cn.zhicloud.module.erp.dal.dataobject.production.mrp.ErpMrpResultDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 物料需求计划结果 Mapper
 *
 * @author 智云
 */
@Mapper
public interface ErpMrpResultMapper extends BaseMapperX<ErpMrpResultDO> {

    default PageResult<ErpMrpResultDO> selectPage(ErpMrpResultPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpMrpResultDO>()
                .eqIfPresent(ErpMrpResultDO::getPlanId, reqVO.getPlanId())
                .eqIfPresent(ErpMrpResultDO::getProductId, reqVO.getProductId())
                .eqIfPresent(ErpMrpResultDO::getDemandType, reqVO.getDemandType())
                .eqIfPresent(ErpMrpResultDO::getPlannedOrderType, reqVO.getPlannedOrderType())
                .orderByAsc(ErpMrpResultDO::getId));
    }

    default List<ErpMrpResultDO> selectListByPlanId(Long planId) {
        return selectList(new LambdaQueryWrapperX<ErpMrpResultDO>()
                .eqIfPresent(ErpMrpResultDO::getPlanId, planId)
                .orderByAsc(ErpMrpResultDO::getId));
    }

    default int deleteByPlanId(Long planId) {
        return delete(new LambdaQueryWrapperX<ErpMrpResultDO>()
                .eqIfPresent(ErpMrpResultDO::getPlanId, planId));
    }

}
