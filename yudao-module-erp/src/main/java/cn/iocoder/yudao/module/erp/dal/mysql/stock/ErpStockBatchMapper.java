package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stockbatch.ErpStockBatchPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockBatchDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

/**
 * ERP 库存批次 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpStockBatchMapper extends BaseMapperX<ErpStockBatchDO> {

    default ErpStockBatchDO selectByBatchNo(String batchNo) {
        return selectOne(ErpStockBatchDO::getBatchNo, batchNo);
    }

    default PageResult<ErpStockBatchDO> selectPage(ErpStockBatchPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpStockBatchDO>()
                .likeIfPresent(ErpStockBatchDO::getBatchNo, reqVO.getBatchNo())
                .eqIfPresent(ErpStockBatchDO::getProductId, reqVO.getProductId())
                .eqIfPresent(ErpStockBatchDO::getWarehouseId, reqVO.getWarehouseId())
                .eqIfPresent(ErpStockBatchDO::getStatus, reqVO.getStatus())
                .orderByDesc(ErpStockBatchDO::getId));
    }

    default List<ErpStockBatchDO> selectListByExpiryDate(LocalDate date) {
        return selectList(new LambdaQueryWrapperX<ErpStockBatchDO>()
                .lt(ErpStockBatchDO::getExpiryDate, date)
                .ne(ErpStockBatchDO::getStatus, 30)); // 排除已过期，仅返回即将过期
    }

}
