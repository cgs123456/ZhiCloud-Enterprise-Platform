package cn.iocoder.yudao.module.tms.service.freight;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.tms.controller.admin.freight.vo.TmsFreightCalculateReqVO;
import cn.iocoder.yudao.module.tms.controller.admin.freight.vo.TmsFreightCalculateRespVO;
import cn.iocoder.yudao.module.tms.controller.admin.freight.vo.TmsFreightSaveReqVO;
import cn.iocoder.yudao.module.tms.dal.dataobject.freight.TmsFreightDO;
import cn.iocoder.yudao.module.tms.dal.dataobject.shipment.TmsShipmentDO;
import cn.iocoder.yudao.module.tms.dal.mysql.freight.TmsFreightMapper;
import cn.iocoder.yudao.module.tms.dal.mysql.shipment.TmsShipmentMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;
import java.math.BigDecimal;

import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link TmsFreightServiceImpl} 的单元测试
 *
 * @author yudao
 */
@Import(TmsFreightServiceImpl.class)
public class TmsFreightServiceImplTest extends BaseDbUnitTest {

    @Resource
    private TmsFreightServiceImpl freightService;

    @Resource
    private TmsFreightMapper freightMapper;

    @Resource
    private TmsShipmentMapper shipmentMapper;

    // ==================== 运费计算测试 ====================

    @Test
    public void test_calculateFreight_byWeight() {
        // 准备数据：运单含重量 500kg
        TmsShipmentDO shipment = randomPojo(TmsShipmentDO.class, o -> {
            o.setTotalWeight(new BigDecimal("500.0"));
            o.setTotalVolume(new BigDecimal("20.0"));
            o.setTotalQuantity(new BigDecimal("100"));
        });
        shipmentMapper.insert(shipment);

        // 调用：按重量计费，单价 3.00
        TmsFreightCalculateReqVO reqVO = new TmsFreightCalculateReqVO();
        reqVO.setShipmentId(shipment.getId());
        reqVO.setBillingMethod(10); // 按重量
        reqVO.setUnitPrice(new BigDecimal("3.00"));
        reqVO.setSurcharge(new BigDecimal("50.00"));
        reqVO.setDiscountAmount(new BigDecimal("20.00"));

        TmsFreightCalculateRespVO respVO = freightService.calculateFreight(reqVO);

        // 断言：500 * 3 = 1500 + 50 - 20 = 1530
        assertEquals(0, new BigDecimal("500.0").compareTo(respVO.getBillingQuantity()));
        assertEquals(new BigDecimal("3.00"), respVO.getUnitPrice());
        assertEquals(new BigDecimal("1500.00"), respVO.getBaseAmount());
        assertEquals(new BigDecimal("1530.00"), respVO.getTotalAmount());
    }

    @Test
    public void test_calculateFreight_byVolume() {
        // 准备数据：运单含体积 20m³
        TmsShipmentDO shipment = randomPojo(TmsShipmentDO.class, o -> {
            o.setTotalWeight(new BigDecimal("500.0"));
            o.setTotalVolume(new BigDecimal("20.0"));
            o.setTotalQuantity(new BigDecimal("100"));
        });
        shipmentMapper.insert(shipment);

        // 调用：按体积计费，单价 50.00
        TmsFreightCalculateReqVO reqVO = new TmsFreightCalculateReqVO();
        reqVO.setShipmentId(shipment.getId());
        reqVO.setBillingMethod(20); // 按体积
        reqVO.setUnitPrice(new BigDecimal("50.00"));

        TmsFreightCalculateRespVO respVO = freightService.calculateFreight(reqVO);

        // 断言：20 * 50 = 1000
        assertEquals(new BigDecimal("1000.00"), respVO.getTotalAmount());
    }

    @Test
    public void test_calculateFreight_flatRate() {
        // 准备数据
        TmsShipmentDO shipment = randomPojo(TmsShipmentDO.class);
        shipmentMapper.insert(shipment);

        // 调用：整车一口价，单价 2000
        TmsFreightCalculateReqVO reqVO = new TmsFreightCalculateReqVO();
        reqVO.setShipmentId(shipment.getId());
        reqVO.setBillingMethod(40); // 整车一口价
        reqVO.setUnitPrice(new BigDecimal("2000.00"));

        TmsFreightCalculateRespVO respVO = freightService.calculateFreight(reqVO);

        // 断言：1 * 2000 = 2000
        assertEquals(new BigDecimal("2000.00"), respVO.getTotalAmount());
    }

    @Test
    public void test_calculateFreight_shipmentNotExists() {
        TmsFreightCalculateReqVO reqVO = new TmsFreightCalculateReqVO();
        reqVO.setShipmentId(randomLongId());
        reqVO.setBillingMethod(10);
        reqVO.setUnitPrice(new BigDecimal("3.00"));

        assertThrows(Exception.class, () -> freightService.calculateFreight(reqVO));
    }

    @Test
    public void test_calculateFreight_discountGreaterThanBase() {
        // 准备数据
        TmsShipmentDO shipment = randomPojo(TmsShipmentDO.class, o -> {
            o.setTotalWeight(new BigDecimal("100.0"));
        });
        shipmentMapper.insert(shipment);

        // 调用：折扣 > 基础运费，总额不应为负
        TmsFreightCalculateReqVO reqVO = new TmsFreightCalculateReqVO();
        reqVO.setShipmentId(shipment.getId());
        reqVO.setBillingMethod(10);
        reqVO.setUnitPrice(new BigDecimal("1.00")); // 100 * 1 = 100
        reqVO.setDiscountAmount(new BigDecimal("200.00")); // 折扣 200

        TmsFreightCalculateRespVO respVO = freightService.calculateFreight(reqVO);

        // 断言：max(100 - 200, 0) = 0
        assertEquals(0, BigDecimal.ZERO.compareTo(respVO.getTotalAmount()));
    }

    // ==================== CRUD 测试 ====================

    @Test
    public void test_createFreight_defaultStatus() {
        TmsFreightSaveReqVO createReqVO = new TmsFreightSaveReqVO();
        createReqVO.setNo("FRT20240101001");
        createReqVO.setShipmentId(randomLongId());
        createReqVO.setBillingMethod(10);
        createReqVO.setTotalAmount(new BigDecimal("1530.00"));

        Long id = freightService.createFreight(createReqVO);

        TmsFreightDO freight = freightMapper.selectById(id);
        assertNotNull(freight);
        assertEquals(10, freight.getStatus()); // 默认待审核
    }

    @Test
    public void test_createFreight_duplicateNo() {
        // 先创建一条
        TmsFreightSaveReqVO createReqVO = new TmsFreightSaveReqVO();
        createReqVO.setNo("FRT_DUP_001");
        createReqVO.setShipmentId(randomLongId());
        createReqVO.setBillingMethod(10);
        createReqVO.setTotalAmount(new BigDecimal("1000.00"));
        freightService.createFreight(createReqVO);

        // 再用相同单号创建，应抛异常
        assertThrows(Exception.class, () -> freightService.createFreight(createReqVO));
    }

    @Test
    public void test_deleteFreight_notExists() {
        assertThrows(Exception.class, () -> freightService.deleteFreight(randomLongId()));
    }

}
