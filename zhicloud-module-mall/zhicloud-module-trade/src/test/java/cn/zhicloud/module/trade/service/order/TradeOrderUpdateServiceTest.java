package cn.zhicloud.module.trade.service.order;

import cn.hutool.core.util.IdUtil;
import cn.zhicloud.framework.common.enums.CommonStatusEnum;
import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.member.api.address.MemberAddressApi;
import cn.zhicloud.module.member.api.user.MemberUserApi;
import cn.zhicloud.module.pay.api.order.PayOrderApi;
import cn.zhicloud.module.pay.api.order.dto.PayOrderRespDTO;
import cn.zhicloud.module.pay.enums.order.PayOrderStatusEnum;
import cn.zhicloud.module.product.api.comment.ProductCommentApi;
import cn.zhicloud.module.product.api.sku.ProductSkuApi;
import cn.zhicloud.module.product.api.spu.ProductSpuApi;
import cn.zhicloud.module.promotion.api.coupon.CouponApi;
import cn.zhicloud.module.system.api.notify.NotifyMessageSendApi;
import cn.zhicloud.module.trade.controller.admin.delivery.vo.express.DeliveryExpressCreateReqVO;
import cn.zhicloud.module.trade.controller.admin.order.vo.TradeOrderDeliveryReqVO;
import cn.zhicloud.module.trade.dal.dataobject.order.TradeOrderDO;
import cn.zhicloud.module.trade.dal.mysql.order.TradeOrderItemMapper;
import cn.zhicloud.module.trade.dal.mysql.order.TradeOrderMapper;
import cn.zhicloud.module.trade.dal.redis.no.TradeNoRedisDAO;
import cn.zhicloud.module.trade.enums.delivery.DeliveryTypeEnum;
import cn.zhicloud.module.trade.enums.order.TradeOrderRefundStatusEnum;
import cn.zhicloud.module.trade.enums.order.TradeOrderStatusEnum;
import cn.zhicloud.module.trade.framework.order.config.TradeOrderConfig;
import cn.zhicloud.module.trade.framework.order.config.TradeOrderProperties;
import cn.zhicloud.module.trade.service.cart.CartServiceImpl;
import cn.zhicloud.module.trade.service.delivery.DeliveryExpressService;
import cn.zhicloud.module.trade.service.delivery.DeliveryExpressServiceImpl;
import cn.zhicloud.module.trade.service.message.TradeMessageServiceImpl;
import cn.zhicloud.module.trade.service.order.handler.TradeOrderHandler;
import cn.zhicloud.module.trade.service.price.TradePriceServiceImpl;
import cn.zhicloud.module.trade.service.price.calculator.TradePriceCalculator;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;

import static cn.zhicloud.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import cn.zhicloud.module.promotion.api.discount.DiscountActivityApi;
import cn.zhicloud.module.promotion.api.reward.RewardActivityApi;
import org.junit.jupiter.api.Disabled;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import cn.zhicloud.module.trade.service.price.calculator.TradeBargainActivityPriceCalculator;
import cn.zhicloud.module.trade.service.price.calculator.TradeCombinationActivityPriceCalculator;
import cn.zhicloud.module.trade.service.price.calculator.TradeCouponPriceCalculator;
import cn.zhicloud.module.trade.service.price.calculator.TradeDeliveryPriceCalculator;
import cn.zhicloud.module.trade.service.price.calculator.TradeDiscountActivityPriceCalculator;
import cn.zhicloud.module.trade.service.price.calculator.TradePointActivityPriceCalculator;
import cn.zhicloud.module.trade.service.price.calculator.TradePointGiveCalculator;
import cn.zhicloud.module.trade.service.price.calculator.TradePointUsePriceCalculator;
import cn.zhicloud.module.trade.service.price.calculator.TradeRewardActivityPriceCalculator;
import cn.zhicloud.module.trade.service.price.calculator.TradeSeckillActivityPriceCalculator;
import cn.zhicloud.module.promotion.api.bargain.BargainRecordApi;
import cn.zhicloud.module.promotion.api.combination.CombinationRecordApi;
import cn.zhicloud.module.trade.service.delivery.DeliveryExpressTemplateService;
import cn.zhicloud.module.trade.service.delivery.DeliveryPickUpStoreService;
import cn.zhicloud.module.member.api.config.MemberConfigApi;
import cn.zhicloud.module.member.api.level.MemberLevelApi;
import cn.zhicloud.module.promotion.api.point.PointActivityApi;
import cn.zhicloud.module.promotion.api.seckill.SeckillActivityApi;
import cn.zhicloud.module.trade.service.config.TradeConfigService;
import cn.zhicloud.module.trade.service.order.TradeOrderQueryService;
import org.junit.jupiter.api.Disabled;

