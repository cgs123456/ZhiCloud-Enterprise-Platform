package cn.zhicloud.module.tms.service.vehicle;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.tms.controller.admin.vehicle.vo.TmsFleetOperationPageReqVO;
import cn.zhicloud.module.tms.controller.admin.vehicle.vo.TmsFleetOperationSaveReqVO;
import cn.zhicloud.module.tms.dal.dataobject.vehicle.TmsFleetOperationDO;
import cn.zhicloud.module.tms.dal.mysql.vehicle.TmsFleetOperationMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomLongId;
import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link TmsFleetOperationServiceImpl} 的单元测试
 *
 * @author 智云
 */
@Import(TmsFleetOperationServiceImpl.class)
public class TmsFleetOperationServiceImplTest extends BaseDbUnitTest {

    @Resource
    private TmsFleetOperationServiceImpl fleetOperationService;

    @Resource
    private TmsFleetOperationMapper fleetOperationMapper;

    @Test
    public void test_createFleetOperation_success() {
        // 准备参数
        TmsFleetOperationSaveReqVO reqVO = new TmsFleetOperationSaveReqVO();
        reqVO.setVehicleId(1L);
        reqVO.setOperationDate(LocalDate.of(2026, 8, 1));
        reqVO.setMileage(new BigDecimal("350.5"));
        reqVO.setFuelCost(new BigDecimal("360.00"));
        reqVO.setMaintenanceCost(new BigDecimal("500.00"));
        reqVO.setRevenue(new BigDecimal("3000.00"));

        // 调用
        Long id = fleetOperationService.createFleetOperation(reqVO);

        // 校验
        TmsFleetOperationDO result = fleetOperationMapper.selectById(id);
        assertNotNull(result);
        assertEquals(0, result.getTotalCost().compareTo(new BigDecimal("860.00")));
        assertEquals(0, result.getProfit().compareTo(new BigDecimal("2140.00")));
    }

    @Test
    public void test_createFleetOperation_zeroRevenue() {
        // 准备参数（无收入，仅支出）
        TmsFleetOperationSaveReqVO reqVO = new TmsFleetOperationSaveReqVO();
        reqVO.setVehicleId(2L);
        reqVO.setOperationDate(LocalDate.of(2026, 8, 2));
        reqVO.setFuelCost(new BigDecimal("200.00"));
        reqVO.setMaintenanceCost(new BigDecimal("100.00"));

        // 调用
        Long id = fleetOperationService.createFleetOperation(reqVO);

        // 校验：总成本 300，利润 -300
        TmsFleetOperationDO result = fleetOperationMapper.selectById(id);
        assertEquals(0, result.getTotalCost().compareTo(new BigDecimal("300.00")));
        assertEquals(0, result.getProfit().compareTo(new BigDecimal("-300.00")));
    }

    @Test
    public void test_updateFleetOperation_success() {
        // mock 数据
        TmsFleetOperationDO operation = randomPojo(TmsFleetOperationDO.class, o -> {
            o.setVehicleId(1L);
            o.setFuelCost(new BigDecimal("100.00"));
        });
        fleetOperationMapper.insert(operation);

        // 准备参数
        TmsFleetOperationSaveReqVO reqVO = new TmsFleetOperationSaveReqVO();
        reqVO.setId(operation.getId());
        reqVO.setVehicleId(1L);
        reqVO.setOperationDate(LocalDate.of(2026, 8, 3));
        reqVO.setFuelCost(new BigDecimal("200.00"));
        reqVO.setRevenue(new BigDecimal("1000.00"));

        // 调用
        fleetOperationService.updateFleetOperation(reqVO);

        // 校验
        TmsFleetOperationDO result = fleetOperationMapper.selectById(operation.getId());
        assertEquals(0, result.getTotalCost().compareTo(new BigDecimal("200.00")));
        assertEquals(0, result.getProfit().compareTo(new BigDecimal("800.00")));
    }

    @Test
    public void test_deleteFleetOperation_success() {
        // mock 数据
        TmsFleetOperationDO operation = randomPojo(TmsFleetOperationDO.class);
        fleetOperationMapper.insert(operation);

        // 调用
        fleetOperationService.deleteFleetOperation(operation.getId());

        // 校验
        assertNull(fleetOperationMapper.selectById(operation.getId()));
    }

    @Test
    public void test_deleteFleetOperation_notExists() {
        assertThrows(Exception.class, () -> fleetOperationService.deleteFleetOperation(randomLongId()));
    }

    @Test
    public void test_getFleetOperation_success() {
        // mock 数据
        TmsFleetOperationDO operation = randomPojo(TmsFleetOperationDO.class);
        fleetOperationMapper.insert(operation);

        // 调用并校验
        TmsFleetOperationDO result = fleetOperationService.getFleetOperation(operation.getId());
        assertNotNull(result);
        assertEquals(operation.getId(), result.getId());
    }

}
