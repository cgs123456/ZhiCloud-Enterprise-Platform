package cn.zhicloud.module.tms.service.gps;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.tms.controller.admin.gps.vo.TmsGpsPositionSaveReqVO;
import cn.zhicloud.module.tms.dal.dataobject.gps.TmsGpsPositionDO;
import cn.zhicloud.module.tms.dal.dataobject.vehicle.TmsVehicleDO;
import cn.zhicloud.module.tms.dal.mysql.gps.TmsGpsPositionMapper;
import cn.zhicloud.module.tms.dal.mysql.vehicle.TmsVehicleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomLongId;
import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link TmsGpsServiceImpl} 的单元测试
 *
 * @author zhicloud
 */
@Import(TmsGpsServiceImpl.class)
public class TmsGpsServiceImplTest extends BaseDbUnitTest {

    @Resource
    private TmsGpsServiceImpl gpsService;

    @Resource
    private TmsGpsPositionMapper gpsPositionMapper;

    @Resource
    private TmsVehicleMapper vehicleMapper;

    @Test
    public void test_reportPosition_success() {
        // 准备数据：先插入车辆
        TmsVehicleDO vehicle = randomPojo(TmsVehicleDO.class);
        vehicleMapper.insert(vehicle);

        // 调用：上报 GPS
        TmsGpsPositionSaveReqVO saveReqVO = new TmsGpsPositionSaveReqVO();
        saveReqVO.setVehicleId(vehicle.getId());
        saveReqVO.setShipmentId(randomLongId());
        saveReqVO.setLongitude(new BigDecimal("116.4074"));
        saveReqVO.setLatitude(new BigDecimal("39.9042"));
        saveReqVO.setSpeed(new BigDecimal("65.5"));
        saveReqVO.setDirection(new BigDecimal("180"));
        saveReqVO.setLocationDesc("北京市朝阳区");

        Long id = gpsService.reportPosition(saveReqVO);

        // 断言
        TmsGpsPositionDO position = gpsPositionMapper.selectById(id);
        assertNotNull(position);
        assertEquals(vehicle.getId(), position.getVehicleId());
        assertNotNull(position.getReportTime()); // 自动填充
    }

    @Test
    public void test_reportPosition_vehicleNotExists() {
        TmsGpsPositionSaveReqVO saveReqVO = new TmsGpsPositionSaveReqVO();
        saveReqVO.setVehicleId(randomLongId());
        saveReqVO.setLongitude(new BigDecimal("116.4074"));
        saveReqVO.setLatitude(new BigDecimal("39.9042"));

        assertThrows(Exception.class, () -> gpsService.reportPosition(saveReqVO));
    }

    @Test
    public void test_getLatestPosition() {
        // 准备数据：同一车辆插入 3 条 GPS，时间递增
        TmsVehicleDO vehicle = randomPojo(TmsVehicleDO.class);
        vehicleMapper.insert(vehicle);

        for (int i = 0; i < 3; i++) {
            TmsGpsPositionDO pos = TmsGpsPositionDO.builder()
                    .vehicleId(vehicle.getId())
                    .longitude(new BigDecimal("116." + i))
                    .latitude(new BigDecimal("39." + i))
                    .reportTime(LocalDateTime.now().minusMinutes(3 - i))
                    .build();
            gpsPositionMapper.insert(pos);
        }

        // 调用
        TmsGpsPositionDO latest = gpsService.getLatestPosition(vehicle.getId());

        // 断言：返回最后一条（时间最大）
        assertNotNull(latest);
        assertEquals(0, new BigDecimal("116.2").compareTo(latest.getLongitude()));
    }

    @Test
    public void test_getShipmentTrack() {
        // 准备数据：同一运单插入 3 条 GPS
        Long shipmentId = randomLongId();
        TmsVehicleDO vehicle = randomPojo(TmsVehicleDO.class);
        vehicleMapper.insert(vehicle);

        for (int i = 0; i < 3; i++) {
            TmsGpsPositionDO pos = TmsGpsPositionDO.builder()
                    .vehicleId(vehicle.getId())
                    .shipmentId(shipmentId)
                    .longitude(new BigDecimal("116." + i))
                    .latitude(new BigDecimal("39." + i))
                    .reportTime(LocalDateTime.now().minusMinutes(3 - i))
                    .build();
            gpsPositionMapper.insert(pos);
        }

        // 调用
        List<TmsGpsPositionDO> track = gpsService.getShipmentTrack(shipmentId);

        // 断言：3 条，按时间正序
        assertEquals(3, track.size());
        assertTrue(track.get(0).getReportTime().isBefore(track.get(1).getReportTime()));
        assertTrue(track.get(1).getReportTime().isBefore(track.get(2).getReportTime()));
    }

    @Test
    public void test_getLatestPosition_noData() {
        // 调用：无数据的车辆
        TmsGpsPositionDO latest = gpsService.getLatestPosition(randomLongId());
        assertNull(latest);
    }

}
