package cn.iocoder.yudao.module.wms.service.order.wave;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.wms.controller.admin.order.wave.vo.order.WmsWaveOrderPageReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.order.wave.vo.order.WmsWaveOrderSaveReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.shipment.WmsShipmentOrderDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.shipment.WmsShipmentOrderDetailDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.wave.WmsWaveOrderDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.wave.WmsWaveOrderDetailDO;
import cn.iocoder.yudao.module.wms.dal.mysql.order.shipment.WmsShipmentOrderDetailMapper;
import cn.iocoder.yudao.module.wms.dal.mysql.order.shipment.WmsShipmentOrderMapper;
import cn.iocoder.yudao.module.wms.dal.mysql.order.wave.WmsWaveOrderDetailMapper;
import cn.iocoder.yudao.module.wms.dal.mysql.order.wave.WmsWaveOrderMapper;
import cn.iocoder.yudao.module.wms.enums.order.WmsOrderStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.*;

/**
 * WMS 波次单 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class WmsWaveOrderServiceImpl implements WmsWaveOrderService {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Resource
    private WmsWaveOrderMapper waveOrderMapper;

    @Resource
    private WmsWaveOrderDetailMapper waveOrderDetailMapper;

    @Resource
    private WmsShipmentOrderMapper shipmentOrderMapper;

    @Resource
    private WmsShipmentOrderDetailMapper shipmentOrderDetailMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWaveOrder(WmsWaveOrderSaveReqVO createReqVO) {
        // 1. 校验出库单列表非空
        if (CollUtil.isEmpty(createReqVO.getShipmentOrderIds())) {
            throw exception(WAVE_ORDER_NO_SHIPMENT_SELECTED);
        }

        // 2. 拉取出库单 + 明细，构造波次明细
        List<WmsShipmentOrderDO> shipmentOrders = shipmentOrderMapper.selectBatchIds(createReqVO.getShipmentOrderIds());
        if (shipmentOrders.size() != createReqVO.getShipmentOrderIds().size()) {
            throw exception(SHIPMENT_ORDER_NOT_EXISTS);
        }

        // 3. 生成波次单号
        String waveNo = generateWaveNo();
        if (waveOrderMapper.selectByNo(waveNo) != null) {
            throw exception(WAVE_ORDER_NO_DUPLICATE);
        }

        // 4. 构造波次单主表
        WmsWaveOrderDO waveOrder = BeanUtils.toBean(createReqVO, WmsWaveOrderDO.class);
        waveOrder.setNo(waveNo);
        waveOrder.setStatus(WmsOrderStatusEnum.PREPARE.getStatus());
        if (waveOrder.getOrderTime() == null) {
            waveOrder.setOrderTime(LocalDateTime.now());
        }

        // 5. 展开明细：从所有出库单明细按 SKU 聚合
        List<WmsWaveOrderDetailDO> detailList = new ArrayList<>();
        Set<Long> skuSet = new HashSet<>();
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (WmsShipmentOrderDO shipment : shipmentOrders) {
            // 校验出库单仓库与波次仓库一致
            if (!shipment.getWarehouseId().equals(createReqVO.getWarehouseId())) {
                throw exception(WAVE_ORDER_DETAIL_REQUIRED);
            }
            List<WmsShipmentOrderDetailDO> shipmentDetails =
                    shipmentOrderDetailMapper.selectList(WmsShipmentOrderDetailDO::getOrderId, shipment.getId());
            for (WmsShipmentOrderDetailDO sd : shipmentDetails) {
                WmsWaveOrderDetailDO wd = WmsWaveOrderDetailDO.builder()
                        .shipmentOrderId(shipment.getId())
                        .skuId(sd.getSkuId())
                        .pickQuantity(sd.getQuantity())
                        .pickedQuantity(BigDecimal.ZERO)
                        .build();
                detailList.add(wd);
                skuSet.add(sd.getSkuId());
                totalQuantity = totalQuantity.add(sd.getQuantity() == null ? BigDecimal.ZERO : sd.getQuantity());
                totalPrice = totalPrice.add(sd.getTotalPrice() == null ? BigDecimal.ZERO : sd.getTotalPrice());
            }
        }

        // 6. 写入主表
        waveOrder.setShipmentCount(shipmentOrders.size());
        waveOrder.setSkuCount(skuSet.size());
        waveOrder.setTotalQuantity(totalQuantity);
        waveOrder.setTotalPrice(totalPrice);
        waveOrderMapper.insert(waveOrder);

        // 7. 写入明细
        if (!detailList.isEmpty()) {
            detailList.forEach(d -> d.setWaveOrderId(waveOrder.getId()));
            waveOrderDetailMapper.insertBatch(detailList);
        }

        log.info("[createWaveOrder][波次单生成成功 no={}, shipmentCount={}, skuCount={}]",
                waveNo, shipmentOrders.size(), skuSet.size());
        return waveOrder.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWaveOrder(WmsWaveOrderSaveReqVO updateReqVO) {
        WmsWaveOrderDO existing = validateWaveOrderExists(updateReqVO.getId());
        if (!existing.getStatus().equals(WmsOrderStatusEnum.PREPARE.getStatus())) {
            throw exception(WAVE_ORDER_STATUS_NOT_PREPARE);
        }
        // 简化：仅更新备注、拣货员等基础字段，明细重新生成
        waveOrderDetailMapper.deleteByWaveOrderId(updateReqVO.getId());

        WmsWaveOrderDO update = BeanUtils.toBean(updateReqVO, WmsWaveOrderDO.class);
        waveOrderMapper.updateById(update);

        // 重新展开明细（与创建逻辑一致）
        if (CollUtil.isNotEmpty(updateReqVO.getShipmentOrderIds())) {
            List<WmsShipmentOrderDO> shipmentOrders =
                    shipmentOrderMapper.selectBatchIds(updateReqVO.getShipmentOrderIds());
            List<WmsWaveOrderDetailDO> detailList = new ArrayList<>();
            for (WmsShipmentOrderDO shipment : shipmentOrders) {
                List<WmsShipmentOrderDetailDO> shipmentDetails =
                        shipmentOrderDetailMapper.selectList(WmsShipmentOrderDetailDO::getOrderId, shipment.getId());
                for (WmsShipmentOrderDetailDO sd : shipmentDetails) {
                    detailList.add(WmsWaveOrderDetailDO.builder()
                            .waveOrderId(updateReqVO.getId())
                            .shipmentOrderId(shipment.getId())
                            .skuId(sd.getSkuId())
                            .pickQuantity(sd.getQuantity())
                            .pickedQuantity(BigDecimal.ZERO)
                            .build());
                }
            }
            if (!detailList.isEmpty()) {
                waveOrderDetailMapper.insertBatch(detailList);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWaveOrder(Long id) {
        WmsWaveOrderDO existing = validateWaveOrderExists(id);
        if (!existing.getStatus().equals(WmsOrderStatusEnum.PREPARE.getStatus())
                && !existing.getStatus().equals(WmsOrderStatusEnum.CANCELED.getStatus())) {
            throw exception(WAVE_ORDER_STATUS_NOT_DELETABLE);
        }
        waveOrderDetailMapper.deleteByWaveOrderId(id);
        waveOrderMapper.deleteById(id);
    }

    @Override
    public WmsWaveOrderDO getWaveOrder(Long id) {
        return waveOrderMapper.selectById(id);
    }

    @Override
    public PageResult<WmsWaveOrderDO> getWaveOrderPage(WmsWaveOrderPageReqVO pageReqVO) {
        return waveOrderMapper.selectPage(pageReqVO);
    }

    @Override
    public List<WmsWaveOrderDetailDO> getWaveOrderDetailListByWaveOrderId(Long waveOrderId) {
        return waveOrderDetailMapper.selectListByWaveOrderId(waveOrderId);
    }

    // ====== 内部工具方法 ======

    private WmsWaveOrderDO validateWaveOrderExists(Long id) {
        WmsWaveOrderDO waveOrder = waveOrderMapper.selectById(id);
        if (waveOrder == null) {
            throw exception(WAVE_ORDER_NOT_EXISTS);
        }
        return waveOrder;
    }

    /**
     * 生成波次单号：WAVE_yyyyMMddHHmmss_4位随机数
     */
    private String generateWaveNo() {
        String timePart = LocalDateTime.now().format(NO_FMT);
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return StrUtil.format("WAVE_{}_{}", timePart, random);
    }

}
