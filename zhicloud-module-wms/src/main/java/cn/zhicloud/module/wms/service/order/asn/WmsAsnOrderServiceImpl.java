package cn.zhicloud.module.wms.service.order.asn;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.wms.controller.admin.order.asn.vo.WmsAsnOrderDetailSaveReqVO;
import cn.zhicloud.module.wms.controller.admin.order.asn.vo.WmsAsnOrderPageReqVO;
import cn.zhicloud.module.wms.controller.admin.order.asn.vo.WmsAsnOrderSaveReqVO;
import cn.zhicloud.module.wms.controller.admin.order.receipt.vo.detail.WmsReceiptOrderDetailSaveReqVO;
import cn.zhicloud.module.wms.controller.admin.order.receipt.vo.order.WmsReceiptOrderSaveReqVO;
import cn.zhicloud.module.wms.dal.dataobject.order.asn.WmsAsnOrderDO;
import cn.zhicloud.module.wms.dal.dataobject.order.asn.WmsAsnOrderDetailDO;
import cn.zhicloud.module.wms.dal.dataobject.order.dock.WmsDockDO;
import cn.zhicloud.module.wms.dal.mysql.order.asn.WmsAsnOrderMapper;
import cn.zhicloud.module.wms.dal.mysql.order.dock.WmsDockMapper;
import cn.zhicloud.module.wms.enums.order.WmsReceiptOrderTypeEnum;
import cn.zhicloud.module.wms.service.md.warehouse.WmsWarehouseService;
import cn.zhicloud.module.wms.service.order.dock.WmsDockService;
import cn.zhicloud.module.wms.service.order.receipt.WmsReceiptOrderService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.ASN_ORDER_NOT_EXISTS;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.ASN_ORDER_NO_DUPLICATE;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.ASN_ORDER_STATUS_NOT_ARRIVED;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.ASN_ORDER_STATUS_NOT_PENDING;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.ASN_ORDER_DETAIL_REQUIRED;

