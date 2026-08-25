package cn.zhicloud.module.erp.dal.mysql.finance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.controller.admin.finance.vo.costallocation.ErpCostAllocationPageReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpCostAllocationDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 成本分摊 Mapper
 *
 * @author 智云
 */
@Mapper
public interface ErpCostAllocationMapper extends BaseMapperX<ErpCostAllocationDO> {

    default PageResult<ErpCostAllocationDO> selectPage(ErpCostAllocationPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpCostAllocationDO>()
                .eqIfPresent(ErpCostAllocationDO::getCostCenterId, reqVO.getCostCenterId())
                .eqIfPresent(ErpCostAllocationDO::getTargetCostCenterId, reqVO.getTargetCostCenterId())
                .eqIfPresent(ErpCostAllocationDO::getAllocationType, reqVO.getAllocationType())
                .orderByDesc(ErpCostAllocationDO::getAllocationDate));
    }

}
