package cn.zhicloud.module.erp.dal.mysql.finance.tax;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.controller.admin.finance.vo.taxinvoiceline.ErpTaxInvoiceLinePageReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.tax.ErpTaxInvoiceLineDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpTaxInvoiceLineMapper extends BaseMapperX<ErpTaxInvoiceLineDO> {

    default List<ErpTaxInvoiceLineDO> selectListByInvoiceId(Long invoiceId) {
        return selectList(ErpTaxInvoiceLineDO::getInvoiceId, invoiceId);
    }

    default int deleteByInvoiceId(Long invoiceId) {
        return delete(new LambdaQueryWrapperX<ErpTaxInvoiceLineDO>()
                .eq(ErpTaxInvoiceLineDO::getInvoiceId, invoiceId));
    }

    default PageResult<ErpTaxInvoiceLineDO> selectPage(ErpTaxInvoiceLinePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpTaxInvoiceLineDO>()
                .eqIfPresent(ErpTaxInvoiceLineDO::getInvoiceId, reqVO.getInvoiceId())
                .likeIfPresent(ErpTaxInvoiceLineDO::getProductName, reqVO.getProductName())
                .orderByAsc(ErpTaxInvoiceLineDO::getLineNo));
    }

}
