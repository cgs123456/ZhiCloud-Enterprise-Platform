package cn.iocoder.yudao.module.crm.dal.mysql.invoice;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.crm.dal.dataobject.invoice.CrmInvoiceLineDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * CRM 开票明细 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface CrmInvoiceLineMapper extends BaseMapperX<CrmInvoiceLineDO> {

    default List<CrmInvoiceLineDO> selectListByInvoiceId(Long invoiceId) {
        return selectList(CrmInvoiceLineDO::getInvoiceId, invoiceId);
    }

    default List<CrmInvoiceLineDO> selectListByInvoiceIds(Collection<Long> invoiceIds) {
        return selectList(new LambdaQueryWrapperX<CrmInvoiceLineDO>()
                .in(CrmInvoiceLineDO::getInvoiceId, invoiceIds));
    }

    default void deleteByInvoiceId(Long invoiceId) {
        delete(new LambdaQueryWrapperX<CrmInvoiceLineDO>()
                .eq(CrmInvoiceLineDO::getInvoiceId, invoiceId));
    }

}
