package cn.iocoder.yudao.module.erp.dal.mysql.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.costallocation.ErpCostAllocationPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpCostAllocationDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 成本分摊 Mapper
 *
 * @author 芋道源码
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
