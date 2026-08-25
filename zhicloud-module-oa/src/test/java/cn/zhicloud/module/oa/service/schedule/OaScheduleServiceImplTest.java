package cn.zhicloud.module.oa.service.schedule;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.oa.controller.admin.schedule.vo.OaSchedulePageReqVO;
import cn.zhicloud.module.oa.controller.admin.schedule.vo.OaScheduleSaveReqVO;
import cn.zhicloud.module.oa.dal.dataobject.schedule.OaScheduleDO;
import cn.zhicloud.module.oa.dal.mysql.schedule.OaScheduleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomLongId;
import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link OaScheduleServiceImpl} 的单元测试
 *
 * @author 智云
 */
@Import(OaScheduleServiceImpl.class)
public class OaScheduleServiceImplTest extends BaseDbUnitTest {

    @Resource
    private OaScheduleServiceImpl scheduleService;

    @Resource
    private OaScheduleMapper scheduleMapper;

    @Test
    public void test_createSchedule_success() {
        // 准备参数
        OaScheduleSaveReqVO reqVO = new OaScheduleSaveReqVO();
        reqVO.setUserId(1L);
        reqVO.setTitle("项目评审");
        reqVO.setStartTime(LocalDateTime.of(2026, 8, 1, 10, 0));
        reqVO.setEndTime(LocalDateTime.of(2026, 8, 1, 11, 0));

        // 调用
        Long id = scheduleService.createSchedule(reqVO);

        // 校验
        OaScheduleDO result = scheduleMapper.selectById(id);
        assertNotNull(result);
        assertEquals("项目评审", result.getTitle());
        assertEquals(0, result.getStatus()); // 默认未完成
        assertFalse(result.getReminded()); // 默认未提醒
    }

    @Test
    public void test_updateSchedule_success() {
        // mock 数据
        OaScheduleDO schedule = randomPojo(OaScheduleDO.class, o -> {
            o.setTitle("旧标题");
            o.setStatus(0);
        });
        scheduleMapper.insert(schedule);

        // 准备参数
        OaScheduleSaveReqVO reqVO = new OaScheduleSaveReqVO();
        reqVO.setId(schedule.getId());
        reqVO.setUserId(schedule.getUserId());
        reqVO.setTitle("新标题");
        reqVO.setStartTime(LocalDateTime.of(2026, 8, 2, 10, 0));

        // 调用
        scheduleService.updateSchedule(reqVO);

        // 校验标题更新，状态不变
        OaScheduleDO result = scheduleMapper.selectById(schedule.getId());
        assertEquals("新标题", result.getTitle());
        assertEquals(0, result.getStatus()); // 通用更新不改状态
    }

    @Test
    public void test_completeSchedule_success() {
        // mock 数据
        OaScheduleDO schedule = randomPojo(OaScheduleDO.class, o -> o.setStatus(0));
        scheduleMapper.insert(schedule);

        // 调用
        scheduleService.completeSchedule(schedule.getId());

        // 校验
        OaScheduleDO result = scheduleMapper.selectById(schedule.getId());
        assertEquals(1, result.getStatus()); // 已完成
    }

    @Test
    public void test_cancelSchedule_success() {
        // mock 数据
        OaScheduleDO schedule = randomPojo(OaScheduleDO.class, o -> o.setStatus(0));
        scheduleMapper.insert(schedule);

        // 调用
        scheduleService.cancelSchedule(schedule.getId());

        // 校验
        OaScheduleDO result = scheduleMapper.selectById(schedule.getId());
        assertEquals(2, result.getStatus()); // 已取消
    }

    @Test
    public void test_deleteSchedule_success() {
        // mock 数据
        OaScheduleDO schedule = randomPojo(OaScheduleDO.class);
        scheduleMapper.insert(schedule);

        // 调用
        scheduleService.deleteSchedule(schedule.getId());

        // 校验
        assertNull(scheduleMapper.selectById(schedule.getId()));
    }

    @Test
    public void test_deleteSchedule_notExists() {
        assertThrows(Exception.class, () -> scheduleService.deleteSchedule(randomLongId()));
    }

    @Test
    public void test_getSchedule_success() {
        // mock 数据
        OaScheduleDO schedule = randomPojo(OaScheduleDO.class);
        scheduleMapper.insert(schedule);

        // 调用并校验
        OaScheduleDO result = scheduleService.getSchedule(schedule.getId());
        assertNotNull(result);
        assertEquals(schedule.getId(), result.getId());
    }

    @Test
    public void test_getScheduleListByUser_success() {
        // mock 数据
        OaScheduleDO schedule = randomPojo(OaScheduleDO.class, o -> {
            o.setUserId(100L);
            o.setStartTime(LocalDateTime.of(2026, 8, 1, 10, 0));
        });
        scheduleMapper.insert(schedule);

        // 调用并校验
        var list = scheduleService.getScheduleListByUser(100L,
                LocalDateTime.of(2026, 8, 1, 0, 0),
                LocalDateTime.of(2026, 8, 1, 23, 59));
        assertTrue(list.size() >= 1);
    }

}
