package cn.zhicloud.module.erp.service;

import cn.zhicloud.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import cn.zhicloud.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import cn.zhicloud.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.zhicloud.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpFinanceReceiptMapper;
import cn.zhicloud.module.erp.dal.mysql.sale.ErpSaleOrderMapper;
import cn.zhicloud.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.zhicloud.module.erp.dal.mysql.sale.ErpSaleReturnMapper;
import cn.zhicloud.module.erp.service.finance.ErpFinanceReceiptServiceImpl;
import cn.zhicloud.module.erp.service.sale.ErpSaleOrderServiceImpl;
import cn.zhicloud.module.erp.service.sale.ErpSaleOutServiceImpl;
import cn.zhicloud.module.erp.service.sale.ErpSaleReturnServiceImpl;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.List;
import java.util.stream.Stream;

import static cn.zhicloud.module.erp.service.AuditStatusTestSupport.AuditCase;

/**
 * 销售到收款主链审核状态机测试。
 */
class OrderToCashStateMachineTest {

    private static final List<AuditCase> AUDIT_CASES = List.of(
            new AuditCase("sale order",
                    ErpSaleOrderServiceImpl.class, ErpSaleOrderMapper.class,
                    "saleOrderMapper", "updateSaleOrderStatus",
                    ErpSaleOrderDO.class, null),
            new AuditCase("sale out",
                    ErpSaleOutServiceImpl.class, ErpSaleOutMapper.class,
                    "saleOutMapper", "updateSaleOutStatus",
                    ErpSaleOutDO.class, "stockRecordService"),
            new AuditCase("sale return",
                    ErpSaleReturnServiceImpl.class, ErpSaleReturnMapper.class,
                    "saleReturnMapper", "updateSaleReturnStatus",
                    ErpSaleReturnDO.class, "stockRecordService"),
            new AuditCase("finance receipt",
                    ErpFinanceReceiptServiceImpl.class, ErpFinanceReceiptMapper.class,
                    "financeReceiptMapper", "updateFinanceReceiptStatus",
                    ErpFinanceReceiptDO.class, null)
    );

    @TestFactory
    Stream<DynamicTest> auditStatusContracts() {
        return AuditStatusTestSupport.tests(AUDIT_CASES);
    }
}
