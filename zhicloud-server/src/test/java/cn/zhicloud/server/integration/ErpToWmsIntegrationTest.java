package cn.zhicloud.server.integration;

import cn.zhicloud.framework.datasource.config.ZhiCloudDataSourceAutoConfiguration;
import cn.zhicloud.framework.integration.api.EventAppendReq;
import cn.zhicloud.framework.integration.config.IntegrationAutoConfiguration;
import cn.zhicloud.framework.integration.contract.ErpSaleOutApprovedEvent;
import cn.zhicloud.framework.integration.contract.IntegrationEventTypes;
import cn.zhicloud.framework.integration.dal.dataobject.EventOutboxDO;
import cn.zhicloud.framework.integration.service.EventOutboxService;
import cn.zhicloud.framework.integration.service.EventRelayService;
import cn.zhicloud.framework.mdm.api.MdmCrossRefUpsertReq;
import cn.zhicloud.framework.mdm.config.MdmAutoConfiguration;
import cn.zhicloud.framework.mdm.enums.MdmConstants;
import cn.zhicloud.framework.mdm.service.MdmCrossRefService;
import cn.zhicloud.framework.mybatis.config.ZhiCloudMybatisAutoConfiguration;
import cn.zhicloud.module.erp.dal.dataobject.product.ErpProductDO;
import cn.zhicloud.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.zhicloud.module.erp.enums.ErpAuditStatus;
import cn.zhicloud.module.erp.service.accounting.ErpAccountingEventService;
import cn.zhicloud.module.erp.service.finance.ErpAccountService;
import cn.zhicloud.module.erp.service.product.ErpProductService;
import cn.zhicloud.module.erp.service.sale.ErpSaleOrderService;
import cn.zhicloud.module.erp.service.sale.ErpSaleOutService;
import cn.zhicloud.module.erp.service.sale.ErpSaleOutServiceImpl;
import cn.zhicloud.module.erp.service.stock.ErpStockRecordService;
import cn.zhicloud.module.system.api.user.AdminUserApi;
import cn.zhicloud.module.wms.dal.dataobject.md.item.WmsItemSkuDO;
import cn.zhicloud.module.wms.dal.dataobject.md.merchant.WmsMerchantDO;
import cn.zhicloud.module.wms.dal.dataobject.md.warehouse.WmsWarehouseDO;
import cn.zhicloud.module.wms.enums.order.WmsOrderStatusEnum;
import cn.zhicloud.module.wms.service.integration.ErpSaleOutApprovedConsumer;
import cn.zhicloud.module.wms.service.inventory.WmsInventoryService;
import cn.zhicloud.module.wms.service.md.item.WmsItemSkuService;
import cn.zhicloud.module.wms.service.md.merchant.WmsMerchantService;
import cn.zhicloud.module.wms.service.md.warehouse.WmsWarehouseService;
import cn.zhicloud.module.wms.service.order.shipment.WmsShipmentOrderDetailServiceImpl;
import cn.zhicloud.module.wms.service.order.shipment.WmsShipmentOrderServiceImpl;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.github.yulichang.autoconfigure.MybatisPlusJoinAutoConfiguration;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Gate 3：真实 MySQL 上的 ERP 销售出库审核 -> Outbox -> Relay -> Inbox -> WMS 出库单链路。
 *
 * <p>本测试刻意不加载测试 SQL 初始化，也不使用 @Transactional：
 * 它验证的是真实提交、真实唯一键、真实 MyBatis 映射和真实中继读取，而不是内存 H2 的近似行为。
 */
@EnabledIfSystemProperty(named = "zhicloud.mysql.integration", matches = "true",
        disabledReason = "真实 MySQL 集成测试需显式开启，避免默认构建依赖本机 53306 沙箱")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = ErpToWmsIntegrationTest.TestApplication.class,
        properties = {
                "spring.config.location=classpath:/application-integration-mysql.yaml",
                "spring.main.banner-mode=off",
                "spring.flyway.enabled=false",
                "spring.sql.init.mode=never"
        })
