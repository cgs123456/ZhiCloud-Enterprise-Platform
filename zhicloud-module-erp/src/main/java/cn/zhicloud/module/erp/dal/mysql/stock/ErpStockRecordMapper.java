package cn.zhicloud.module.erp.dal.mysql.stock;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.controller.admin.stock.vo.record.ErpStockRecordPageReqVO;
import cn.zhicloud.module.erp.dal.dataobject.stock.ErpStockRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ERP 产品库存明细 Mapper
 *
 * @author 智云
 */
@Mapper
public interface ErpStockRecordMapper extends BaseMapperX<ErpStockRecordDO> {

    default PageResult<ErpStockRecordDO> selectPage(ErpStockRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpStockRecordDO>()
                .eqIfPresent(ErpStockRecordDO::getProductId, reqVO.getProductId())
                .eqIfPresent(ErpStockRecordDO::getWarehouseId, reqVO.getWarehouseId())
                .eqIfPresent(ErpStockRecordDO::getBizType, reqVO.getBizType())
                .likeIfPresent(ErpStockRecordDO::getBizNo, reqVO.getBizNo())
                .betweenIfPresent(ErpStockRecordDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ErpStockRecordDO::getId));
    }

    /**
     * 查询指定时间范围内的出库记录（count < 0）
     * 用于BI分析，避免全表扫描
     */
    default List<ErpStockRecordDO> selectOutRecordsBetween(LocalDateTime beginTime, LocalDateTime endTime) {
        return selectList(new LambdaQueryWrapperX<ErpStockRecordDO>()
                .lt(ErpStockRecordDO::getCount, BigDecimal.ZERO)
                .ge(ErpStockRecordDO::getCreateTime, beginTime)
                .le(ErpStockRecordDO::getCreateTime, endTime));
    }

}