/**
 * WMS ASN 到货通知单 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class WmsAsnOrderServiceImpl implements WmsAsnOrderService {

    /**
     * ASN 状态：待到货
     */
    private static final int STATUS_PENDING = 10;
    /**
     * ASN 状态：已到货
     */
    private static final int STATUS_ARRIVED = 20;
    /**
     * ASN 状态：已收货
     */
    private static final int STATUS_RECEIVED = 30;
    /**
     * ASN 状态：已关闭
     */
    private static final int STATUS_CLOSED = 50;
    /**
     * 月台状态：占用
     */
    private static final int DOCK_STATUS_OCCUPIED = 20;

    @Resource
    private WmsAsnOrderMapper asnOrderMapper;
    @Resource
    private WmsAsnOrderDetailService asnOrderDetailService;
    @Resource
    private WmsDockService dockService;
    @Resource
    private WmsDockMapper dockMapper;
    @Resource
    private WmsWarehouseService warehouseService;
    /**
     * 收货单 Service，用于转收货单。使用 @Lazy 避免潜在的循环依赖。
     */
    @Resource
    @Lazy
    private WmsReceiptOrderService receiptOrderService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAsnOrder(WmsAsnOrderSaveReqVO createReqVO) {
        // 1. 校验
        validateAsnNoUnique(null, createReqVO.getNo());
        warehouseService.validateWarehouseExists(createReqVO.getWarehouseId());
        // 2. 插入 ASN 单
        WmsAsnOrderDO order = BeanUtils.toBean(createReqVO, WmsAsnOrderDO.class);
        order.setStatus(STATUS_PENDING);
        fillAsnOrderTotal(order, createReqVO);
        asnOrderMapper.insert(order);
        // 3. 插入明细
        asnOrderDetailService.createAsnOrderDetailList(order.getId(), createReqVO.getDetails());
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAsnOrder(WmsAsnOrderSaveReqVO updateReqVO) {
        // 1. 校验存在且为待到货
        WmsAsnOrderDO order = validateAsnOrderExists(updateReqVO.getId());
        if (ObjectUtil.notEqual(order.getStatus(), STATUS_PENDING)) {
            throw exception(ASN_ORDER_STATUS_NOT_PENDING);
        }
        // 2. 校验编号唯一
        validateAsnNoUnique(updateReqVO.getId(), updateReqVO.getNo());
        warehouseService.validateWarehouseExists(updateReqVO.getWarehouseId());
        // 3. 更新
        WmsAsnOrderDO updateObj = BeanUtils.toBean(updateReqVO, WmsAsnOrderDO.class);
        fillAsnOrderTotal(updateObj, updateReqVO);
        asnOrderMapper.updateById(updateObj);
        // 4. 更新明细
        asnOrderDetailService.updateAsnOrderDetailList(updateReqVO.getId(), updateReqVO.getDetails());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAsnOrder(Long id) {
        WmsAsnOrderDO order = validateAsnOrderExists(id);
        if (ObjectUtil.notEqual(order.getStatus(), STATUS_PENDING)
                && ObjectUtil.notEqual(order.getStatus(), STATUS_CLOSED)) {
            throw exception(ASN_ORDER_STATUS_NOT_PENDING);
        }
        asnOrderMapper.deleteById(id);
        asnOrderDetailService.deleteAsnOrderDetailListByAsnOrderId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeAsnOrder(Long id) {
        WmsAsnOrderDO order = validateAsnOrderExists(id);
        if (ObjectUtil.notEqual(order.getStatus(), STATUS_PENDING)) {
            throw exception(ASN_ORDER_STATUS_NOT_PENDING);
        }
        WmsAsnOrderDO updateObj = new WmsAsnOrderDO();
        updateObj.setId(id);
        updateObj.setStatus(STATUS_CLOSED);
        asnOrderMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmArrival(Long id) {
        // 1. 校验存在且为待到货
        WmsAsnOrderDO order = validateAsnOrderExists(id);
        if (ObjectUtil.notEqual(order.getStatus(), STATUS_PENDING)) {
            throw exception(ASN_ORDER_STATUS_NOT_PENDING);
        }
        // 2. 更新 ASN 状态为已到货，记录实际到货时间
        WmsAsnOrderDO updateObj = new WmsAsnOrderDO();
        updateObj.setId(id);
        updateObj.setStatus(STATUS_ARRIVED);
        updateObj.setActualArrivalTime(LocalDateTime.now());
        asnOrderMapper.updateById(updateObj);
        // 3. 将月台置为占用
        if (order.getDockId() != null) {
            dockService.validateDockExists(order.getDockId());
            WmsDockDO dockUpdate = new WmsDockDO();
            dockUpdate.setId(order.getDockId());
            dockUpdate.setStatus(DOCK_STATUS_OCCUPIED);
            dockMapper.updateById(dockUpdate);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long convertToReceipt(Long id) {
        // 1. 校验存在且为已到货
        WmsAsnOrderDO order = validateAsnOrderExists(id);
        if (ObjectUtil.notEqual(order.getStatus(), STATUS_ARRIVED)) {
            throw exception(ASN_ORDER_STATUS_NOT_ARRIVED);
        }
        // 2. 获取明细
        List<WmsAsnOrderDetailDO> details = asnOrderDetailService.getAsnOrderDetailList(id);
        if (CollUtil.isEmpty(details)) {
            throw exception(ASN_ORDER_DETAIL_REQUIRED);
        }
        // 3. 构建收货单
        WmsReceiptOrderSaveReqVO receiptReqVO = new WmsReceiptOrderSaveReqVO();
        receiptReqVO.setNo("RK-ASN-" + order.getNo());
        receiptReqVO.setType(WmsReceiptOrderTypeEnum.PURCHASE.getType());
        receiptReqVO.setOrderTime(LocalDateTime.now());
        receiptReqVO.setWarehouseId(order.getWarehouseId());
        receiptReqVO.setMerchantId(order.getSupplierId());
        receiptReqVO.setRemark("由 ASN 单 " + order.getNo() + " 转换");
        List<WmsReceiptOrderDetailSaveReqVO> receiptDetails = new ArrayList<>();
        for (WmsAsnOrderDetailDO detail : details) {
            WmsReceiptOrderDetailSaveReqVO receiptDetail = new WmsReceiptOrderDetailSaveReqVO();
            receiptDetail.setSkuId(detail.getSkuId());
            receiptDetail.setQuantity(detail.getExpectedQuantity());
            receiptDetails.add(receiptDetail);
        }
        receiptReqVO.setDetails(receiptDetails);
        // 4. 创建收货单
        Long receiptOrderId = receiptOrderService.createReceiptOrder(receiptReqVO);
        // 5. 更新 ASN 状态为已收货
        WmsAsnOrderDO updateObj = new WmsAsnOrderDO();
        updateObj.setId(id);
        updateObj.setStatus(STATUS_RECEIVED);
        asnOrderMapper.updateById(updateObj);
        return receiptOrderId;
    }

    @Override
    public WmsAsnOrderDO getAsnOrder(Long id) {
        return asnOrderMapper.selectById(id);
    }

    @Override
    public WmsAsnOrderDO getAsnOrderByNo(String no) {
        return asnOrderMapper.selectByNo(no);
    }

    @Override
    public PageResult<WmsAsnOrderDO> getAsnOrderPage(WmsAsnOrderPageReqVO pageReqVO) {
        return asnOrderMapper.selectPage(pageReqVO);
    }

    // ==================== 校验与辅助方法 ====================

    private WmsAsnOrderDO validateAsnOrderExists(Long id) {
        WmsAsnOrderDO order = id == null ? null : asnOrderMapper.selectById(id);
        if (order == null) {
            throw exception(ASN_ORDER_NOT_EXISTS);
        }
        return order;
    }

    private void validateAsnNoUnique(Long id, String no) {
        WmsAsnOrderDO order = asnOrderMapper.selectByNo(no);
        if (order == null) {
            return;
        }
        if (id == null || !order.getId().equals(id)) {
            throw exception(ASN_ORDER_NO_DUPLICATE);
        }
    }

    private void fillAsnOrderTotal(WmsAsnOrderDO order, WmsAsnOrderSaveReqVO reqVO) {
        BigDecimal totalQuantity = BigDecimal.ZERO;
        if (CollUtil.isNotEmpty(reqVO.getDetails())) {
            for (WmsAsnOrderDetailSaveReqVO detail : reqVO.getDetails()) {
                if (detail.getExpectedQuantity() != null) {
                    totalQuantity = totalQuantity.add(detail.getExpectedQuantity());
                }
            }
        }
        order.setTotalQuantity(totalQuantity);
        if (reqVO.getTotalAmount() != null) {
            order.setTotalAmount(reqVO.getTotalAmount());
        }
    }

}
