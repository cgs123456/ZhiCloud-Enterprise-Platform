package cn.iocoder.yudao.module.erp.service.finance.cashier;

import cn.iocoder.yudao.module.erp.controller.admin.finance.cashier.vo.ErpBankStatementRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.cashier.ErpCashierDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/**
 * ERP 网银直联接口 Service 实现类（Stub）
 *
 * <p>当前为桩实现，未实际对接银行 API。所有方法返回占位结果。
 * 接入真实银行 API 时，替换本实现类中的方法逻辑即可。
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class ErpBankDirectLinkServiceImpl implements ErpBankDirectLinkService {

    /**
     * 银行流水号状态：30 已到账（stub 默认返回已到账）
     */
    private static final int STUB_STATUS_ARRIVED = 30;

    @Override
    public String sendPayment(ErpCashierDO cashier) {
        // Stub 实现：返回 "BANK" + 时间戳
        String bankSerialNo = "BANK" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        log.info("[sendPayment][stub] cashierNo({}) amount({}) bankSerialNo({})",
                cashier.getNo(), cashier.getAmount(), bankSerialNo);
        return bankSerialNo;
    }

    @Override
    public Integer queryPaymentStatus(String bankSerialNo) {
        // Stub 实现：始终返回已到账
        log.info("[queryPaymentStatus][stub] bankSerialNo({}) -> status(30 已到账)", bankSerialNo);
        return STUB_STATUS_ARRIVED;
    }

    @Override
    public List<ErpBankStatementRespVO> receiveBankStatement(LocalDateTime start, LocalDateTime end) {
        // Stub 实现：返回空列表
        log.info("[receiveBankStatement][stub] start({}) end({}) -> empty list", start, end);
        return Collections.emptyList();
    }

}
