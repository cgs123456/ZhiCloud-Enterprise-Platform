package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.erp.controller.admin.finance.vo.currency.ErpCurrencyPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.currency.ErpCurrencySaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpCurrencyDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * ERP 币种 Service 接口
 *
 * @author 智云
 */
public interface ErpCurrencyService {

    /**
     * 创建币种
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createCurrency(@Valid ErpCurrencySaveReqVO createReqVO);

    /**
     * 更新币种
     *
     * @param updateReqVO 更新信息
     */
    void updateCurrency(@Valid ErpCurrencySaveReqVO updateReqVO);

    /**
     * 删除币种
     *
     * @param id 编号
     */
    void deleteCurrency(Long id);

    /**
     * 获得币种
     *
     * @param id 编号
     * @return 币种
     */
    ErpCurrencyDO getCurrency(Long id);

    /**
     * 获得币种分页
     *
     * @param pageReqVO 分页查询
     * @return 币种分页
     */
    PageResult<ErpCurrencyDO> getCurrencyPage(ErpCurrencyPageReqVO pageReqVO);

    /**
     * 获得启用的币种列表（前端下拉用）
     *
     * @return 币种列表
     */
    List<ErpCurrencyDO> getEnabledCurrencyList();

}