/**
 * {@link TradeOrderUpdateServiceImpl} 的单元测试类
 *
 * @author LeeYan9
 * @since 2022-09-07
 */
@Import({TradeOrderUpdateServiceImpl.class, TradeOrderConfig.class, CartServiceImpl.class, TradePriceServiceImpl.class,
        DeliveryExpressServiceImpl.class, TradeMessageServiceImpl.class,
        // 价格计算链：TradePriceServiceImpl 按类型注入这些计算器，缺一个就起不了上下文
        TradeDiscountActivityPriceCalculator.class, TradeRewardActivityPriceCalculator.class,
        TradeCouponPriceCalculator.class, TradeDeliveryPriceCalculator.class, TradeBargainActivityPriceCalculator.class,
        TradeCombinationActivityPriceCalculator.class, TradePointActivityPriceCalculator.class,
        TradePointGiveCalculator.class, TradePointUsePriceCalculator.class, TradeSeckillActivityPriceCalculator.class,
        // 方法级 @Validated 需要真实 Validator（用 mock 会让参数校验静默失效）
        ValidationAutoConfiguration.class
})
@Disabled // TODO 上游标注"后续 fix 补充的单测"。本轮已把价格计算链（10 个计算器）与一批跨模块 API 替身补齐、并接入真实 Validator（不再用 mock 把参数校验变成空转）；还差 system 侧 SocialClientApi 等替身，见 handoff §12.92 的实测清单
public class TradeOrderUpdateServiceTest extends BaseDbUnitTest {

    @Resource
    private TradeOrderUpdateServiceImpl tradeOrderUpdateService;

    @Resource
    private TradeOrderMapper tradeOrderMapper;
    @Resource
    private TradeOrderItemMapper tradeOrderItemMapper;

    // 价格计算链与跨模块 API 的替身：每个字段恰好一个 @MockitoBean（曾有插入错位，导致注解堆叠）
    @MockitoBean
    private MemberUserApi memberUserApi;

    @MockitoBean
    private ProductSpuApi productSpuApi;

    @MockitoBean
    private ProductSkuApi productSkuApi;

    @MockitoBean
    private ProductCommentApi productCommentApi;

    @MockitoBean
    private PayOrderApi payOrderApi;

    @MockitoBean
    private MemberAddressApi addressApi;

    @MockitoBean
    private CouponApi couponApi;

    @MockitoBean
    private TradeOrderProperties tradeOrderProperties;

    @MockitoBean
    private TradeNoRedisDAO tradeNoRedisDAO;

    @MockitoBean
    private TradeOrderHandler tradeOrderHandler;

    @MockitoBean
    private NotifyMessageSendApi notifyMessageSendApi;

    @MockitoBean
    private DeliveryExpressService deliveryExpressService;

    @MockitoBean
    private DiscountActivityApi discountActivityApi;

    @MockitoBean
    private TradeOrderQueryService tradeOrderQueryService;

    @MockitoBean
    private TradeConfigService tradeConfigService;

    @MockitoBean
    private SeckillActivityApi seckillActivityApi;

    @MockitoBean
    private PointActivityApi pointActivityApi;

    @MockitoBean
    private MemberLevelApi memberLevelApi;

    @MockitoBean
    private MemberConfigApi memberConfigApi;

    @MockitoBean
    private DeliveryPickUpStoreService deliveryPickUpStoreService;

    @MockitoBean
    private DeliveryExpressTemplateService deliveryExpressTemplateService;

    @MockitoBean
    private CombinationRecordApi combinationRecordApi;

    @MockitoBean
    private BargainRecordApi bargainRecordApi;

