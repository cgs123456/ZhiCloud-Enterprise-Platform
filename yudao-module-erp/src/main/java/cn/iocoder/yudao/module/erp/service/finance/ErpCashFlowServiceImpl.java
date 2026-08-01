package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.cashflow.ErpCashFlowPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.cashflow.ErpCashFlowSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpBankAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpCashFlowDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpBankAccountMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpCashFlowMapper;
import cn.iocoder.yudao.module.erp.enums.finance.ErpCashFlowBizTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CASH_FLOW_NOT_EXISTS;

/**
 * ERP 现金流 Service 实现类（P0-3 资金管理）
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpCashFlowServiceImpl implements ErpCashFlowService {

    @Resource
    private ErpCashFlowMapper cashFlowMapper;
    @Resource
    private ErpBankAccountMapper bankAccountMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long recordCashFlow(Integer bizType, BigDecimal amount, Long bankAccountId,
                               Long bizOrderId, String bizOrderType, LocalDate occurDate, String remark) {
        // 1. 记录现金流
        ErpCashFlowDO cashFlow = ErpCashFlowDO.builder()
                .bizType(bizType).amount(amount).bankAccountId(bankAccountId)
                .bizOrderId(bizOrderId).bizOrderType(bizOrderType)
                .occurDate(occurDate).remark(remark)
                .build();
        cashFlowMapper.insert(cashFlow);
        // 2. 更新银行账户余额（收款 +，付款 -）
        if (bankAccountId != null && amount != null) {
            ErpBankAccountDO bankAccount = bankAccountMapper.selectById(bankAccountId);
            if (bankAccount != null) {
                BigDecimal balance = bankAccount.getBalance() == null ? BigDecimal.ZERO : bankAccount.getBalance();
                if (ErpCashFlowBizTypeEnum.RECEIPT.getType().equals(bizType)) {
                    balance = balance.add(amount);
                } else if (ErpCashFlowBizTypeEnum.PAYMENT.getType().equals(bizType)) {
                    balance = balance.subtract(amount);
                }
                ErpBankAccountDO update = new ErpBankAccountDO();
                update.setId(bankAccountId);
                update.setBalance(balance);
                bankAccountMapper.updateById(update);
            }
        }
        return cashFlow.getId();
    }

    @Override
    public Long createCashFlow(ErpCashFlowSaveReqVO createReqVO) {
        return recordCashFlow(createReqVO.getBizType(), createReqVO.getAmount(), createReqVO.getBankAccountId(),
                createReqVO.getBizOrderId(), createReqVO.getBizOrderType(),
                createReqVO.getOccurDate(), createReqVO.getRemark());
    }

    @Override
    public void deleteCashFlow(Long id) {
        if (cashFlowMapper.selectById(id) == null) {
            throw exception(CASH_FLOW_NOT_EXISTS);
        }
        cashFlowMapper.deleteById(id);
    }

    @Override
    public ErpCashFlowDO getCashFlow(Long id) {
        return cashFlowMapper.selectById(id);
    }

    @Override
    public PageResult<ErpCashFlowDO> getCashFlowPage(ErpCashFlowPageReqVO pageReqVO) {
        return cashFlowMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpCashFlowDO> getCashFlowByPeriod(LocalDate startDate, LocalDate endDate) {
        return cashFlowMapper.selectListByPeriod(startDate, endDate);
    }

}