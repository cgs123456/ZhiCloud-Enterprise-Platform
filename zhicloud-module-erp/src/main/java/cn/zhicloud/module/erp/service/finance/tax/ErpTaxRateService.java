package cn.zhicloud.module.erp.service.finance.tax;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.erp.controller.admin.finance.vo.taxrate.ErpTaxRatePageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.taxrate.ErpTaxRateSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.tax.ErpTaxRateDO;
import jakarta.validation.Valid;

/**
 * ERP 税率 Service 接口
 *
 * @author 智云
 */
public interface ErpTaxRateService {

    /**
     * 创建税率
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createTaxRate(@Valid ErpTaxRateSaveReqVO createReqVO);

    /**
     * 更新税率
     *
     * @param updateReqVO 更新信息
     */
    void updateTaxRate(@Valid ErpTaxRateSaveReqVO updateReqVO);

    /**
     * 删除税率
     *
     * @param id 编号
     */
    void deleteTaxRate(Long id);

    /**
     * 获得税率
     *
     * @param id 编号
     * @return 税率
     */
    ErpTaxRateDO getTaxRate(Long id);

    /**
     * 获得税率分页
     *
     * @param pageReqVO 分页查询
     * @return 税率分页
     */
    PageResult<ErpTaxRateDO> getTaxRatePage(ErpTaxRatePageReqVO pageReqVO);

    /**
     * 获得默认税率
     *
     * @return 默认税率
     */
    ErpTaxRateDO getDefaultTaxRate();

}
