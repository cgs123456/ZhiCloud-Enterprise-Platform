package cn.zhicloud.framework.mdm.service;

import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.framework.datasource.config.ZhiCloudDataSourceAutoConfiguration;
import cn.zhicloud.framework.mdm.api.MdmCrossRefUpsertReq;
import cn.zhicloud.framework.mdm.config.MdmAutoConfiguration;
import cn.zhicloud.framework.mdm.dal.mysql.MdmCrossRefMapper;
import cn.zhicloud.framework.mybatis.config.ZhiCloudMybatisAutoConfiguration;
import cn.zhicloud.framework.test.config.SqlInitializationTestConfiguration;
import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.github.yulichang.autoconfigure.MybatisPlusJoinAutoConfiguration;
import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static cn.zhicloud.framework.mdm.enums.ErrorCodeConstants.MDM_CROSS_REF_ARG_INVALID;
import static cn.zhicloud.framework.mdm.enums.ErrorCodeConstants.MDM_CROSS_REF_NOT_FOUND;
import static cn.zhicloud.framework.mdm.enums.ErrorCodeConstants.MDM_CROSS_REF_TARGET_ID_INVALID;
import static cn.zhicloud.framework.mdm.enums.ErrorCodeConstants.MDM_CROSS_REF_TENANT_MISMATCH;
import static cn.zhicloud.framework.mdm.enums.ErrorCodeConstants.MDM_CROSS_REF_TENANT_REQUIRED;
import static cn.zhicloud.framework.mdm.enums.MdmConstants.ENTITY_PRODUCT;
import static cn.zhicloud.framework.mdm.enums.MdmConstants.ENTITY_SKU;
import static cn.zhicloud.framework.mdm.enums.MdmConstants.ENTITY_WAREHOUSE;
import static cn.zhicloud.framework.mdm.enums.MdmConstants.SYSTEM_ERP;
import static cn.zhicloud.framework.mdm.enums.MdmConstants.SYSTEM_WMS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 主数据跨域对照测试（V101）：upsert 幂等、解析命中、映射缺失必须抛错、租户隔离
 *
 * @author 智云集成治理
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = MdmCrossRefServiceTest.TestApplication.class)
@ActiveProfiles("unit-test")
@Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class MdmCrossRefServiceTest {

    @Import({
            ZhiCloudDataSourceAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            DruidDataSourceAutoConfigure.class,
            SqlInitializationTestConfiguration.class,
            ZhiCloudMybatisAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class,
            MybatisPlusJoinAutoConfiguration.class,
            MdmAutoConfiguration.class
    })
    public static class TestApplication {
    }

    @Resource
    private MdmCrossRefService mdmCrossRefService;
    @Resource
    private MdmCrossRefMapper mdmCrossRefMapper;

    @BeforeEach
    public void setUpTenantContext() {
        TenantContextHolder.setTenantId(0L);
    }

    @AfterEach
    public void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    public void testUpsertWithoutTenantContextFailsClosed() {
        TenantContextHolder.clear();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> mdmCrossRefService.upsert(productRef("100", "9001")));
        assertEquals(MDM_CROSS_REF_TENANT_REQUIRED.getCode(), ex.getCode());
    }

    @Test
    public void testUpsertRejectsTenantMismatch() {
        TenantContextHolder.setTenantId(7L);
        MdmCrossRefUpsertReq req = MdmCrossRefUpsertReq.builder()
                .tenantId(8L)
                .sourceSystem(SYSTEM_ERP).sourceEntity(ENTITY_PRODUCT).sourceId("100")
                .targetSystem(SYSTEM_WMS).targetEntity(ENTITY_SKU).targetId("9001")
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> mdmCrossRefService.upsert(req));
        assertEquals(MDM_CROSS_REF_TENANT_MISMATCH.getCode(), ex.getCode());
    }

    @Test
    public void testResolveRejectsTenantMismatch() {
        TenantContextHolder.setTenantId(7L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> mdmCrossRefService.mapTargetId(8L, SYSTEM_ERP, ENTITY_PRODUCT, "100",
                        SYSTEM_WMS, ENTITY_SKU));
        assertEquals(MDM_CROSS_REF_TENANT_MISMATCH.getCode(), ex.getCode());
    }

    @Test
    public void testUpsertThenMap() {
        Long id = mdmCrossRefService.upsert(productRef("100", "9001"));
        assertNotNull(id);

        assertEquals("9001", mdmCrossRefService.mapTargetId(0L, SYSTEM_ERP, ENTITY_PRODUCT, "100",
                SYSTEM_WMS, ENTITY_SKU));
        assertEquals(9001L, mdmCrossRefService.mapTargetIdAsLong(0L, SYSTEM_ERP, ENTITY_PRODUCT, "100",
                SYSTEM_WMS, ENTITY_SKU));
        assertEquals("9001", mdmCrossRefService.findTargetId(0L, SYSTEM_ERP, ENTITY_PRODUCT, "100",
                SYSTEM_WMS, ENTITY_SKU));
    }

    @Test
    public void testUpsertTwiceIsIdempotentAndKeepsLatestTarget() {
        Long firstId = mdmCrossRefService.upsert(productRef("100", "9001"));
        Long secondId = mdmCrossRefService.upsert(productRef("100", "9002"));

        assertEquals(firstId, secondId, "同一源键重复登记必须更新同一行，而不是新增第二行");
        assertEquals(1L, mdmCrossRefMapper.selectCount(null));
        assertEquals("9002", mdmCrossRefService.mapTargetId(0L, SYSTEM_ERP, ENTITY_PRODUCT, "100",
                SYSTEM_WMS, ENTITY_SKU));
    }

    @Test
    public void testMapMissingThrows() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> mdmCrossRefService.mapTargetId(0L, SYSTEM_ERP, ENTITY_PRODUCT, "999", SYSTEM_WMS, ENTITY_SKU));
        assertEquals(MDM_CROSS_REF_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    public void testFindMissingReturnsNull() {
        assertNull(mdmCrossRefService.findTargetId(0L, SYSTEM_ERP, ENTITY_PRODUCT, "999", SYSTEM_WMS, ENTITY_SKU));
    }

    @Test
    public void testTenantIsolation() {
        mdmCrossRefService.upsert(productRef("100", "9001"));

        assertEquals("9001", mdmCrossRefService.mapTargetId(0L, SYSTEM_ERP, ENTITY_PRODUCT, "100",
                SYSTEM_WMS, ENTITY_SKU));
        TenantContextHolder.setTenantId(1L);
        assertThrows(ServiceException.class,
                () -> mdmCrossRefService.mapTargetId(1L, SYSTEM_ERP, ENTITY_PRODUCT, "100", SYSTEM_WMS, ENTITY_SKU),
                "A 租户的映射不能被 B 租户解析到");
        assertNull(mdmCrossRefService.findTargetId(1L, SYSTEM_ERP, ENTITY_PRODUCT, "100", SYSTEM_WMS, ENTITY_SKU));
    }

    @Test
    public void testInvalidateStopsResolution() {
        mdmCrossRefService.upsert(productRef("100", "9001"));

        assertTrue(mdmCrossRefService.invalidate(0L, SYSTEM_ERP, ENTITY_PRODUCT, "100", SYSTEM_WMS, ENTITY_SKU));
        assertThrows(ServiceException.class,
                () -> mdmCrossRefService.mapTargetId(0L, SYSTEM_ERP, ENTITY_PRODUCT, "100", SYSTEM_WMS, ENTITY_SKU),
                "失效映射必须等同于缺失，不能继续解析");
    }

    @Test
    public void testUpsertWithBlankArgsThrows() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> mdmCrossRefService.upsert(productRef("", "9001")));
        assertEquals(MDM_CROSS_REF_ARG_INVALID.getCode(), ex.getCode());
    }

    @Test
    public void testNonNumericTargetIdThrows() {
        mdmCrossRefService.upsert(productRef("100", "SKU-A"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> mdmCrossRefService.mapTargetIdAsLong(0L, SYSTEM_ERP, ENTITY_PRODUCT, "100", SYSTEM_WMS, ENTITY_SKU));
        assertEquals(MDM_CROSS_REF_TARGET_ID_INVALID.getCode(), ex.getCode());
    }

    @Test
    public void testWarehouseMappingIsSeparateFromProductMapping() {
        mdmCrossRefService.upsert(productRef("100", "9001"));

        // 同为 sourceId=100，但实体类型不同，不能互相命中
        assertThrows(ServiceException.class,
                () -> mdmCrossRefService.mapTargetId(0L, SYSTEM_ERP, ENTITY_WAREHOUSE, "100", SYSTEM_WMS, ENTITY_WAREHOUSE));

        mdmCrossRefService.upsert(MdmCrossRefUpsertReq.builder()
                .tenantId(0L)
                .sourceSystem(SYSTEM_ERP).sourceEntity(ENTITY_WAREHOUSE).sourceId("100")
                .targetSystem(SYSTEM_WMS).targetEntity(ENTITY_WAREHOUSE).targetId("77")
                .remark("模拟数据：ERP 仓库 100 -> WMS 仓库 77")
                .build());
        assertEquals(77L, mdmCrossRefService.mapTargetIdAsLong(0L, SYSTEM_ERP, ENTITY_WAREHOUSE, "100",
                SYSTEM_WMS, ENTITY_WAREHOUSE));
    }

    private static MdmCrossRefUpsertReq productRef(String erpProductId, String wmsSkuId) {
        return MdmCrossRefUpsertReq.builder()
                .tenantId(0L)
                .sourceSystem(SYSTEM_ERP).sourceEntity(ENTITY_PRODUCT).sourceId(erpProductId)
                .targetSystem(SYSTEM_WMS).targetEntity(ENTITY_SKU).targetId(wmsSkuId)
                .remark("模拟数据：ERP 产品 -> WMS SKU")
                .build();
    }

}