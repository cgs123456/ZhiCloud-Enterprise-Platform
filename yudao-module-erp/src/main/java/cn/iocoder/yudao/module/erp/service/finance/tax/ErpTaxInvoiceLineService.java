package cn.iocoder.yudao.module.erp.service.finance.tax;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.taxinvoiceline.ErpTaxInvoiceLinePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.taxinvoiceline.ErpTaxInvoiceLineSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.tax.ErpTaxInvoiceLineDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * ERP 发票明细 Service 接口
 *
 * @author 芋道源码
 */
public interface ErpTaxInvoiceLineService {

    /**
     * 创建发票明细
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createTaxInvoiceLine(@Valid ErpTaxInvoiceLineSaveReqVO createReqVO);

    /**
     * 更新发票明细
     *
     * @param updateReqVO 更新信息
     */
    void updateTaxInvoiceLine(@Valid ErpTaxInvoiceLineSaveReqVO updateReqVO);

    /**
     * 删除发票明细
     *
     * @param id 编号
     */
    void deleteTaxInvoiceLine(Long id);

    /**
     * 获得发票明细
     *
     * @param id 编号
     * @return 发票明细
     */
    ErpTaxInvoiceLineDO getTaxInvoiceLine(Long id);

    /**
     * 获得发票明细分页
     *
     * @param pageReqVO 分页查询
     * @return 发票明细分页
     */
    PageResult<ErpTaxInvoiceLineDO> getTaxInvoiceLinePage(ErpTaxInvoiceLinePageReqVO pageReqVO);

    /**
     * 根据发票 ID 获得明细列表
     *
     * @param invoiceId 发票 ID
     * @return 发票明细列表
     */
    List<ErpTaxInvoiceLineDO> getTaxInvoiceLineListByInvoiceId(Long invoiceId);

}