public class ErpToWmsIntegrationTest {

    private static final String EXPECTED_DATABASE = "zhicloud_sandbox_chain";
    private static final Long TENANT_ID = 0L;

    private static final Long ERP_PRODUCT_ID = 990000001L;
    private static final Long WMS_SKU_ID = 990000101L;
    private static final Long ERP_WAREHOUSE_ID = 990000201L;
    private static final Long WMS_WAREHOUSE_ID = 990000301L;
    private static final Long ERP_CUSTOMER_ID = 990000401L;
    private static final Long WMS_MERCHANT_ID = 990000501L;
    private static final Long ERP_SALE_OUT_ID = 990000901L;
    private static final BigDecimal QUANTITY = new BigDecimal("3.0000");
    private static final BigDecimal UNIT_PRICE = new BigDecimal("12.3400");
    private static final BigDecimal TOTAL_PRICE = new BigDecimal("37.0200");
    private static final BigDecimal PURCHASE_PRICE = new BigDecimal("8.0000");
    private static final LocalDateTime OUT_TIME = LocalDateTime.of(2026, 9, 26, 10, 0);

    @Import({
            ZhiCloudDataSourceAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            DruidDataSourceAutoConfigure.class,
            ZhiCloudMybatisAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class,
            MybatisPlusJoinAutoConfiguration.class,
            SpringUtil.class,
            IntegrationAutoConfiguration.class,
            MdmAutoConfiguration.class,
            ErpSaleOutServiceImpl.class,
            ErpSaleOutApprovedConsumer.class,
            WmsShipmentOrderServiceImpl.class,
            WmsShipmentOrderDetailServiceImpl.class
    })
    public static class TestApplication {
    }

    @Resource
    private DataSource dataSource;
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private ErpSaleOutService saleOutService;
    @Resource
    private EventOutboxService eventOutboxService;
    @Resource
    private EventRelayService eventRelayService;
    @Resource
    private MdmCrossRefService mdmCrossRefService;
    @Resource
    private ErpSaleOutApprovedConsumer consumer;

    @MockitoBean
    private ErpNoRedisDAO noRedisDAO;
    @MockitoBean
    private ErpProductService productService;
    @MockitoBean
    private ErpSaleOrderService saleOrderService;
    @MockitoBean
    private ErpAccountService accountService;
    @MockitoBean
    private ErpStockRecordService stockRecordService;
    @MockitoBean
    private ErpAccountingEventService accountingEventService;
    @MockitoBean
    private AdminUserApi adminUserApi;
    @MockitoBean
    private WmsWarehouseService warehouseService;
    @MockitoBean
    private WmsMerchantService merchantService;
    @MockitoBean
    private WmsInventoryService inventoryService;
    @MockitoBean
    private WmsItemSkuService itemSkuService;

    private String erpNo;
    private String duplicateEventId;

