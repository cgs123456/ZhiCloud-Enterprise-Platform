package cn.zhicloud.module.erp.service.finance.tax;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.erp.controller.admin.finance.vo.taxinvoice.ErpTaxInvoicePageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.taxinvoice.ErpTaxInvoiceSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.tax.ErpTaxInvoiceDO;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * ERP 发票 Service 接口
 *
 * @author 智云
 */
public interface ErpTaxInvoiceService {

    /**
     * 创建发票
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createTaxInvoice(@Valid ErpTaxInvoiceSaveReqVO createReqVO);

    /**
     * 更新发票
     *
     * @param updateReqVO 更新信息
     */
    void updateTaxInvoice(@Valid ErpTaxInvoiceSaveReqVO updateReqVO);

    /**
     * 删除发票
     *
     * @param id 编号
     */
    void deleteTaxInvoice(Long id);

    /**
     * 获得发票
     *
     * @param id 编号
     * @return 发票
     */
    ErpTaxInvoiceDO getTaxInvoice(Long id);

    /**
     * 获得发票分页
     *
     * @param pageReqVO 分页查询
     * @return 发票分页
     */
    PageResult<ErpTaxInvoiceDO> getTaxInvoicePage(ErpTaxInvoicePageReqVO pageReqVO);

    /**
     * 开具发票（草稿 → 已开具）：调用金税四期接口
     *
     * @param id 发票 ID
     */
    void issueInvoice(Long id);

    /**
     * 作废发票（已开具 → 已作废）：调用金税四期接口
     *
     * @param id 发票 ID
     */
    void revokeInvoice(Long id);

    /**
     * 红冲发票（已开具 → 已红冲）：调用金税四期接口
     *
     * @param id 发票 ID
     */
    void redInvoice(Long id);

    /**
     * 获得期间内已开具的发票列表
     *
     * @param startDate 起始日期
     * @param endDate 结束日期
     * @param invoiceType 发票类型（可选）
     * @return 发票列表
     */
    List<ErpTaxInvoiceDO> getTaxInvoiceListByPeriod(LocalDate startDate, LocalDate endDate, Integer invoiceType);

}