    @MockitoBean
    private RewardActivityApi rewardActivityApi;


    //    @MockitoBean
//    private PriceApi priceApi;   // 上游注释掉的替身，原样保留

    @BeforeEach
    public void setUp() {
        when(tradeOrderProperties.getPayAppKey()).thenReturn("mall");
        when(tradeOrderProperties.getPayExpireTime()).thenReturn(Duration.ofDays(1));
        when(tradeNoRedisDAO.generate(anyString())).thenReturn(IdUtil.randomUUID());
    }

//    @Test
//    public void testCreateTradeOrder_success() {
//        // 准备参数
//        Long userId = 100L;
//        String userIp = "127.0.0.1";
////        AppTradeOrderCreateReqVO reqVO = new AppTradeOrderCreateReqVO()
////                .setAddressId(10L).setCouponId(101L).setRemark("我是备注").setFromCart(true)
////                .setItems(Arrays.asList(new AppTradeOrderCreateReqVO.Item().setSkuId(1L).setCount(3),
////                        new AppTradeOrderCreateReqVO.Item().setSkuId(2L).setCount(4)));
//        AppTradeOrderCreateReqVO reqVO = null;
//        // TODO 芋艿：重新高下
//        // mock 方法（商品 SKU 检查）
//        ProductSkuRespDTO sku01 = randomPojo(ProductSkuRespDTO.class, o -> o.setId(1L).setSpuId(11L)
//                .setPrice(50).setStock(100)
//                .setProperties(singletonList(new ProductPropertyValueDetailRespDTO().setPropertyId(111L).setValueId(222L))));
//        ProductSkuRespDTO sku02 = randomPojo(ProductSkuRespDTO.class, o -> o.setId(2L).setSpuId(21L)
//                .setPrice(20).setStock(50))
//                .setProperties(singletonList(new ProductPropertyValueDetailRespDTO().setPropertyId(333L).setValueId(444L)));
//        when(productSkuApi.getSkuList(eq(asSet(1L, 2L)))).thenReturn(Arrays.asList(sku01, sku02));
//        // mock 方法（商品 SPU 检查）
//        ProductSpuRespDTO spu01 = randomPojo(ProductSpuRespDTO.class, o -> o.setId(11L)
//                .setStatus(ProductSpuStatusEnum.ENABLE.getStatus()).setName("商品 1"));
//        ProductSpuRespDTO spu02 = randomPojo(ProductSpuRespDTO.class, o -> o.setId(21L)
//                .setStatus(ProductSpuStatusEnum.ENABLE.getStatus()));
//        when(productSpuApi.getSpuList(eq(asSet(11L, 21L)))).thenReturn(Arrays.asList(spu01, spu02));
//        // mock 方法（用户收件地址的校验）
//        MemberAddressRespDTO addressRespDTO = new MemberAddressRespDTO().setId(10L).setUserId(userId).setName("芋艿")
//                .setMobile("15601691300").setAreaId(3306).setDetailAddress("土豆村");
//        when(addressApi.getAddress(eq(10L), eq(userId))).thenReturn(addressRespDTO);
//        // mock 方法（价格计算）
//        PriceCalculateRespDTO.OrderItem priceOrderItem01 = new PriceCalculateRespDTO.OrderItem()
//                .setSpuId(11L).setSkuId(1L).setCount(3).setOriginalPrice(150).setOriginalUnitPrice(50)
//                .setDiscountPrice(20).setPayPrice(130).setOrderPartPrice(7).setOrderDividePrice(35);
//        PriceCalculateRespDTO.OrderItem priceOrderItem02 = new PriceCalculateRespDTO.OrderItem()
//                .setSpuId(21L).setSkuId(2L).setCount(4).setOriginalPrice(80).setOriginalUnitPrice(20)
//                .setDiscountPrice(40).setPayPrice(40).setOrderPartPrice(15).setOrderDividePrice(25);
//        PriceCalculateRespDTO.Order priceOrder = new PriceCalculateRespDTO.Order()
//                .setTotalPrice(230).setDiscountPrice(0).setCouponPrice(30)
//                .setPointPrice(10).setDeliveryPrice(20).setPayPrice(80).setCouponId(101L).setCouponPrice(30)
//                .setItems(Arrays.asList(priceOrderItem01, priceOrderItem02));
//        when(priceApi.calculatePrice(argThat(priceCalculateReqDTO -> {
//            assertEquals(priceCalculateReqDTO.getUserId(), 100L);
//            assertEquals(priceCalculateReqDTO.getCouponId(), 101L);
//            assertEquals(priceCalculateReqDTO.getItems().get(0).getSkuId(), 1L);
//            assertEquals(priceCalculateReqDTO.getItems().get(0).getCount(), 3);
//            assertEquals(priceCalculateReqDTO.getItems().get(1).getSkuId(), 2L);
//            assertEquals(priceCalculateReqDTO.getItems().get(1).getCount(), 4);
//            return true;
//        }))).thenReturn(new PriceCalculateRespDTO().setOrder(priceOrder));
//        // mock 方法（创建支付单）
//        when(payOrderApi.createOrder(argThat(createReqDTO -> {
//            assertEquals(createReqDTO.getAppId(), 888L);
//            assertEquals(createReqDTO.getUserIp(), userIp);
//            assertNotNull(createReqDTO.getMerchantOrderId()); // 由于 tradeOrderId 后生成，只能校验非空
//            assertEquals(createReqDTO.getSubject(), "商品 1 等多件");
//            assertNull(createReqDTO.getBody());
//            assertEquals(createReqDTO.getPrice(), 80);
//            assertNotNull(createReqDTO.getExpireTime());
//            return true;
//        }))).thenReturn(1000L);
//
//        // 调用方法
//        TradeOrderDO order = tradeOrderUpdateService.createOrder(userId, userIp, reqVO, null);
//        // 断言 TradeOrderDO 订单
//        List<TradeOrderDO> tradeOrderDOs = tradeOrderMapper.selectList();
//        assertEquals(tradeOrderDOs.size(), 1);
//        TradeOrderDO tradeOrderDO = tradeOrderDOs.get(0);
//        assertEquals(tradeOrderDO.getId(), order.getId());
//        assertNotNull(tradeOrderDO.getNo());
//        assertEquals(tradeOrderDO.getType(), TradeOrderTypeEnum.NORMAL.getType());
//        assertEquals(tradeOrderDO.getTerminal(), TerminalEnum.H5.getTerminal());
//        assertEquals(tradeOrderDO.getUserId(), userId);
//        assertEquals(tradeOrderDO.getUserIp(), userIp);
//        assertEquals(tradeOrderDO.getStatus(), TradeOrderStatusEnum.UNPAID.getStatus());
//        assertEquals(tradeOrderDO.getProductCount(), 7);
//        assertNull(tradeOrderDO.getFinishTime());
//        assertNull(tradeOrderDO.getCancelTime());
//        assertNull(tradeOrderDO.getCancelType());
//        assertEquals(tradeOrderDO.getUserRemark(), "我是备注");
//        assertNull(tradeOrderDO.getRemark());
//        assertFalse(tradeOrderDO.getPayStatus());
//        assertNull(tradeOrderDO.getPayTime());
//        assertEquals(tradeOrderDO.getTotalPrice(), 230);
//        assertEquals(tradeOrderDO.getDiscountPrice(), 0);
//        assertEquals(tradeOrderDO.getAdjustPrice(), 0);
//        assertEquals(tradeOrderDO.getPayPrice(), 80);
//        assertEquals(tradeOrderDO.getPayOrderId(), 1000L);
//        assertNull(tradeOrderDO.getPayChannelCode());
//        assertNull(tradeOrderDO.getLogisticsId());
//        assertNull(tradeOrderDO.getDeliveryTime());
//        assertNull(tradeOrderDO.getReceiveTime());
//        assertEquals(tradeOrderDO.getReceiverName(), "芋艿");
//        assertEquals(tradeOrderDO.getReceiverMobile(), "15601691300");
//        assertEquals(tradeOrderDO.getReceiverAreaId(), 3306);
//        assertEquals(tradeOrderDO.getReceiverDetailAddress(), "土豆村");
//        assertEquals(tradeOrderDO.getRefundStatus(), TradeOrderRefundStatusEnum.NONE.getStatus());
//        assertEquals(tradeOrderDO.getRefundPrice(), 0);
//        assertEquals(tradeOrderDO.getCouponPrice(), 30);
//        assertEquals(tradeOrderDO.getPointPrice(), 10);
//        // 断言 TradeOrderItemDO 订单（第 1 个）
//        List<TradeOrderItemDO> tradeOrderItemDOs = tradeOrderItemMapper.selectList();
//        assertEquals(tradeOrderItemDOs.size(), 2);
//        TradeOrderItemDO tradeOrderItemDO01 = tradeOrderItemDOs.get(0);
//        assertNotNull(tradeOrderItemDO01.getId());
//        assertEquals(tradeOrderItemDO01.getUserId(), userId);
//        assertEquals(tradeOrderItemDO01.getOrderId(), order.getId());
//        assertEquals(tradeOrderItemDO01.getSpuId(), 11L);
//        assertEquals(tradeOrderItemDO01.getSkuId(), 1L);
//        assertEquals(tradeOrderItemDO01.getProperties().size(), 1);
//        assertEquals(tradeOrderItemDO01.getProperties().get(0).getPropertyId(), 111L);
//        assertEquals(tradeOrderItemDO01.getProperties().get(0).getValueId(), 222L);
//        //assertEquals(tradeOrderItemDO01.getSpuName(), sku01.getSpuName()); TODO 找不到spuName
//        assertEquals(tradeOrderItemDO01.getPicUrl(), sku01.getPicUrl());
//        assertEquals(tradeOrderItemDO01.getCount(), 3);
////        assertEquals(tradeOrderItemDO01.getOriginalPrice(), 150);
//        assertEquals(tradeOrderItemDO01.getPrice(), 50);
//        assertEquals(tradeOrderItemDO01.getDiscountPrice(), 20);
//        assertEquals(tradeOrderItemDO01.getPayPrice(), 130);
//        assertEquals(tradeOrderItemDO01.getAfterSaleStatus(), TradeOrderItemAfterSaleStatusEnum.NONE.getStatus());
//        // 断言 TradeOrderItemDO 订单（第 2 个）
//        TradeOrderItemDO tradeOrderItemDO02 = tradeOrderItemDOs.get(1);
//        assertNotNull(tradeOrderItemDO02.getId());
//        assertEquals(tradeOrderItemDO02.getUserId(), userId);
//        assertEquals(tradeOrderItemDO02.getOrderId(), order.getId());
//        assertEquals(tradeOrderItemDO02.getSpuId(), 21L);
//        assertEquals(tradeOrderItemDO02.getSkuId(), 2L);
//        assertEquals(tradeOrderItemDO02.getProperties().size(), 1);
//        assertEquals(tradeOrderItemDO02.getProperties().get(0).getPropertyId(), 333L);
//        assertEquals(tradeOrderItemDO02.getProperties().get(0).getValueId(), 444L);
//        //assertEquals(tradeOrderItemDO02.getSpuName(), sku02.getSpuName()); TODO 找不到spuName
//        assertEquals(tradeOrderItemDO02.getPicUrl(), sku02.getPicUrl());
//        assertEquals(tradeOrderItemDO02.getCount(), 4);
////        assertEquals(tradeOrderItemDO02.getOriginalPrice(), 80);
//        assertEquals(tradeOrderItemDO02.getPrice(), 20);
//        assertEquals(tradeOrderItemDO02.getDiscountPrice(), 40);
//        assertEquals(tradeOrderItemDO02.getPayPrice(), 40);
//        assertEquals(tradeOrderItemDO02.getAfterSaleStatus(), TradeOrderItemAfterSaleStatusEnum.NONE.getStatus());
//        // 校验调用
//        verify(productSkuApi).updateSkuStock(argThat(updateStockReqDTO -> {
//            assertEquals(updateStockReqDTO.getItems().size(), 2);
//            assertEquals(updateStockReqDTO.getItems().get(0).getId(), 1L);
//            assertEquals(updateStockReqDTO.getItems().get(0).getIncrCount(), 3);
//            assertEquals(updateStockReqDTO.getItems().get(1).getId(), 2L);
//            assertEquals(updateStockReqDTO.getItems().get(1).getIncrCount(), 4);
//            return true;
//        }));
//        verify(couponApi).useCoupon(argThat(reqDTO -> {
//            assertEquals(reqDTO.getId(), reqVO.getCouponId());
//            assertEquals(reqDTO.getUserId(), userId);
//            assertEquals(reqDTO.getOrderId(), order.getId());
//            return true;
//        }));
//    }

