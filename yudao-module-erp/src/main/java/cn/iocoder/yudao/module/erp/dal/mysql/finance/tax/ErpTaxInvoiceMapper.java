package cn.iocoder.yudao.module.erp.dal.mysql.finance.tax;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.taxinvoice.ErpTaxInvoicePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.tax.ErpTaxInvoiceDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ErpTaxInvoiceMapper extends BaseMapperX<ErpTaxInvoiceDO> {

    default ErpTaxInvoiceDO selectByInvoiceNoAndCode(String invoiceNo, String invoiceCode) {
        return selectOne(new LambdaQueryWrapperX<ErpTaxInvoiceDO>()
                .eq(ErpTaxInvoiceDO::getInvoiceNo, invoiceNo)
                .eq(ErpTaxInvoiceDO::getInvoiceCode, invoiceCode));
    }

    default List<ErpTaxInvoiceDO> selectListByPeriod(LocalDate startDate, LocalDate endDate, Integer invoiceType) {
        return selectList(new LambdaQueryWrapperX<ErpTaxInvoiceDO>()
                .eqIfPresent(ErpTaxInvoiceDO::getInvoiceType, invoiceType)
                .geIfPresent(ErpTaxInvoiceDO::getInvoiceDate, startDate)
                .leIfPresent(ErpTaxInvoiceDO::getInvoiceDate, endDate)
                .eq(ErpTaxInvoiceDO::getStatus, 20));
    }

    default PageResult<ErpTaxInvoiceDO> selectPage(ErpTaxInvoicePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpTaxInvoiceDO>()
                .likeIfPresent(ErpTaxInvoiceDO::getInvoiceNo, reqVO.getInvoiceNo())
                .eqIfPresent(ErpTaxInvoiceDO::getInvoiceType, reqVO.getInvoiceType())
                .eqIfPresent(ErpTaxInvoiceDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpTaxInvoiceDO::getBuyerName, reqVO.getBuyerName())
                .likeIfPresent(ErpTaxInvoiceDO::getSellerName, reqVO.getSellerName())
                .orderByDesc(ErpTaxInvoiceDO::getInvoiceDate));
    }

}
