package cn.zhicloud.module.erp.service;

import cn.zhicloud.module.erp.dal.dataobject.stock.ErpStockCheckDO;
import cn.zhicloud.module.erp.dal.dataobject.stock.ErpStockInDO;
import cn.zhicloud.module.erp.dal.dataobject.stock.ErpStockMoveDO;
import cn.zhicloud.module.erp.dal.dataobject.stock.ErpStockOutDO;
import cn.zhicloud.module.erp.dal.mysql.stock.ErpStockCheckMapper;
import cn.zhicloud.module.erp.dal.mysql.stock.ErpStockInMapper;
import cn.zhicloud.module.erp.dal.mysql.stock.ErpStockMoveMapper;
import cn.zhicloud.module.erp.dal.mysql.stock.ErpStockOutMapper;
import cn.zhicloud.module.erp.service.stock.ErpStockCheckServiceImpl;
import cn.zhicloud.module.erp.service.stock.ErpStockInServiceImpl;
import cn.zhicloud.module.erp.service.stock.ErpStockMoveServiceImpl;
import cn.zhicloud.module.erp.service.stock.ErpStockOutServiceImpl;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.List;
import java.util.stream.Stream;

import static cn.zhicloud.module.erp.service.AuditStatusTestSupport.AuditCase;

/**
 * 库存审核入口幂等与并发重复过账测试。
 */
class StockAuditStatusIdempotencyTest {

    private static final List<AuditCase> AUDIT_CASES = List.of(
            new AuditCase("stock in",
                    ErpStockInServiceImpl.class, ErpStockInMapper.class,
                    "stockInMapper", "updateStockInStatus",
                    ErpStockInDO.class, "stockRecordService"),
            new AuditCase("stock out",
                    ErpStockOutServiceImpl.class, ErpStockOutMapper.class,
                    "stockOutMapper", "updateStockOutStatus",
                    ErpStockOutDO.class, "stockRecordService"),
            new AuditCase("stock move",
                    ErpStockMoveServiceImpl.class, ErpStockMoveMapper.class,
                    "stockMoveMapper", "updateStockMoveStatus",
                    ErpStockMoveDO.class, "stockRecordService"),
            new AuditCase("stock check",
                    ErpStockCheckServiceImpl.class, ErpStockCheckMapper.class,
                    "stockCheckMapper", "updateStockCheckStatus",
                    ErpStockCheckDO.class, "stockRecordService")
    );

    @TestFactory
    Stream<DynamicTest> auditStatusContracts() {
        return AuditStatusTestSupport.tests(AUDIT_CASES);
    }
}
