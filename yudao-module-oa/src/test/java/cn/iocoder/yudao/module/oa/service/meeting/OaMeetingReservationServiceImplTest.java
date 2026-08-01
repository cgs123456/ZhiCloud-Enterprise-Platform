package cn.iocoder.yudao.module.oa.service.meeting;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.oa.controller.admin.meeting.vo.OaMeetingReservationSaveReqVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.meeting.OaMeetingReservationDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.meeting.OaMeetingRoomDO;
import cn.iocoder.yudao.module.oa.dal.mysql.meeting.OaMeetingReservationMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.meeting.OaMeetingRoomMapper;
import cn.iocoder.yudao.module.oa.service.meeting.OaMeetingRoomService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link OaMeetingReservationServiceImpl} 的单元测试
 *
 * @author 芋道源码
 */
@Import(OaMeetingReservationServiceImpl.class)
public class OaMeetingReservationServiceImplTest extends BaseDbUnitTest {

    @Resource
    private OaMeetingReservationServiceImpl reservationService;

    @Resource
    private OaMeetingReservationMapper reservationMapper;

    @Resource
    private OaMeetingRoomMapper meetingRoomMapper;

    @MockitoBean
    private OaMeetingRoomService meetingRoomService;

    @Test
    public void test_createReservation_success() {
        // mock 会议室
        OaMeetingRoomDO room = randomPojo(OaMeetingRoomDO.class, o -> {
            o.setStatus(10); // 会议室可用状态 ROOM_STATUS_AVAILABLE=10
        });
        meetingRoomMapper.insert(room);

        // 准备参数
        OaMeetingReservationSaveReqVO reqVO = new OaMeetingReservationSaveReqVO();
        reqVO.setTitle("项目周会");
        reqVO.setRoomId(room.getId());
        reqVO.setOrganizerUserId(1L);
        reqVO.setStartTime(LocalDateTime.of(2026, 8, 1, 14, 0));
        reqVO.setEndTime(LocalDateTime.of(2026, 8, 1, 15, 0));

        // 调用
        Long id = reservationService.createReservation(reqVO);

        // 校验
        OaMeetingReservationDO result = reservationMapper.selectById(id);
        assertNotNull(result);
        assertEquals("项目周会", result.getTitle());
    }

    @Test
    public void test_checkTimeConflict_noConflict() {
        // mock 会议室
        OaMeetingRoomDO room = randomPojo(OaMeetingRoomDO.class, o -> o.setStatus(1));
        meetingRoomMapper.insert(room);

        // 调用（无冲突）
        boolean conflict = reservationService.checkTimeConflict(
                room.getId(),
                LocalDateTime.of(2026, 8, 1, 10, 0),
                LocalDateTime.of(2026, 8, 1, 11, 0),
                null);

        assertFalse(conflict);
    }

    @Test
    public void test_checkTimeConflict_hasConflict() {
        // mock 会议室
        OaMeetingRoomDO room = randomPojo(OaMeetingRoomDO.class, o -> o.setStatus(1));
        meetingRoomMapper.insert(room);

        // mock 已有预约
        OaMeetingReservationDO exist = randomPojo(OaMeetingReservationDO.class, o -> {
            o.setRoomId(room.getId());
            o.setStartTime(LocalDateTime.of(2026, 8, 1, 14, 0));
            o.setEndTime(LocalDateTime.of(2026, 8, 1, 15, 0));
            o.setStatus(10); // 会议室可用状态 ROOM_STATUS_AVAILABLE=10
        });
        reservationMapper.insert(exist);

        // 调用（冲突时间段）
        boolean conflict = reservationService.checkTimeConflict(
                room.getId(),
                LocalDateTime.of(2026, 8, 1, 14, 30),
                LocalDateTime.of(2026, 8, 1, 16, 0),
                null);

        assertTrue(conflict);
    }

    @Test
    public void test_deleteReservation_success() {
        // mock 数据
        OaMeetingReservationDO reservation = randomPojo(OaMeetingReservationDO.class);
        reservationMapper.insert(reservation);

        // 调用
        reservationService.deleteReservation(reservation.getId());

        // 校验
        assertNull(reservationMapper.selectById(reservation.getId()));
    }

    @Test
    public void test_deleteReservation_notExists() {
        assertThrows(Exception.class, () -> reservationService.deleteReservation(randomLongId()));
    }

    @Test
    public void test_getReservation_success() {
        // mock 数据
        OaMeetingReservationDO reservation = randomPojo(OaMeetingReservationDO.class);
        reservationMapper.insert(reservation);

        // 调用并校验
        OaMeetingReservationDO result = reservationService.getReservation(reservation.getId());
        assertNotNull(result);
        assertEquals(reservation.getId(), result.getId());
    }

}
