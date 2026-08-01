package cn.iocoder.yudao.module.erp.service.finance.tax;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.taxinvoice.ErpTaxInvoicePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.taxinvoice.ErpTaxInvoiceSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.taxinvoiceline.ErpTaxInvoiceLineSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.tax.ErpTaxInvoiceDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.tax.ErpTaxInvoiceLineDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.tax.ErpTaxInvoiceLineMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.tax.ErpTaxInvoiceMapper;
import cn.iocoder.yudao.module.erp.enums.finance.tax.ErpInvoiceStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.INVOICE_AMOUNT_MISMATCH;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.INVOICE_NO_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.INVOICE_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.INVOICE_STATUS_INVALID;

/**
 * ERP 发票 Service 实现类
 *
 * <p>实现发票的状态机：草稿(DRAFT) → 已开具(ISSUED) → 已作废(REVOKED) / 已红冲(RED)
 *
 * @author 芋道源码
 */
@Slf4j
@Service
@Validated
public class ErpTaxInvoiceServiceImpl implements ErpTaxInvoiceService {

    @Resource
    private ErpTaxInvoiceMapper taxInvoiceMapper;
    @Resource
    private ErpTaxInvoiceLineMapper taxInvoiceLineMapper;
    @Resource
    private GoldenTaxApi goldenTaxApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTaxInvoice(ErpTaxInvoiceSaveReqVO createReqVO) {
        // 校验发票号+代码唯一
        validateInvoiceNoUnique(null, createReqVO.getInvoiceNo(), createReqVO.getInvoiceCode());
        // 插入主表
        ErpTaxInvoiceDO invoice = BeanUtils.toBean(createReqVO, ErpTaxInvoiceDO.class);
        if (invoice.getStatus() == null) {
            invoice.setStatus(ErpInvoiceStatusEnum.DRAFT.getStatus());
        }
        // 明细金额汇总到主表
        recalculateAmounts(invoice, createReqVO.getLines());
        taxInvoiceMapper.insert(invoice);
        // 插入明细
        if (createReqVO.getLines() != null) {
            int lineNo = 1;
            for (ErpTaxInvoiceLineDO line : BeanUtils.toBean(createReqVO.getLines(), ErpTaxInvoiceLineDO.class)) {
                line.setInvoiceId(invoice.getId());
                if (line.getLineNo() == null) {
                    line.setLineNo(lineNo++);
                }
                taxInvoiceLineMapper.insert(line);
            }
        }
        return invoice.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTaxInvoice(ErpTaxInvoiceSaveReqVO updateReqVO) {
        // 校验存在
        ErpTaxInvoiceDO existing = validateTaxInvoiceExists(updateReqVO.getId());
        // 校验状态：草稿才能修改
        if (!ErpInvoiceStatusEnum.DRAFT.getStatus().equals(existing.getStatus())) {
            throw exception(INVOICE_STATUS_INVALID);
        }
        // 校验发票号+代码唯一
        validateInvoiceNoUnique(updateReqVO.getId(), updateReqVO.getInvoiceNo(), updateReqVO.getInvoiceCode());
        // 更新主表
        ErpTaxInvoiceDO updateObj = BeanUtils.toBean(updateReqVO, ErpTaxInvoiceDO.class);
        recalculateAmounts(updateObj, updateReqVO.getLines());
        taxInvoiceMapper.updateById(updateObj);
        // 删除旧明细，插入新明细
        taxInvoiceLineMapper.deleteByInvoiceId(updateReqVO.getId());
        if (updateReqVO.getLines() != null) {
            int lineNo = 1;
            for (ErpTaxInvoiceLineDO line : BeanUtils.toBean(updateReqVO.getLines(), ErpTaxInvoiceLineDO.class)) {
                line.setInvoiceId(updateReqVO.getId());
                if (line.getLineNo() == null) {
                    line.setLineNo(lineNo++);
                }
                taxInvoiceLineMapper.insert(line);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTaxInvoice(Long id) {
        // 校验存在
        ErpTaxInvoiceDO existing = validateTaxInvoiceExists(id);
        // 校验状态：草稿才能删除
        if (!ErpInvoiceStatusEnum.DRAFT.getStatus().equals(existing.getStatus())) {
            throw exception(INVOICE_STATUS_INVALID);
        }
        // 删除主表和明细
        taxInvoiceMapper.deleteById(id);
        taxInvoiceLineMapper.deleteByInvoiceId(id);
    }

    @Override
    public ErpTaxInvoiceDO getTaxInvoice(Long id) {
        return taxInvoiceMapper.selectById(id);
    }

    @Override
    public PageResult<ErpTaxInvoiceDO> getTaxInvoicePage(ErpTaxInvoicePageReqVO pageReqVO) {
        return taxInvoiceMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void issueInvoice(Long id) {
        // 校验存在
        ErpTaxInvoiceDO invoice = validateTaxInvoiceExists(id);
        // 校验状态：草稿才能开具
        if (!ErpInvoiceStatusEnum.DRAFT.getStatus().equals(invoice.getStatus())) {
            throw exception(INVOICE_STATUS_INVALID);
        }
        // 调用金税四期接口
        goldenTaxApi.issueInvoice(invoice);
        // 更新状态为已开具
        invoice.setStatus(ErpInvoiceStatusEnum.ISSUED.getStatus());
        taxInvoiceMapper.updateById(invoice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeInvoice(Long id) {
        // 校验存在
        ErpTaxInvoiceDO invoice = validateTaxInvoiceExists(id);
        // 校验状态：已开具才能作废
        if (!ErpInvoiceStatusEnum.ISSUED.getStatus().equals(invoice.getStatus())) {
            throw exception(INVOICE_STATUS_INVALID);
        }
        // 调用金税四期接口
        goldenTaxApi.revokeInvoice(invoice.getInvoiceNo(), invoice.getInvoiceCode());
        // 更新状态为已作废
        invoice.setStatus(ErpInvoiceStatusEnum.REVOKED.getStatus());
        taxInvoiceMapper.updateById(invoice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void redInvoice(Long id) {
        // 校验存在
        ErpTaxInvoiceDO invoice = validateTaxInvoiceExists(id);
        // 校验状态：已开具才能红冲
        if (!ErpInvoiceStatusEnum.ISSUED.getStatus().equals(invoice.getStatus())) {
            throw exception(INVOICE_STATUS_INVALID);
        }
        // 调用金税四期接口
        goldenTaxApi.redInvoice(invoice.getInvoiceNo(), invoice.getInvoiceCode(), invoice.getAmountWithTax());
        // 更新状态为已红冲
        invoice.setStatus(ErpInvoiceStatusEnum.RED.getStatus());
        taxInvoiceMapper.updateById(invoice);
    }

    @Override
    public List<ErpTaxInvoiceDO> getTaxInvoiceListByPeriod(LocalDate startDate, LocalDate endDate, Integer invoiceType) {
        return taxInvoiceMapper.selectListByPeriod(startDate, endDate, invoiceType);
    }

    private ErpTaxInvoiceDO validateTaxInvoiceExists(Long id) {
        ErpTaxInvoiceDO invoice = taxInvoiceMapper.selectById(id);
        if (invoice == null) {
            throw exception(INVOICE_NOT_EXISTS);
        }
        return invoice;
    }

    private void validateInvoiceNoUnique(Long id, String invoiceNo, String invoiceCode) {
        ErpTaxInvoiceDO existing = taxInvoiceMapper.selectByInvoiceNoAndCode(invoiceNo, invoiceCode);
        if (existing != null && !Objects.equals(existing.getId(), id)) {
            throw exception(INVOICE_NO_DUPLICATE, invoiceNo, invoiceCode);
        }
    }

    /**
     * 根据发票明细汇总主表金额
     *
     * <p>不含税金额 = ∑明细.不含税金额
     * <p>税额 = ∑明细.税额
     * <p>价税合计 = ∑明细.价税合计
     * <p>当明细金额合计与主表金额不一致时抛出异常
     */
    private void recalculateAmounts(ErpTaxInvoiceDO invoice,
                                     List<ErpTaxInvoiceLineSaveReqVO> lines) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        BigDecimal sumWithoutTax = BigDecimal.ZERO;
        BigDecimal sumTax = BigDecimal.ZERO;
        BigDecimal sumWithTax = BigDecimal.ZERO;
        for (ErpTaxInvoiceLineSaveReqVO line : lines) {
            sumWithoutTax = sumWithoutTax.add(nullToZero(line.getAmountWithoutTax()));
            sumTax = sumTax.add(nullToZero(line.getTaxAmount()));
            sumWithTax = sumWithTax.add(nullToZero(line.getAmountWithTax()));
        }
        // 校验主表金额与明细合计一致
        if (invoice.getAmountWithoutTax() != null && invoice.getAmountWithoutTax().compareTo(sumWithoutTax) != 0) {
            throw exception(INVOICE_AMOUNT_MISMATCH, sumWithoutTax, sumTax, sumWithTax);
        }
        // 以明细合计为准
        invoice.setAmountWithoutTax(sumWithoutTax);
        invoice.setTaxAmount(sumTax);
        invoice.setAmountWithTax(sumWithTax);
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

}
