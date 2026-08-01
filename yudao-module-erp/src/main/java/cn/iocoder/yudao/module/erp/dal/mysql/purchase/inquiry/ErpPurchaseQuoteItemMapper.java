package cn.iocoder.yudao.module.erp.dal.mysql.purchase.inquiry;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.inquiry.ErpPurchaseQuoteItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * ERP 采购报价单明细 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpPurchaseQuoteItemMapper extends BaseMapperX<ErpPurchaseQuoteItemDO> {

    default List<ErpPurchaseQuoteItemDO> selectListByQuoteId(Long quoteId) {
        return selectList(ErpPurchaseQuoteItemDO::getQuoteId, quoteId);
    }

    default List<ErpPurchaseQuoteItemDO> selectListByQuoteIds(Collection<Long> quoteIds) {
        return selectList(ErpPurchaseQuoteItemDO::getQuoteId, quoteIds);
    }

    default int deleteByQuoteId(Long quoteId) {
        return delete(ErpPurchaseQuoteItemDO::getQuoteId, quoteId);
    }

}
