package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.bankaccount.ErpBankAccountPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.bankaccount.ErpBankAccountSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpBankAccountDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpBankAccountMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.BANK_ACCOUNT_NOT_ENABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.BANK_ACCOUNT_NOT_EXISTS;

/**
 * ERP 银行账户 Service 实现类（P0-3 资金管理）
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpBankAccountServiceImpl implements ErpBankAccountService {

    @Resource
    private ErpBankAccountMapper bankAccountMapper;

    @Override
    public Long createBankAccount(ErpBankAccountSaveReqVO createReqVO) {
        ErpBankAccountDO bankAccount = BeanUtils.toBean(createReqVO, ErpBankAccountDO.class);
        if (bankAccount.getBalance() == null) {
            bankAccount.setBalance(BigDecimal.ZERO);
        }
        bankAccountMapper.insert(bankAccount);
        return bankAccount.getId();
    }

    @Override
    public void updateBankAccount(ErpBankAccountSaveReqVO updateReqVO) {
        validateBankAccountExists(updateReqVO.getId());
        ErpBankAccountDO updateObj = BeanUtils.toBean(updateReqVO, ErpBankAccountDO.class);
        bankAccountMapper.updateById(updateObj);
    }

    @Override
    public void deleteBankAccount(Long id) {
        validateBankAccountExists(id);
        bankAccountMapper.deleteById(id);
    }

    private void validateBankAccountExists(Long id) {
        if (bankAccountMapper.selectById(id) == null) {
            throw exception(BANK_ACCOUNT_NOT_EXISTS);
        }
    }

    @Override
    public ErpBankAccountDO getBankAccount(Long id) {
        return bankAccountMapper.selectById(id);
    }

    @Override
    public PageResult<ErpBankAccountDO> getBankAccountPage(ErpBankAccountPageReqVO pageReqVO) {
        return bankAccountMapper.selectPage(pageReqVO);
    }

    @Override
    public ErpBankAccountDO validateBankAccount(Long id) {
        ErpBankAccountDO bankAccount = bankAccountMapper.selectById(id);
        if (bankAccount == null) {
            throw exception(BANK_ACCOUNT_NOT_EXISTS);
        }
        if (cn.iocoder.yudao.framework.common.enums.CommonStatusEnum.isDisable(bankAccount.getStatus())) {
            throw exception(BANK_ACCOUNT_NOT_ENABLE, bankAccount.getAccountName());
        }
        return bankAccount;
    }

    @Override
    public BigDecimal getBalance(Long id) {
        ErpBankAccountDO bankAccount = bankAccountMapper.selectById(id);
        return bankAccount == null ? BigDecimal.ZERO : (bankAccount.getBalance() == null ? BigDecimal.ZERO : bankAccount.getBalance());
    }

}