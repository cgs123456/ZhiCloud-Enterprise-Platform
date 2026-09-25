package cn.zhicloud.module.erp.dal.mysql.accounting;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.dal.dataobject.accounting.ErpFinanceSettlementAllocationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 财务结算分配 Mapper。
 *
 * @author 智云
 */
@Mapper
public interface ErpFinanceSettlementAllocationMapper extends BaseMapperX<ErpFinanceSettlementAllocationDO> {

    default List<ErpFinanceSettlementAllocationDO> selectListByFinanceDoc(String financeDocType, Long financeDocId) {
        return selectList(new LambdaQueryWrapperX<ErpFinanceSettlementAllocationDO>()
                .eq(ErpFinanceSettlementAllocationDO::getFinanceDocType, financeDocType)
                .eq(ErpFinanceSettlementAllocationDO::getFinanceDocId, financeDocId)
                .orderByAsc(ErpFinanceSettlementAllocationDO::getId));
    }

    default List<ErpFinanceSettlementAllocationDO> selectListByInvoice(Long invoiceId, Long invoiceLineId) {
        return selectList(new LambdaQueryWrapperX<ErpFinanceSettlementAllocationDO>()
                .eq(ErpFinanceSettlementAllocationDO::getInvoiceId, invoiceId)
                .eq(ErpFinanceSettlementAllocationDO::getInvoiceLineId, invoiceLineId)
                .orderByAsc(ErpFinanceSettlementAllocationDO::getId));
    }

}
