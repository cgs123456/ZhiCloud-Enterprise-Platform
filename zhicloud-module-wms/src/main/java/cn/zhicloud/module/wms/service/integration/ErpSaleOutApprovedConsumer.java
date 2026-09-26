package cn.zhicloud.module.wms.service.integration;

import cn.zhicloud.framework.integration.api.EventConsumer;
import cn.zhicloud.framework.integration.contract.ErpSaleOutApprovedEvent;
import cn.zhicloud.framework.integration.contract.IntegrationEventTypes;
import cn.zhicloud.framework.integration.dal.dataobject.EventOutboxDO;
import cn.zhicloud.framework.mdm.enums.MdmConstants;
import cn.zhicloud.framework.mdm.service.MdmCrossRefService;
import cn.zhicloud.module.wms.controller.admin.order.shipment.vo.detail.WmsShipmentOrderDetailSaveReqVO;
import cn.zhicloud.module.wms.controller.admin.order.shipment.vo.order.WmsShipmentOrderSaveReqVO;
import cn.zhicloud.module.wms.dal.mysql.order.shipment.WmsShipmentOrderMapper;
import cn.zhicloud.module.wms.enums.order.WmsShipmentOrderTypeEnum;
import cn.zhicloud.module.wms.service.order.shipment.WmsShipmentOrderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.SHIPMENT_ORDER_MULTI_WAREHOUSE_UNSUPPORTED;

/**
 * 「ERP 销售出库审核通过」事件消费者：生成 WMS 销售出库单（V101 首条链路）
 *
 * <p>三段式处理顺序是刻意的，顺序错了会留下不一致数据：
 * <ol>
 *   <li>业务级查重：先用 ERP 单据号查有没有建过出库单。幂等必须放在最前面，
 *       否则「至少一次投递」的重试会建出第二张单。</li>
 *   <li>MDM 主数据映射：ERP 产品/仓库/客户编号在 WMS 里是另一套主键，必须经对照表翻译。
 *       映射缺失抛异常而不是跳过：跳过会让下游建到错误 SKU/仓库上且事后不可见，
 *       抛异常则由可靠集成数据面（V100）重试并最终进死信告警。</li>
 *   <li>建单：走 WMS 自己的 Service，不绕过校验（单号唯一、仓库存在、客户存在）。</li>
 * </ol>
 *
 * <p>已知限制（已在风险清单登记）：
 * 出库单号用 {@code WS + ERP 单号} 拼，不是 WMS 自己的单号生成器产物；
 * WMS 出库单模型只有单仓库，ERP 出库行跨仓库时本版本直接失败而不是拆单。
 *
 * @author 智云集成治理
 */
@Component
@Slf4j
public class ErpSaleOutApprovedConsumer implements EventConsumer {

    /**
     * 消费者名称：会写进 {@code event_inbox} 的去重唯一键，改名等于换了一个消费者，
     * 历史事件会被重新消费一次，务必谨慎。
     */
    public static final String NAME = "wms-shipment-order-creator";

    /**
     * 默认租户：ERP/WMS 主表虽有 tenant_id 列，但相关 DO 未映射租户字段，取不到真实租户；
     * 接入多租户前不要用本链路承载多租户数据（已在风险清单登记）。
     */
    private static final Long DEFAULT_TENANT_ID = 0L;

    @Resource
    private WmsShipmentOrderService shipmentOrderService;
    @Resource
    private WmsShipmentOrderMapper shipmentOrderMapper;
    @Resource
    private MdmCrossRefService mdmCrossRefService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean supports(String eventType) {
        return IntegrationEventTypes.ERP_SALE_OUT_APPROVED.equals(eventType);
    }

