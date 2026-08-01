package cn.iocoder.yudao.module.erp.service.sale.credit;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.sale.credit.vo.ErpCreditLimitPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.credit.vo.ErpCreditLimitSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.credit.ErpCreditLimitDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * ERP 客户信用额度 Service 接口
 *
 * @author 芋道源码
 */
public interface ErpCreditLimitService {

    /**
     * 创建客户信用额度
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createCreditLimit(@Valid ErpCreditLimitSaveReqVO createReqVO);

    /**
     * 更新客户信用额度
     *
     * @param updateReqVO 更新信息
     */
    void updateCreditLimit(@Valid ErpCreditLimitSaveReqVO updateReqVO);

    /**
     * 删除客户信用额度
     *
     * @param ids 编号数组
     */
    void deleteCreditLimit(List<Long> ids);

    /**
     * 获得客户信用额度
     *
     * @param id 编号
     * @return 客户信用额度
     */
    ErpCreditLimitDO getCreditLimit(Long id);

    /**
     * 根据客户编号获得信用额度
     *
     * @param customerId 客户编号
     * @return 客户信用额度
     */
    ErpCreditLimitDO getCreditLimitByCustomerId(Long customerId);

    /**
     * 获得客户信用额度分页
     *
     * @param pageReqVO 分页查询
     * @return 客户信用额度分页
     */
    PageResult<ErpCreditLimitDO> getCreditLimitPage(ErpCreditLimitPageReqVO pageReqVO);

    /**
     * 校验客户信用额度存在
     *
     * @param id 编号
     * @return 客户信用额度
     */
    ErpCreditLimitDO validateCreditLimit(Long id);

}
