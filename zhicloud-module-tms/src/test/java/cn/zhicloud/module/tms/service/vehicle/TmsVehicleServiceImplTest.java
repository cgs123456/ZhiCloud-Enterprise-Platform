package cn.zhicloud.module.tms.service.vehicle;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.tms.controller.admin.vehicle.vo.TmsVehicleSaveReqVO;
import cn.zhicloud.module.tms.dal.dataobject.vehicle.TmsVehicleDO;
import cn.zhicloud.module.tms.dal.mysql.vehicle.TmsVehicleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link TmsVehicleServiceImpl} 的版本/状态机不变量测试。
 *
 * <p>为什么用真 H2 + 真 mapper 而不是 Mockito：§12.231 加 {@code @Version} 之后，
 * 用 mock 写「断言 version 传的是 3」是循环断言（mock 返回什么就断言什么），
 * 证明不了 DB 真的会因版本不符而拒绝。这里用真表，让「并发编辑输家被拒」成为可执行的事实。</p>
 *
 * <p>§12.233：同时守住 SaveReqVO 不再暴露 status——否则调用方能直接把「运输中」的车
 * 改回「可用」，绕过 dispatch 的可用性校验。</p>
 */
@Import(TmsVehicleServiceImpl.class)
public class TmsVehicleServiceImplTest extends BaseDbUnitTest {

    @Resource
    private TmsVehicleServiceImpl vehicleService;

    @Resource
    private TmsVehicleMapper vehicleMapper;

    private TmsVehicleSaveReqVO req(String plateNo) {
        TmsVehicleSaveReqVO vo = new TmsVehicleSaveReqVO();
        vo.setPlateNo(plateNo);
        vo.setVehicleType("厢式货车");
        vo.setLoadCapacity(new BigDecimal("5.0"));
        vo.setVolume(new BigDecimal("20.0"));
        return vo;
    }

    /**
     * 乐观锁：拿旧 version 去更新必须被拒绝，而不是静默覆盖。
     * 这是「版本机制不是装饰」的直接证据。
     *
     * <p>注：SaveReqVO 刻意不暴露 version——版本是实现细节不是业务契约。
     * 所以这里直接操纵 mapper 来构造「基于旧版本的并发更新」这一失败场景：
     * 两个请求都读到 version=0，其中一个先提交（0→1），另一个再拿 0 去写就必须被拒。</p>
     */
    @Test
    public void test_updateVehicle_staleVersionRejected() {
        Long id = vehicleService.createVehicle(req("京A-OPT-1"));
        TmsVehicleDO snapshot = vehicleMapper.selectById(id);

        // 请求 A：用同一份快照先提交，version 0 -> 1
        TmsVehicleSaveReqVO first = req("京A-OPT-1");
        first.setId(id);
        first.setRemark("第一次编辑");
        vehicleService.updateVehicle(first);
        assertEquals(Long.valueOf(1L), vehicleMapper.selectById(id).getVersion());

        // 请求 B：仍持着 version=0 的快照，晚一步提交
        TmsVehicleDO stale = new TmsVehicleDO();
        stale.setId(id);
        stale.setRemark("基于旧版本的并发编辑");
        stale.setVersion(snapshot.getVersion()); // 0

        assertEquals(0, vehicleMapper.updateById(stale),
                "拿旧 version 更新必须影响 0 行——否则乐观锁没接上写入路径");
        // 被拒后备注必须还是第一次编辑的值：失败方不得留下任何写入
        TmsVehicleDO after = vehicleMapper.selectById(id);
        assertEquals("第一次编辑", after.getRemark());
        assertEquals(1L, after.getVersion().longValue());
    }

    /**
     * SaveReqVO 不得暴露 status。veyicle 的状态是 dispatch 的产物，
     * 表单能改状态就能把「运输中」改回「可用」从而重复派车。
     */
    @Test
    public void testSaveReqVoDoesNotExposeStatus() {
        var fields = java.util.Arrays.stream(TmsVehicleSaveReqVO.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName).toList();
        assertFalse(fields.contains("status"),
                "TmsVehicleSaveReqVO 暴露了 status：可把运输中的车改回可用，绕过 dispatch 可用性校验");
    }

    /**
     * 编辑改不到状态列：即使 DB 里那一行已是「运输中」，updateVehicle 之后仍是运输。
     */
    @Test
    public void test_updateVehicle_cannotChangeStatus() {
        Long id = vehicleService.createVehicle(req("京A-OPT-2"));
        TmsVehicleDO row = vehicleMapper.selectById(id);
        row.setStatus(20); // 模拟已被 dispatch 置为运输中
        vehicleMapper.updateById(row);

        TmsVehicleSaveReqVO vo = req("京A-OPT-2");
        vo.setId(id);
        vo.setRemark("普通编辑");
        vehicleService.updateVehicle(vo);

        assertEquals(20L, vehicleMapper.selectById(id).getStatus().longValue(),
                "编辑车辆不得改写状态列，否则运输中的车会被改成可用而被重复派车");
    }
}
