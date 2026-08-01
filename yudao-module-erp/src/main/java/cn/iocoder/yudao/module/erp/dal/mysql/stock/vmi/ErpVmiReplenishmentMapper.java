package cn.iocoder.yudao.module.erp.dal.mysql.stock.vmi;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vmi.vo.ErpVmiReplenishmentPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.vmi.ErpVmiReplenishmentDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP VMI 补货建议 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpVmiReplenishmentMapper extends BaseMapperX<ErpVmiReplenishmentDO> {

    default PageResult<ErpVmiReplenishmentDO> selectPage(ErpVmiReplenishmentPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpVmiReplenishmentDO>()
                .likeIfPresent(ErpVmiReplenishmentDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpVmiReplenishmentDO::getSupplierId, reqVO.getSupplierId())
                .eqIfPresent(ErpVmiReplenishmentDO::getWarehouseId, reqVO.getWarehouseId())
                .eqIfPresent(ErpVmiReplenishmentDO::getStatus, reqVO.getStatus())
                .orderByDesc(ErpVmiReplenishmentDO::getId));
    }

    default ErpVmiReplenishmentDO selectByNo(String no) {
        return selectOne(ErpVmiReplenishmentDO::getNo, no);
    }

}
