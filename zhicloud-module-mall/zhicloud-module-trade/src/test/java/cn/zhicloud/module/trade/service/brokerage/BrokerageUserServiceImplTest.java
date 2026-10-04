package cn.zhicloud.module.trade.service.brokerage;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.trade.controller.admin.brokerage.vo.user.BrokerageUserPageReqVO;
import cn.zhicloud.module.trade.dal.dataobject.brokerage.BrokerageUserDO;
import cn.zhicloud.module.trade.dal.mysql.brokerage.BrokerageUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import java.time.LocalDateTime;

import static cn.zhicloud.framework.common.util.date.LocalDateTimeUtils.buildBetweenTime;
import static cn.zhicloud.framework.common.util.object.ObjectUtils.cloneIgnoreId;
import static cn.zhicloud.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import cn.zhicloud.module.trade.service.config.TradeConfigService;
import cn.zhicloud.module.trade.dal.dataobject.config.TradeConfigDO;
import cn.zhicloud.module.trade.enums.brokerage.BrokerageEnabledConditionEnum;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import cn.zhicloud.module.member.api.user.MemberUserApi;
import org.junit.jupiter.api.Disabled;

// TODO @芋艿：单测后续看看
/**
 * {@link BrokerageUserServiceImpl} 的单元测试类
 *
 * @author owen
 */
@Import(BrokerageUserServiceImpl.class)
public class BrokerageUserServiceImplTest extends BaseDbUnitTest {

    @Resource
    private BrokerageUserServiceImpl brokerageUserService;

    @Resource
    private BrokerageUserMapper brokerageUserMapper;
    @MockitoBean
    private TradeConfigService tradeConfigService;
    @MockitoBean
    private MemberUserApi memberUserApi;

    /**
     * 上游原样跑不通的一条：`BrokerageUserMapper.selectPage` 只按
     * brokerageEnabled / createTime / bindUserTime 过滤（bindUserId 根本不在查询条件里，
     * 只有配 level 走 childIds 时才生效），而原用例把"应查到"的那行的 createTime 设成 null、
     * 又用 bindUserId 制造"不匹配"，于是结果只可能是 0 或 4，断言却写死 1。
     * 这里按 mapper 真实的过滤列重造夹具：三个维度各留一条能区分的，断言 1 才成立。
     */
    @Test
    public void testGetBrokerageUserPage() {
        // mock 数据（会被查到：enabled=true 且 createTime 落在窗口内）
        BrokerageUserDO dbBrokerageUser = randomPojo(BrokerageUserDO.class, o -> {
            o.setBrokerageEnabled(true);
            o.setCreateTime(LocalDateTime.of(2023, 2, 10, 10, 0, 0));
        });
        brokerageUserMapper.insert(dbBrokerageUser);
        // 不匹配：brokerageEnabled 相反
        brokerageUserMapper.insert(cloneIgnoreId(dbBrokerageUser, o -> o.setBrokerageEnabled(false)));
        // 不匹配：createTime 落在窗口外
        brokerageUserMapper.insert(cloneIgnoreId(dbBrokerageUser,
                o -> o.setCreateTime(LocalDateTime.of(2023, 3, 10, 10, 0, 0))));
        // 准备参数
        BrokerageUserPageReqVO reqVO = new BrokerageUserPageReqVO();
        reqVO.setBrokerageEnabled(true);
        reqVO.setCreateTime(buildBetweenTime(2023, 2, 1, 2023, 2, 28));

        // 调用
        PageResult<BrokerageUserDO> pageResult = brokerageUserService.getBrokerageUserPage(reqVO);
        // 断言
        assertEquals(1, pageResult.getTotal());
        assertEquals(1, pageResult.getList().size());
        assertPojoEquals(dbBrokerageUser, pageResult.getList().get(0));
    }

    /**
     * trade_config 在干净初始化的库里是空表（仓库没有种子行）。
     * 修复前这里会 NullPointerException（getOrCreateBrokerageUser 直接解引用配置），
     * 表现是"会员一绑定推荐人就 500"；修复后沿用同模块既有语义：配置缺失＝人人分销未启用。
     */
    @Test
    public void testGetOrCreateBrokerageUser_whenTradeConfigMissing() {
        when(tradeConfigService.getTradeConfig()).thenReturn(null);
        assertNull(brokerageUserService.getOrCreateBrokerageUser(1L));
        assertNull(brokerageUserMapper.selectById(1L));
    }

    /**
     * 对照：配置存在且是"人人分销"时，行为不变（会建出分销用户）。
     * 防止把上面的空值分支顺手改成"永不创建"。
     */
    @Test
    public void testGetOrCreateBrokerageUser_whenAllUsersEnabled() {
        TradeConfigDO config = randomPojo(TradeConfigDO.class,
                o -> o.setBrokerageEnabledCondition(BrokerageEnabledConditionEnum.ALL.getCondition()));
        when(tradeConfigService.getTradeConfig()).thenReturn(config);
        assertNotNull(brokerageUserService.getOrCreateBrokerageUser(2L));
        assertNotNull(brokerageUserMapper.selectById(2L));
    }

}
