package cn.zhicloud.module.oa.service.portal;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.oa.dal.dataobject.knowledge.OaKnowledgeArticleDO;
import cn.zhicloud.module.oa.dal.dataobject.meeting.OaMeetingReservationDO;
import cn.zhicloud.module.oa.dal.mysql.knowledge.OaKnowledgeArticleMapper;
import cn.zhicloud.module.oa.dal.mysql.meeting.OaMeetingReservationMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomLongId;
import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link OaPortalServiceImpl} 工作台统计的单元测试。
 *
 * <p>写这个测试的缘由：这三个计数项曾经被硬编码成 0，注释还写错了理由——
 * 「今日会议数依赖会议 service，暂占位」「已发布知识文章数：知识库 service 未实现」。
 * 两个 service 在**同一模块内**早已实现（{@code OaKnowledgeArticleServiceImpl}
 * 有 {@code publishArticle}，{@code OaMeetingReservationServiceImpl} 完整可用）。
 * 也就是说工作台上这两块长期显示 0，而库里明明有数据。</p>
 *
 * <p>这类缺陷的特征是「没有任何测试会红」：返回值恒为 0 也是合法的整型。
 * 所以这里的断言必须钉住**具体非零值**，而不是「不等于 null」——
 * 否则将来再退化成 0，测试依然会绿。</p>
 *
 * @author 智云
 */
@Import(OaPortalServiceImpl.class)
public class OaPortalServiceImplTest extends BaseDbUnitTest {

    @Resource
    private OaPortalServiceImpl portalService;

    @Resource
    private OaKnowledgeArticleMapper knowledgeArticleMapper;

    @Resource
    private OaMeetingReservationMapper meetingReservationMapper;

    /**
     * 今日会议数必须真的数得出来，且不含已取消的。
     *
     * <p>构造 3 条：今天 1 条、昨天 1 条、今天但已取消 1 条 → 期望 1。
     * 三个条件分别覆盖：按日期窗口过滤、不把昨天的算成今天、排除已取消。</p>
     */
    @Test
    public void test_todayMeetingCount_countsOnlyTodayAndNotCanceled() {
        LocalDateTime now = LocalDateTime.now();
        // 今天的会议（保留时分秒，确认不是整天比较）
        meetingReservationMapper.insert(randomPojo(OaMeetingReservationDO.class, o -> {
            o.setStartTime(now);
            o.setEndTime(now.plusHours(1));
            o.setStatus(10); // 待确认
        }));
        // 昨天的会议
        meetingReservationMapper.insert(randomPojo(OaMeetingReservationDO.class, o -> {
            o.setStartTime(LocalDate.now().minusDays(1).atTime(9, 0));
            o.setEndTime(LocalDate.now().minusDays(1).atTime(10, 0));
            o.setStatus(10);
        }));
        // 今天但已取消的会议（status=30）
        meetingReservationMapper.insert(randomPojo(OaMeetingReservationDO.class, o -> {
            o.setStartTime(now.plusHours(3));
            o.setEndTime(now.plusHours(4));
            o.setStatus(30);
        }));

        Integer todayMeetingCount = portalService.getDashboard().getTodayMeetingCount();

        assertEquals(1, todayMeetingCount, "今日会议数应只统计今天、且排除已取消的会议");
    }

    /**
     * 已发布知识文章数：只算 status=20。
     *
     * <p>构造 已发布2 / 草稿1 / 已下架1 → 期望 2。
     * 这三个状态在 {@code OaKnowledgeArticleDO#status} 的注释里有定义，
     * 但模块内没有枚举，所以这里直接写字面量并在注释里标明来源。</p>
     */
    @Test
    public void test_publishedKnowledgeCount_countsOnlyPublished() {
        insertKnowledgeArticle(20); // 已发布
        insertKnowledgeArticle(20); // 已发布
        insertKnowledgeArticle(10); // 草稿
        insertKnowledgeArticle(30); // 已下架

        Integer publishedKnowledgeCount = portalService.getDashboard().getPublishedKnowledgeCount();

        assertEquals(2, publishedKnowledgeCount, "已发布知识文章数应只统计 status=20");
    }

    /**
     * 这两个计数项必须能被「库里没数据」如实反映成 0，
     * 而不是依赖任何缓存或上一次调用的残留。
     */
    @Test
    public void test_countsAreZeroWhenNoData() {
        assertEquals(0, portalService.getDashboard().getTodayMeetingCount());
        assertEquals(0, portalService.getDashboard().getPublishedKnowledgeCount());
    }

    private void insertKnowledgeArticle(Integer status) {
        knowledgeArticleMapper.insert(randomPojo(OaKnowledgeArticleDO.class, o -> {
            o.setId(randomLongId());
            o.setStatus(status);
        }));
    }
}
