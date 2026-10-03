package cn.zhicloud.module.erp.service;

import cn.zhicloud.module.erp.dal.dataobject.finance.ErpFinancePaymentDO;
import cn.zhicloud.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.zhicloud.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.zhicloud.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpFinancePaymentMapper;
import cn.zhicloud.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.zhicloud.module.erp.dal.mysql.purchase.ErpPurchaseOrderMapper;
import cn.zhicloud.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.zhicloud.module.erp.service.finance.ErpFinancePaymentServiceImpl;
import cn.zhicloud.module.erp.service.purchase.ErpPurchaseInServiceImpl;
import cn.zhicloud.module.erp.service.purchase.ErpPurchaseOrderServiceImpl;
import cn.zhicloud.module.erp.service.purchase.ErpPurchaseReturnServiceImpl;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.List;
import java.util.stream.Stream;

import static cn.zhicloud.module.erp.service.AuditStatusTestSupport.AuditCase;

/**
 * 采购到付款主链审核状态机测试。
 */
class PurchaseToPayStateMachineTest {

    private static final List<AuditCase> AUDIT_CASES = List.of(
            new AuditCase("purchase order",
                    ErpPurchaseOrderServiceImpl.class, ErpPurchaseOrderMapper.class,
                    "purchaseOrderMapper", "updatePurchaseOrderStatus",
                    ErpPurchaseOrderDO.class, null),
            new AuditCase("purchase in",
                    ErpPurchaseInServiceImpl.class, ErpPurchaseInMapper.class,
                    "purchaseInMapper", "updatePurchaseInStatus",
                    ErpPurchaseInDO.class, "stockRecordService"),
            new AuditCase("purchase return",
                    ErpPurchaseReturnServiceImpl.class, ErpPurchaseReturnMapper.class,
                    "purchaseReturnMapper", "updatePurchaseReturnStatus",
                    ErpPurchaseReturnDO.class, "stockRecordService"),
            new AuditCase("finance payment",
                    ErpFinancePaymentServiceImpl.class, ErpFinancePaymentMapper.class,
                    "financePaymentMapper", "updateFinancePaymentStatus",
                    ErpFinancePaymentDO.class, null)
    );

    @TestFactory
    Stream<DynamicTest> auditStatusContracts() {
        return AuditStatusTestSupport.tests(AUDIT_CASES);
    }
}
