package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.bankaccount.ErpBankAccountPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.bankaccount.ErpBankAccountSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpBankAccountDO;
import jakarta.validation.Valid;

import java.math.BigDecimal;

/**
 * ERP 银行账户 Service 接口（P0-3 资金管理）
 *
 * @author 芋道源码
 */
public interface ErpBankAccountService {

    /**
     * 创建银行账户
     */
    Long createBankAccount(@Valid ErpBankAccountSaveReqVO createReqVO);

    /**
     * 更新银行账户
     */
    void updateBankAccount(@Valid ErpBankAccountSaveReqVO updateReqVO);

    /**
     * 删除银行账户
     */
    void deleteBankAccount(Long id);

    /**
     * 获得银行账户
     */
    ErpBankAccountDO getBankAccount(Long id);

    /**
     * 获得银行账户分页
     */
    PageResult<ErpBankAccountDO> getBankAccountPage(ErpBankAccountPageReqVO pageReqVO);

    /**
     * 校验银行账户存在且启用
     */
    ErpBankAccountDO validateBankAccount(Long id);

    /**
     * 获得银行账户余额
     */
    BigDecimal getBalance(Long id);

}