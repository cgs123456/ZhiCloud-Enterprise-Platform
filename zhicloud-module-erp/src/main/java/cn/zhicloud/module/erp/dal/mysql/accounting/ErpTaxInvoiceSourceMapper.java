package cn.zhicloud.module.erp.dal.mysql.accounting;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.dal.dataobject.accounting.ErpTaxInvoiceSourceDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 发票来源关联 Mapper。
 *
 * @author 智云
 */
@Mapper
public interface ErpTaxInvoiceSourceMapper extends BaseMapperX<ErpTaxInvoiceSourceDO> {

    default List<ErpTaxInvoiceSourceDO> selectListByInvoiceId(Long invoiceId) {
        return selectList(ErpTaxInvoiceSourceDO::getInvoiceId, invoiceId);
    }

    default List<ErpTaxInvoiceSourceDO> selectListBySourceDoc(String sourceDocType, Long sourceDocId) {
        return selectList(new LambdaQueryWrapperX<ErpTaxInvoiceSourceDO>()
                .eq(ErpTaxInvoiceSourceDO::getSourceDocType, sourceDocType)
                .eq(ErpTaxInvoiceSourceDO::getSourceDocId, sourceDocId)
                .orderByAsc(ErpTaxInvoiceSourceDO::getId));
    }

}
