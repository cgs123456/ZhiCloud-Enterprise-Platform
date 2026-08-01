package cn.iocoder.yudao.module.erp.service.finance.tax;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.taxinvoiceline.ErpTaxInvoiceLinePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.taxinvoiceline.ErpTaxInvoiceLineSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.tax.ErpTaxInvoiceLineDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.tax.ErpTaxInvoiceLineMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.INVOICE_LINE_NOT_EXISTS;

/**
 * ERP 发票明细 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpTaxInvoiceLineServiceImpl implements ErpTaxInvoiceLineService {

    @Resource
    private ErpTaxInvoiceLineMapper taxInvoiceLineMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTaxInvoiceLine(ErpTaxInvoiceLineSaveReqVO createReqVO) {
        ErpTaxInvoiceLineDO taxInvoiceLine = BeanUtils.toBean(createReqVO, ErpTaxInvoiceLineDO.class);
        taxInvoiceLineMapper.insert(taxInvoiceLine);
        return taxInvoiceLine.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTaxInvoiceLine(ErpTaxInvoiceLineSaveReqVO updateReqVO) {
        // 校验存在
        validateTaxInvoiceLineExists(updateReqVO.getId());
        // 更新
        ErpTaxInvoiceLineDO updateObj = BeanUtils.toBean(updateReqVO, ErpTaxInvoiceLineDO.class);
        taxInvoiceLineMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTaxInvoiceLine(Long id) {
        // 校验存在
        validateTaxInvoiceLineExists(id);
        // 删除
        taxInvoiceLineMapper.deleteById(id);
    }

    @Override
    public ErpTaxInvoiceLineDO getTaxInvoiceLine(Long id) {
        return taxInvoiceLineMapper.selectById(id);
    }

    @Override
    public PageResult<ErpTaxInvoiceLineDO> getTaxInvoiceLinePage(ErpTaxInvoiceLinePageReqVO pageReqVO) {
        return taxInvoiceLineMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpTaxInvoiceLineDO> getTaxInvoiceLineListByInvoiceId(Long invoiceId) {
        return taxInvoiceLineMapper.selectListByInvoiceId(invoiceId);
    }

    private void validateTaxInvoiceLineExists(Long id) {
        if (taxInvoiceLineMapper.selectById(id) == null) {
            throw exception(INVOICE_LINE_NOT_EXISTS);
        }
    }

}
