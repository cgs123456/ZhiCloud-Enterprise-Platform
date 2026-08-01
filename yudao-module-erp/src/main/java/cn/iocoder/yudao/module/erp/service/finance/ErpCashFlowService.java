package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.cashflow.ErpCashFlowPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.cashflow.ErpCashFlowSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpCashFlowDO;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * ERP 现金流 Service 接口（P0-3 资金管理）
 *
 * @author 芋道源码
 */
public interface ErpCashFlowService {

    /**
     * 记录现金流（收款/付款时调用，同时更新银行账户余额）
     *
     * @param bizType       业务类型（10 收款 20 付款）
     * @param amount        金额
     * @param bankAccountId 银行账户编号
     * @param bizOrderId    业务单据编号
     * @param bizOrderType  业务单据类型
     * @param occurDate     发生日期
     * @param remark        备注
     * @return 现金流记录编号
     */
    Long recordCashFlow(Integer bizType, BigDecimal amount, Long bankAccountId,
                        Long bizOrderId, String bizOrderType, LocalDate occurDate, String remark);

    /**
     * 创建现金流记录（管理端）
     */
    Long createCashFlow(@Valid ErpCashFlowSaveReqVO createReqVO);

    /**
     * 删除现金流记录
     */
    void deleteCashFlow(Long id);

    /**
     * 获得现金流记录
     */
    ErpCashFlowDO getCashFlow(Long id);

    /**
     * 获得现金流分页
     */
    PageResult<ErpCashFlowDO> getCashFlowPage(ErpCashFlowPageReqVO pageReqVO);

    /**
     * 按期间统计现金流
     */
    List<ErpCashFlowDO> getCashFlowByPeriod(LocalDate startDate, LocalDate endDate);

}