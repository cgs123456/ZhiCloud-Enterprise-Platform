package cn.zhicloud.module.erp.dal.mysql.stock;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.controller.admin.stock.vo.stockserial.ErpStockSerialPageReqVO;
import cn.zhicloud.module.erp.dal.dataobject.stock.ErpStockSerialDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 库存序列号 Mapper
 *
 * @author 智云
 */
@Mapper
public interface ErpStockSerialMapper extends BaseMapperX<ErpStockSerialDO> {

    default ErpStockSerialDO selectBySerialNo(String serialNo) {
        return selectOne(ErpStockSerialDO::getSerialNo, serialNo);
    }

    default PageResult<ErpStockSerialDO> selectPage(ErpStockSerialPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpStockSerialDO>()
                .likeIfPresent(ErpStockSerialDO::getSerialNo, reqVO.getSerialNo())
                .eqIfPresent(ErpStockSerialDO::getProductId, reqVO.getProductId())
                .eqIfPresent(ErpStockSerialDO::getWarehouseId, reqVO.getWarehouseId())
                .eqIfPresent(ErpStockSerialDO::getBatchId, reqVO.getBatchId())
                .eqIfPresent(ErpStockSerialDO::getStatus, reqVO.getStatus())
                .orderByDesc(ErpStockSerialDO::getId));
    }

}
