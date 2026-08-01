package cn.iocoder.yudao.module.erp.service.finance.tax;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.taxrate.ErpTaxRatePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.taxrate.ErpTaxRateSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.tax.ErpTaxRateDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.tax.ErpTaxRateMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.TAX_RATE_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.TAX_RATE_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.TAX_RATE_RATE_INVALID;

/**
 * ERP 税率 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpTaxRateServiceImpl implements ErpTaxRateService {

    @Resource
    private ErpTaxRateMapper taxRateMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTaxRate(ErpTaxRateSaveReqVO createReqVO) {
        // 校验编码唯一
        validateCodeUnique(null, createReqVO.getCode());
        // 校验税率范围
        validateRate(createReqVO.getRate());
        // 插入
        ErpTaxRateDO taxRate = BeanUtils.toBean(createReqVO, ErpTaxRateDO.class);
        taxRateMapper.insert(taxRate);
        return taxRate.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTaxRate(ErpTaxRateSaveReqVO updateReqVO) {
        // 校验存在
        validateTaxRateExists(updateReqVO.getId());
        // 校验编码唯一
        validateCodeUnique(updateReqVO.getId(), updateReqVO.getCode());
        // 校验税率范围
        validateRate(updateReqVO.getRate());
        // 更新
        ErpTaxRateDO updateObj = BeanUtils.toBean(updateReqVO, ErpTaxRateDO.class);
        taxRateMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTaxRate(Long id) {
        // 校验存在
        validateTaxRateExists(id);
        // 删除
        taxRateMapper.deleteById(id);
    }

    @Override
    public ErpTaxRateDO getTaxRate(Long id) {
        return taxRateMapper.selectById(id);
    }

    @Override
    public PageResult<ErpTaxRateDO> getTaxRatePage(ErpTaxRatePageReqVO pageReqVO) {
        return taxRateMapper.selectPage(pageReqVO);
    }

    @Override
    public ErpTaxRateDO getDefaultTaxRate() {
        return taxRateMapper.selectDefault();
    }

    private void validateTaxRateExists(Long id) {
        if (taxRateMapper.selectById(id) == null) {
            throw exception(TAX_RATE_NOT_EXISTS);
        }
    }

    private void validateCodeUnique(Long id, String code) {
        ErpTaxRateDO existing = taxRateMapper.selectByCode(code);
        if (existing != null && !Objects.equals(existing.getId(), id)) {
            throw exception(TAX_RATE_CODE_DUPLICATE, code);
        }
    }

    private void validateRate(BigDecimal rate) {
        if (rate != null && (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.ONE) > 0)) {
            throw exception(TAX_RATE_RATE_INVALID);
        }
    }

}