    @Test
    public void testUpdateOrderPaid() {
        // mock 数据（TradeOrder）
        TradeOrderDO order = randomPojo(TradeOrderDO.class, o -> {
            o.setId(1L).setStatus(TradeOrderStatusEnum.UNPAID.getStatus());
            o.setPayOrderId(10L).setPayStatus(false).setPayPrice(100).setPayTime(null);
        });
        tradeOrderMapper.insert(order);
        // 准备参数
        Long id = 1L;
        Long payOrderId = 10L;
        // mock 方法（支付单）
        when(payOrderApi.getOrder(eq(10L))).thenReturn(randomPojo(PayOrderRespDTO.class,
                o -> o.setStatus(PayOrderStatusEnum.SUCCESS.getStatus()).setChannelCode("wx_pub")
                        .setMerchantOrderId("1")).setPrice(100));

        // 调用
        tradeOrderUpdateService.updateOrderPaid(id, payOrderId);
        // 断言
        TradeOrderDO dbOrder = tradeOrderMapper.selectById(id);
        assertEquals(dbOrder.getStatus(), TradeOrderStatusEnum.UNDELIVERED.getStatus());
        assertTrue(dbOrder.getPayStatus());
        assertNotNull(dbOrder.getPayTime());
        assertEquals(dbOrder.getPayChannelCode(), "wx_pub");
    }

