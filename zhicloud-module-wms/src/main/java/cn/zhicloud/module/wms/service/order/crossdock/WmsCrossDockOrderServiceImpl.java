package cn.zhicloud.module.wms.service.order.crossdock;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.number.MoneyUtils;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.wms.controller.admin.order.crossdock.vo.WmsCrossDockOrderDetailSaveReqVO;
import cn.zhicloud.module.wms.controller.admin.order.crossdock.vo.WmsCrossDockOrderPageReqVO;
import cn.zhicloud.module.wms.controller.admin.order.crossdock.vo.WmsCrossDockOrderSaveReqVO;
import cn.zhicloud.module.wms.dal.dataobject.order.crossdock.WmsCrossDockOrderDO;
import cn.zhicloud.module.wms.dal.mysql.order.crossdock.WmsCrossDockOrderMapper;
import cn.zhicloud.module.wms.service.md.merchant.WmsMerchantService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.*;

/**
 * WMS 越库单 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class WmsCrossDockOrderServiceImpl implements WmsCrossDockOrderService {

    /**
     * 越库单状态：10 待收货
     */
    public static final int STATUS_PENDING_RECEIPT = 10;
    /**
     * 越库单状态：20 已收货
     */
    public static final int STATUS_RECEIVED = 20;
    /**
     * 越库单状态：30 已分配（已分配至出库月台）
     */
    public static final int STATUS_ALLOCATED = 30;
    /**
     * 越库单状态：40 已完成
     */
    public static final int STATUS_COMPLETED = 40;
    /**
     * 越库单状态：50 已取消
     */
    public static final int STATUS_CANCELED = 50;

    @Resource
    private WmsCrossDockOrderMapper crossDockOrderMapper;
    @Resource
    private WmsCrossDockOrderDetailService crossDockOrderDetailService;
    @Resource
    private WmsMerchantService merchantService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCrossDockOrder(WmsCrossDockOrderSaveReqVO createReqVO) {
        // 1. 校验越库单保存数据
        validateCrossDockOrderSaveData(createReqVO);

        // 2.1 插入越库单
        WmsCrossDockOrderDO order = BeanUtils.toBean(createReqVO, WmsCrossDockOrderDO.class);
        order.setStatus(STATUS_PENDING_RECEIPT);
        fillCrossDockOrderTotal(order, createReqVO);
        crossDockOrderMapper.insert(order);
        // 2.2 插入越库单明细
        crossDockOrderDetailService.createCrossDockOrderDetailList(order.getId(), createReqVO);
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCrossDockOrder(WmsCrossDockOrderSaveReqVO updateReqVO) {
        // 1. 校验越库单保存数据
        validateCrossDockOrderPrepare(updateReqVO.getId());
        validateCrossDockOrderSaveData(updateReqVO);

        // 2.1 更新越库单
        WmsCrossDockOrderDO updateObj = BeanUtils.toBean(updateReqVO, WmsCrossDockOrderDO.class)
                .setStatus(STATUS_PENDING_RECEIPT);
        fillCrossDockOrderTotal(updateObj, updateReqVO);
        int updateCount = crossDockOrderMapper.updateByIdAndStatus(updateReqVO.getId(),
                STATUS_PENDING_RECEIPT, updateObj);
        if (updateCount == 0) {
            throw exception(CROSS_DOCK_ORDER_STATUS_NOT_PREPARE);
        }
        // 2.2 更新越库单明细
        crossDockOrderDetailService.updateCrossDockOrderDetailList(updateReqVO.getId(), updateReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCrossDockOrder(Long id) {
        // 1. 校验存在，且可删除
        WmsCrossDockOrderDO order = validateCrossDockOrderExists(id);
        if (ObjectUtil.notEqual(order.getStatus(), STATUS_PENDING_RECEIPT)
                && ObjectUtil.notEqual(order.getStatus(), STATUS_CANCELED)) {
            throw exception(CROSS_DOCK_ORDER_STATUS_NOT_DELETABLE);
        }

        // 2.1 删除越库单
        crossDockOrderMapper.deleteById(id);
        // 2.2 删除越库单明细
        crossDockOrderDetailService.deleteCrossDockOrderDetailListByOrderId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceipt(Long id) {
        // 1. 校验存在，且为待收货状态
        WmsCrossDockOrderDO order = validateCrossDockOrderExists(id);
        if (ObjectUtil.notEqual(order.getStatus(), STATUS_PENDING_RECEIPT)) {
            throw exception(CROSS_DOCK_ORDER_STATUS_NOT_RECEIVABLE);
        }
        // 1.2 校验越库单明细存在
        crossDockOrderDetailService.validateCrossDockOrderDetailListExists(id);

        // 2. 越库核心：确认收货 → 跳过上架 → 直接分配至出库月台
        //    状态由 待收货(10) 流转至 已分配(30)，不经过 上架/已收货 中间环节
        if (crossDockOrderMapper.updateByIdAndStatus(id, STATUS_PENDING_RECEIPT,
                new WmsCrossDockOrderDO().setStatus(STATUS_ALLOCATED)) == 0) {
            throw exception(CROSS_DOCK_ORDER_STATUS_NOT_RECEIVABLE);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeCrossDockOrder(Long id) {
        // 1. 校验存在，且为已分配状态
        WmsCrossDockOrderDO order = validateCrossDockOrderExists(id);
        if (ObjectUtil.notEqual(order.getStatus(), STATUS_ALLOCATED)) {
            throw exception(CROSS_DOCK_ORDER_STATUS_NOT_COMPLETABLE);
        }

        // 2. 完成越库单：已分配(30) → 已完成(40)
        if (crossDockOrderMapper.updateByIdAndStatus(id, STATUS_ALLOCATED,
                new WmsCrossDockOrderDO().setStatus(STATUS_COMPLETED)) == 0) {
            throw exception(CROSS_DOCK_ORDER_STATUS_NOT_COMPLETABLE);
        }
    }

    @Override
    public WmsCrossDockOrderDO getCrossDockOrder(Long id) {
        return crossDockOrderMapper.selectById(id);
    }

    @Override
    public PageResult<WmsCrossDockOrderDO> getCrossDockOrderPage(WmsCrossDockOrderPageReqVO pageReqVO) {
        return crossDockOrderMapper.selectPage(pageReqVO);
    }

    private void validateCrossDockOrderSaveData(WmsCrossDockOrderSaveReqVO reqVO) {
        // 校验越库单号唯一
        validateCrossDockOrderNoUnique(reqVO.getId(), reqVO.getNo());
        // 校验供应商类型
        if (reqVO.getSourceSupplierId() != null) {
            merchantService.validateSupplierMerchantExists(reqVO.getSourceSupplierId());
        }
        // 校验客户类型
        if (reqVO.getTargetCustomerId() != null) {
            merchantService.validateCustomerMerchantExists(reqVO.getTargetCustomerId());
        }
    }

    private void validateCrossDockOrderNoUnique(Long id, String no) {
        WmsCrossDockOrderDO order = crossDockOrderMapper.selectByNo(no);
        if (order == null) {
            return;
        }
        if (id == null || ObjectUtil.notEqual(order.getId(), id)) {
            throw exception(CROSS_DOCK_ORDER_NO_DUPLICATE);
        }
    }

    private void fillCrossDockOrderTotal(WmsCrossDockOrderDO order, WmsCrossDockOrderSaveReqVO reqVO) {
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (CollUtil.isNotEmpty(reqVO.getDetails())) {
            for (WmsCrossDockOrderDetailSaveReqVO detail : reqVO.getDetails()) {
                if (detail.getQuantity() != null) {
                    totalQuantity = totalQuantity.add(detail.getQuantity());
                }
                BigDecimal detailAmount = getDetailAmount(detail.getQuantity(), detail.getUnitPrice(),
                        detail.getAmount());
                if (detailAmount != null) {
                    totalAmount = totalAmount.add(detailAmount);
                }
            }
        }
        order.setTotalQuantity(totalQuantity).setTotalAmount(totalAmount);
    }

    private static BigDecimal getDetailAmount(BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount) {
        if (amount != null) {
            return amount;
        }
        return MoneyUtils.priceMultiply(unitPrice, quantity);
    }

    private WmsCrossDockOrderDO validateCrossDockOrderExists(Long id) {
        WmsCrossDockOrderDO order = id == null ? null : crossDockOrderMapper.selectById(id);
        if (order == null) {
            throw exception(CROSS_DOCK_ORDER_NOT_EXISTS);
        }
        return order;
    }

    /**
     * 校验越库单存在且为待收货状态
     *
     * @param id 越库单编号
     * @return 越库单
     */
    private WmsCrossDockOrderDO validateCrossDockOrderPrepare(Long id) {
        WmsCrossDockOrderDO order = validateCrossDockOrderExists(id);
        if (ObjectUtil.notEqual(order.getStatus(), STATUS_PENDING_RECEIPT)) {
            throw exception(CROSS_DOCK_ORDER_STATUS_NOT_PREPARE);
        }
        return order;
    }

}