    @Override
    public void handle(EventOutboxDO event) throws Exception {
        ErpSaleOutApprovedEvent payload = ErpSaleOutApprovedEvent.fromJson(event.getPayload());

        // 1. 业务级幂等：同一张 ERP 出库单只建一张 WMS 出库单
        if (shipmentOrderMapper.selectByBizOrderNo(payload.getNo()) != null) {
            log.info("[ERP->WMS] 业务单号已存在，跳过建单 bizOrderNo={}", payload.getNo());
            return;
        }

        // 2. 主数据映射：ERP 编号 -> WMS 编号
        Long warehouseId = null;
        List<WmsShipmentOrderDetailSaveReqVO> details = new ArrayList<>();
        for (ErpSaleOutApprovedEvent.Item item : payload.getItems()) {
            Long skuId = mdmCrossRefService.mapTargetIdAsLong(DEFAULT_TENANT_ID,
                    MdmConstants.SYSTEM_ERP, MdmConstants.ENTITY_PRODUCT, String.valueOf(item.getProductId()),
                    MdmConstants.SYSTEM_WMS, MdmConstants.ENTITY_SKU);
            Long itemWarehouseId = mdmCrossRefService.mapTargetIdAsLong(DEFAULT_TENANT_ID,
                    MdmConstants.SYSTEM_ERP, MdmConstants.ENTITY_WAREHOUSE, String.valueOf(item.getWarehouseId()),
                    MdmConstants.SYSTEM_WMS, MdmConstants.ENTITY_WAREHOUSE);
            // WMS 出库单只有单仓库头字段，跨仓库的 ERP 出库单本版本不接受：宁可失败进死信，
            // 也不要把第二个仓库的货按第一个仓库建单（那会造成库存账实不符且难以追回）。
            if (warehouseId == null) {
                warehouseId = itemWarehouseId;
            } else if (!Objects.equals(warehouseId, itemWarehouseId)) {
                throw exception(SHIPMENT_ORDER_MULTI_WAREHOUSE_UNSUPPORTED, payload.getNo());
            }
            details.add(new WmsShipmentOrderDetailSaveReqVO().setSkuId(skuId)
                    .setQuantity(item.getCount())
                    .setPrice(item.getPrice()));
        }
        // 客户是可选映射：映射缺失只影响单据上的客户字段，不影响出库动作本身是否发生
        String merchantIdText = mdmCrossRefService.findTargetId(DEFAULT_TENANT_ID,
                MdmConstants.SYSTEM_ERP, MdmConstants.ENTITY_CUSTOMER, String.valueOf(payload.getCustomerId()),
                MdmConstants.SYSTEM_WMS, MdmConstants.ENTITY_MERCHANT);
        Long merchantId = merchantIdText == null ? null : Long.valueOf(merchantIdText);

        // 3. 建单：走 WMS 自己的 Service，保留全部业务校验
        WmsShipmentOrderSaveReqVO createReqVO = new WmsShipmentOrderSaveReqVO()
                .setNo(buildShipmentOrderNo(payload.getNo()))
                .setType(WmsShipmentOrderTypeEnum.SALE.getType())
                .setOrderTime(payload.getOutTime() == null ? LocalDateTime.now() : payload.getOutTime())
                .setBizOrderNo(payload.getNo())
                .setMerchantId(merchantId)
                .setWarehouseId(warehouseId)
                .setRemark("来源：ERP 销售出库 " + payload.getNo() + "（V101 集成链路自动生成）")
                .setDetails(details);
        Long id = shipmentOrderService.createShipmentOrder(createReqVO);
        log.info("[ERP->WMS] 已生成出库单 id={} no={} bizOrderNo={}", id, createReqVO.getNo(), payload.getNo());
    }

    /**
     * 出库单号：{@code WS + ERP 单号}。
     * <p>刻意可推导：人工看到 WMS 单号就能反查 ERP 单号；也保证同一 ERP 单号重试时不会因为
     * 单号随机而绕过「单号唯一」校验建出第二张单。真实部署应换成 WMS 单号生成器，
     * 但生成后必须仍把 ERP 单号写进 bizOrderNo 用于追溯（已登记为风险）。
     */
    private static String buildShipmentOrderNo(String erpNo) {
        return "WS" + erpNo;
    }

}