package cn.zhicloud.framework.integration.job;

import cn.zhicloud.framework.integration.service.EventRelayService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 事件中继定时任务测试（V100）
 *
 * <p>关注点只有一个：任务必须真的去排空发件箱，且不能因为基础设施故障而让调度线程终止。
 * 「bean 存在但从不执行」是定时任务最难发现的一类故障，这里用直接调用 execute() 的方式钉死。
 *
 * @author 智云集成治理
 */
public class EventRelayJobTest {

    @Test
    public void testJobDrainsOutbox() throws Exception {
        CountingRelay relay = new CountingRelay(3, null);
        EventRelayJob job = newJob(relay);

        assertEquals(3, job.execute());
        assertEquals(1, relay.calls);
        assertEquals(0, relay.lastLimit, "批大小应交给配置决定，任务本身不硬编码条数");
    }

    @Test
    public void testJobSurvivesInfrastructureFailure() throws Exception {
        CountingRelay relay = new CountingRelay(0, new IllegalStateException("db down"));
        EventRelayJob job = newJob(relay);

        assertEquals(-1, job.execute(), "基础设施故障返回 -1，调度线程必须继续存活等下一轮");
        assertEquals(1, relay.calls);
    }

    private EventRelayJob newJob(EventRelayService relay) throws Exception {
        EventRelayJob job = new EventRelayJob();
        Field field = EventRelayJob.class.getDeclaredField("eventRelayService");
        field.setAccessible(true);
        field.set(job, relay);
        return job;
    }

    /**
     * 只记录调用的假中继：不碰数据库，保证测试确定性
     */
    private static class CountingRelay extends EventRelayService {

        private final int published;
        private final RuntimeException boom;
        private int calls;
        private int lastLimit = -1;

        private CountingRelay(int published, RuntimeException boom) {
            this.published = published;
            this.boom = boom;
        }

        @Override
        public int relayOnce(int limit) {
            calls++;
            lastLimit = limit;
            if (boom != null) {
                throw boom;
            }
            return published;
        }
    }

}