    @Test
    public void testDeliveryOrder() {
        // mock 数据（TradeOrder）
        TradeOrderDO order = randomPojo(TradeOrderDO.class, o -> {
            o.setId(1L).setStatus(TradeOrderStatusEnum.UNDELIVERED.getStatus());
            o.setLogisticsId(null).setLogisticsNo(null).setDeliveryTime(null);
            o.setRefundStatus(TradeOrderRefundStatusEnum.NONE.getStatus());
            o.setDeliveryType(DeliveryTypeEnum.EXPRESS.getType());
        });
        tradeOrderMapper.insert(order);

        DeliveryExpressCreateReqVO expressCreateReqVO = new DeliveryExpressCreateReqVO();
        expressCreateReqVO.setCode("code").setName("Name").setLogo("logo").setSort(0).setStatus(CommonStatusEnum.ENABLE.getStatus());
        Long deliveryExpressId = deliveryExpressService.createDeliveryExpress(expressCreateReqVO);
        // 准备参数
        TradeOrderDeliveryReqVO deliveryReqVO = new TradeOrderDeliveryReqVO().setId(1L)
                .setLogisticsId(deliveryExpressId).setLogisticsNo("100");

        // mock 方法（支付单）

        // 调用
        tradeOrderUpdateService.deliveryOrder(deliveryReqVO);
        // 断言
        TradeOrderDO dbOrder = tradeOrderMapper.selectById(1L);
        assertEquals(dbOrder.getStatus(), TradeOrderStatusEnum.DELIVERED.getStatus());
        assertPojoEquals(dbOrder, deliveryReqVO);
        assertNotNull(dbOrder.getDeliveryTime());
    }

    @Test
    public void testReceiveOrder() {
        // mock 数据（TradeOrder）
        TradeOrderDO order = randomPojo(TradeOrderDO.class, o -> {
            o.setId(1L).setUserId(10L).setStatus(TradeOrderStatusEnum.DELIVERED.getStatus());
            o.setReceiveTime(null);
        });
        tradeOrderMapper.insert(order);
        // 准备参数
        Long id = 1L;
        Long userId = 10L;
        // mock 方法（支付单）

        // 调用
        tradeOrderUpdateService.receiveOrderByMember(userId, id);
        // 断言
        TradeOrderDO dbOrder = tradeOrderMapper.selectById(1L);
        assertEquals(dbOrder.getStatus(), TradeOrderStatusEnum.COMPLETED.getStatus());
        assertNotNull(dbOrder.getReceiveTime());
    }

}