    @BeforeEach
    void setUp() throws Exception {
        assertSandboxDatabase();
        erpNo = "IT-ERP-" + UUID.randomUUID().toString().substring(0, 8);
        duplicateEventId = "it-dup-" + UUID.randomUUID().toString().substring(0, 8);
        cleanup();
        insertErpSaleOut();
        insertErpSaleOutItem();

        when(productService.getProduct(ERP_PRODUCT_ID)).thenReturn(ErpProductDO.builder()
                .id(ERP_PRODUCT_ID)
                .purchasePrice(PURCHASE_PRICE)
                .build());
        when(warehouseService.validateWarehouseExists(WMS_WAREHOUSE_ID))
                .thenReturn(WmsWarehouseDO.builder().id(WMS_WAREHOUSE_ID).build());
        when(merchantService.validateCustomerMerchantExists(WMS_MERCHANT_ID))
                .thenReturn(WmsMerchantDO.builder().id(WMS_MERCHANT_ID).build());
        when(itemSkuService.validateItemSkuExists(WMS_SKU_ID))
                .thenReturn(WmsItemSkuDO.builder().id(WMS_SKU_ID).build());
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    @Test
    void testApproveRelaysToWmsAndMdmMapping() throws Exception {
        insertAllMappings();

        saleOutService.updateSaleOutStatus(ERP_SALE_OUT_ID, ErpAuditStatus.APPROVE.getStatus());

        String eventId = ErpSaleOutApprovedEvent.eventIdOf(ERP_SALE_OUT_ID);
        assertEquals("NEW", outboxStatus(eventId));
        assertEquals(0L, count("SELECT COUNT(*) FROM wms_shipment_order WHERE biz_order_no = ?", erpNo));
        assertTrue(consumer.supports(IntegrationEventTypes.ERP_SALE_OUT_APPROVED));

        eventRelayService.relayOnce(10);

        assertEquals("PUBLISHED", outboxStatus(eventId));
        assertEquals("PROCESSED", inboxStatus(ErpSaleOutApprovedConsumer.NAME, eventId));
        assertWmsOrder();

        verify(warehouseService).validateWarehouseExists(WMS_WAREHOUSE_ID);
        verify(merchantService).validateCustomerMerchantExists(WMS_MERCHANT_ID);
        verify(itemSkuService).validateItemSkuExists(WMS_SKU_ID);
        verify(stockRecordService).createStockRecord(any());
        verify(accountingEventService).postEvent(any());

        // 第一层：已发布事件再次扫描不会重复投递。
        eventRelayService.relayOnce(10);
        assertWmsOrderCount(1L);

        // 第二层：换一个 eventId 指向同一 ERP 业务单，消费者仍必须按 bizOrderNo 拒绝重复建单。
        String payload = jdbcTemplate.queryForObject(
                "SELECT payload FROM event_outbox WHERE event_id = ?", String.class, eventId);
        consumer.handle(EventOutboxDO.builder()
                .eventId(duplicateEventId)
                .tenantId(TENANT_ID)
                .eventType(IntegrationEventTypes.ERP_SALE_OUT_APPROVED)
                .payload(payload)
                .build());
        assertWmsOrderCount(1L);
    }

    @Test
    void testMissingProductMappingLeavesOutboxRetryableAndInboxFailed() throws Exception {
        // 只登记仓库映射，故意不登记 ERP 产品 -> WMS SKU 映射。
        mdmCrossRefService.upsert(MdmCrossRefUpsertReq.builder()
                .tenantId(TENANT_ID)
                .sourceSystem(MdmConstants.SYSTEM_ERP)
                .sourceEntity(MdmConstants.ENTITY_WAREHOUSE)
                .sourceId(String.valueOf(ERP_WAREHOUSE_ID))
                .targetSystem(MdmConstants.SYSTEM_WMS)
                .targetEntity(MdmConstants.ENTITY_WAREHOUSE)
                .targetId(String.valueOf(WMS_WAREHOUSE_ID))
                .build());

        saleOutService.updateSaleOutStatus(ERP_SALE_OUT_ID, ErpAuditStatus.APPROVE.getStatus());
        String eventId = ErpSaleOutApprovedEvent.eventIdOf(ERP_SALE_OUT_ID);
        assertEquals("NEW", outboxStatus(eventId));

        eventRelayService.relayOnce(10);

        assertEquals("NEW", outboxStatus(eventId));
        assertEquals(1, retryCount(eventId));
        assertNotNull(nextRetryAt(eventId));
        assertEquals("FAILED", inboxStatus(ErpSaleOutApprovedConsumer.NAME, eventId));
        assertWmsOrderCount(0L);
    }

    private void insertErpSaleOut() {
        jdbcTemplate.update("""
                INSERT INTO erp_sale_out
                (id, no, customer_id, out_time, status, total_count, total_price, receipt_price,
                 total_product_price, total_tax_price, discount_price, other_price, deleted, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?)
                """,
                ERP_SALE_OUT_ID, erpNo, ERP_CUSTOMER_ID, Timestamp.valueOf(OUT_TIME),
                ErpAuditStatus.PROCESS.getStatus(), QUANTITY, TOTAL_PRICE, BigDecimal.ZERO,
                TOTAL_PRICE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, TENANT_ID);
    }

    private void insertErpSaleOutItem() {
        jdbcTemplate.update("""
                INSERT INTO erp_sale_out_items
                (out_id, product_id, warehouse_id, product_price, `count`, total_price, deleted, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, 0, ?)
                """,
                ERP_SALE_OUT_ID, ERP_PRODUCT_ID, ERP_WAREHOUSE_ID, UNIT_PRICE, QUANTITY, TOTAL_PRICE, TENANT_ID);
    }

    private void insertAllMappings() {
        mdmCrossRefService.upsert(MdmCrossRefUpsertReq.builder()
                .tenantId(TENANT_ID)
                .sourceSystem(MdmConstants.SYSTEM_ERP)
                .sourceEntity(MdmConstants.ENTITY_PRODUCT)
                .sourceId(String.valueOf(ERP_PRODUCT_ID))
                .targetSystem(MdmConstants.SYSTEM_WMS)
                .targetEntity(MdmConstants.ENTITY_SKU)
                .targetId(String.valueOf(WMS_SKU_ID))
                .build());
        mdmCrossRefService.upsert(MdmCrossRefUpsertReq.builder()
                .tenantId(TENANT_ID)
                .sourceSystem(MdmConstants.SYSTEM_ERP)
                .sourceEntity(MdmConstants.ENTITY_WAREHOUSE)
                .sourceId(String.valueOf(ERP_WAREHOUSE_ID))
                .targetSystem(MdmConstants.SYSTEM_WMS)
                .targetEntity(MdmConstants.ENTITY_WAREHOUSE)
                .targetId(String.valueOf(WMS_WAREHOUSE_ID))
                .build());
        mdmCrossRefService.upsert(MdmCrossRefUpsertReq.builder()
                .tenantId(TENANT_ID)
                .sourceSystem(MdmConstants.SYSTEM_ERP)
                .sourceEntity(MdmConstants.ENTITY_CUSTOMER)
                .sourceId(String.valueOf(ERP_CUSTOMER_ID))
                .targetSystem(MdmConstants.SYSTEM_WMS)
                .targetEntity(MdmConstants.ENTITY_MERCHANT)
                .targetId(String.valueOf(WMS_MERCHANT_ID))
                .build());
    }

    private void assertSandboxDatabase() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertEquals(EXPECTED_DATABASE, connection.getCatalog(),
                    "集成测试必须运行在专用沙箱库，禁止指向生产或开发库");
        }
    }

    private void assertWmsOrder() {
        Map<String, Object> order = jdbcTemplate.queryForMap(
                "SELECT no, biz_order_no, warehouse_id, merchant_id, status, total_quantity, total_price " +
                        "FROM wms_shipment_order WHERE biz_order_no = ?", erpNo);
        assertEquals("WS" + erpNo, order.get("no"));
        assertEquals(erpNo, order.get("biz_order_no"));
        assertEquals(WMS_WAREHOUSE_ID, ((Number) order.get("warehouse_id")).longValue());
        assertEquals(WMS_MERCHANT_ID, ((Number) order.get("merchant_id")).longValue());
        assertEquals(WmsOrderStatusEnum.PREPARE.getStatus(), ((Number) order.get("status")).intValue());
        assertDecimal("3.0000", order.get("total_quantity"));
        assertDecimal("37.0200", order.get("total_price"));

        List<Map<String, Object>> details = jdbcTemplate.queryForList(
                "SELECT d.sku_id, d.warehouse_id, d.quantity, d.price, d.total_price " +
                        "FROM wms_shipment_order_detail d JOIN wms_shipment_order o ON o.id = d.order_id " +
                        "WHERE o.biz_order_no = ?", erpNo);
        assertEquals(1, details.size());
        Map<String, Object> detail = details.get(0);
        assertEquals(WMS_SKU_ID, ((Number) detail.get("sku_id")).longValue());
        assertEquals(WMS_WAREHOUSE_ID, ((Number) detail.get("warehouse_id")).longValue());
        assertDecimal("3.0000", detail.get("quantity"));
        assertDecimal("12.3400", detail.get("price"));
        assertDecimal("37.0200", detail.get("total_price"));
    }

    private void assertWmsOrderCount(long expected) {
        assertEquals(expected, count("SELECT COUNT(*) FROM wms_shipment_order WHERE biz_order_no = ?", erpNo));
    }

    private String outboxStatus(String eventId) {
        return jdbcTemplate.queryForObject("SELECT status FROM event_outbox WHERE event_id = ?", String.class, eventId);
    }

    private int retryCount(String eventId) {
        Number value = jdbcTemplate.queryForObject(
                "SELECT retry_count FROM event_outbox WHERE event_id = ?", Number.class, eventId);
        return value == null ? 0 : value.intValue();
    }

    private Object nextRetryAt(String eventId) {
        return jdbcTemplate.queryForObject("SELECT next_retry_at FROM event_outbox WHERE event_id = ?", Object.class, eventId);
    }

    private String inboxStatus(String consumerName, String eventId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM event_inbox WHERE consumer_name = ? AND event_id = ?",
                String.class, consumerName, eventId);
    }

    private long count(String sql, Object... args) {
        Number value = jdbcTemplate.queryForObject(sql, Number.class, args);
        return value == null ? 0L : value.longValue();
    }

    private static void assertDecimal(String expected, Object actual) {
        assertNotNull(actual);
        assertEquals(0, new BigDecimal(expected).compareTo((BigDecimal) actual));
    }

    private void cleanup() {
        String wmsNo = erpNo == null ? null : "WS" + erpNo;
        if (erpNo != null) {
            jdbcTemplate.update("""
                    DELETE d FROM wms_shipment_order_detail d
                    JOIN wms_shipment_order o ON o.id = d.order_id
                    WHERE o.biz_order_no = ? OR o.no = ?
                    """, erpNo, wmsNo);
            jdbcTemplate.update("DELETE FROM wms_shipment_order WHERE biz_order_no = ? OR no = ?", erpNo, wmsNo);
        }
        String eventId = ErpSaleOutApprovedEvent.eventIdOf(ERP_SALE_OUT_ID);
        jdbcTemplate.update("DELETE FROM event_inbox WHERE event_id IN (?, ?)", eventId, duplicateEventId);
        jdbcTemplate.update("DELETE FROM event_outbox WHERE event_id IN (?, ?)", eventId, duplicateEventId);
        jdbcTemplate.update("DELETE FROM erp_sale_out_items WHERE out_id = ?", ERP_SALE_OUT_ID);
        jdbcTemplate.update("DELETE FROM erp_sale_out WHERE id = ? OR no = ?", ERP_SALE_OUT_ID, erpNo);
        jdbcTemplate.update("""
                DELETE FROM mdm_cross_ref
                WHERE tenant_id = 0 AND source_system = 'ERP' AND target_system = 'WMS'
                  AND source_id IN (?, ?, ?)
                  AND ((source_entity = 'PRODUCT' AND target_entity = 'SKU')
                    OR (source_entity = 'WAREHOUSE' AND target_entity = 'WAREHOUSE')
                    OR (source_entity = 'CUSTOMER' AND target_entity = 'MERCHANT'))
                """,
                String.valueOf(ERP_PRODUCT_ID), String.valueOf(ERP_WAREHOUSE_ID), String.valueOf(ERP_CUSTOMER_ID));
    }
}
