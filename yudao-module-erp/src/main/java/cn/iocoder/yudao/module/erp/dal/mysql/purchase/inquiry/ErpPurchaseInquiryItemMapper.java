package cn.iocoder.yudao.module.erp.dal.mysql.purchase.inquiry;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.inquiry.ErpPurchaseInquiryItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * ERP 采购询价单明细 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpPurchaseInquiryItemMapper extends BaseMapperX<ErpPurchaseInquiryItemDO> {

    default List<ErpPurchaseInquiryItemDO> selectListByInquiryId(Long inquiryId) {
        return selectList(ErpPurchaseInquiryItemDO::getInquiryId, inquiryId);
    }

    default List<ErpPurchaseInquiryItemDO> selectListByInquiryIds(Collection<Long> inquiryIds) {
        return selectList(ErpPurchaseInquiryItemDO::getInquiryId, inquiryIds);
    }

    default int deleteByInquiryId(Long inquiryId) {
        return delete(ErpPurchaseInquiryItemDO::getInquiryId, inquiryId);
    }

}
