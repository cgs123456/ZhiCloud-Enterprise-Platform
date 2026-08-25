package cn.zhicloud.module.erp.service.finance.cashier;

import cn.zhicloud.module.erp.controller.admin.finance.cashier.vo.ErpBankStatementRespVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.cashier.ErpCashierDO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ERP 网银直联接口 Service
 *
 * <p>定义与银行网银系统的对接接口方法。当前为 stub 实现，仅返回占位结果，
 * 后续接入实际银行 API（如银企直联、第三方网银代理）时替换为真实实现即可。
 *
 * @author 智云
 */
public interface ErpBankDirectLinkService {

    /**
     * 发送支付指令到银行
     *
     * @param cashier 出纳单
     * @return 银行流水号
     */
    String sendPayment(ErpCashierDO cashier);

    /**
     * 查询支付状态
     *
     * @param bankSerialNo 银行流水号
     * @return 状态 10待处理/20已提交银行/30已到账/40已退回
     */
    Integer queryPaymentStatus(String bankSerialNo);

    /**
     * 接收银行对账单
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 银行对账单列表
     */
    List<ErpBankStatementRespVO> receiveBankStatement(LocalDateTime start, LocalDateTime end);

}
