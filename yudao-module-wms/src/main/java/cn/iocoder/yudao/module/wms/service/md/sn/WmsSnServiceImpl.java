package cn.iocoder.yudao.module.wms.service.md.sn;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.wms.controller.admin.md.sn.vo.WmsSnGenerateReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.md.sn.vo.WmsSnPageReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.md.sn.vo.WmsSnSaveReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.md.sn.vo.WmsSnTraceRespVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.item.WmsItemDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.sn.WmsSnDO;
import cn.iocoder.yudao.module.wms.dal.mysql.md.sn.WmsSnMapper;
import cn.iocoder.yudao.module.wms.enums.md.WmsSnStatusEnum;
import cn.iocoder.yudao.module.wms.service.md.item.WmsItemService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.ITEM_NOT_EXISTS;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.SN_ALREADY_BOUND;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.SN_DUPLICATE;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.SN_GENERATE_QUANTITY_INVALID;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.SN_NOT_BOUND;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.SN_NOT_EXISTS;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.SN_STATUS_INVALID;

/**
 * WMS 序列号 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class WmsSnServiceImpl implements WmsSnService {

    /**
     * 默认序列号前缀
     */
    private static final String DEFAULT_PREFIX = "SN";

    /**
     * 日期格式（yyyyMMdd）
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 流水号位数
     */
    private static final int SEQ_WIDTH = 4;

    @Resource
    private WmsSnMapper snMapper;
    @Resource
    private WmsItemService itemService;

    // ==================== 生成 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<WmsSnDO> generateSnList(WmsSnGenerateReqVO reqVO) {
        // 1. 校验数量
        int quantity = reqVO.getQuantity() == null ? 0 : reqVO.getQuantity().intValue();
        if (quantity <= 0) {
            throw exception(SN_GENERATE_QUANTITY_INVALID);
        }
        // 2. 校验商品存在
        validateProductExists(reqVO.getProductId());
        // 3. 生成序列号：前缀 + 日期 + 流水号
        String prefix = (reqVO.getPrefix() == null || reqVO.getPrefix().isEmpty()) ? DEFAULT_PREFIX : reqVO.getPrefix();
        String dateStr = LocalDateTime.now().format(DATE_FORMATTER);
        int startSeq = resolveStartSequence(prefix, dateStr);
        List<WmsSnDO> result = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            String sn = prefix + dateStr + String.format("%0" + SEQ_WIDTH + "d", startSeq + i);
            WmsSnDO snDO = WmsSnDO.builder()
                    .sn(sn)
                    .productId(reqVO.getProductId())
                    .status(WmsSnStatusEnum.GENERATED.getStatus())
                    .build();
            snMapper.insert(snDO);
            result.add(snDO);
        }
        return result;
    }

    /**
     * 解析起始流水号：基于当天已存在的同前缀序列号数量 + 1
     */
    private int resolveStartSequence(String prefix, String dateStr) {
        String likePattern = prefix + dateStr;
        List<WmsSnDO> existing = snMapper.selectList(new LambdaQueryWrapperX<WmsSnDO>()
                .likeRight(WmsSnDO::getSn, likePattern));
        return existing.size() + 1;
    }

    // ==================== CRUD ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSn(WmsSnSaveReqVO createReqVO) {
        // 校验序列号唯一
        validateSnUnique(null, createReqVO.getSn());
        if (createReqVO.getProductId() != null) {
            validateProductExists(createReqVO.getProductId());
        }
        WmsSnDO sn = BeanUtils.toBean(createReqVO, WmsSnDO.class);
        if (sn.getStatus() == null) {
            sn.setStatus(WmsSnStatusEnum.GENERATED.getStatus());
        }
        snMapper.insert(sn);
        return sn.getId();
    }

    @Override
    public void updateSn(WmsSnSaveReqVO updateReqVO) {
        validateSnExists(updateReqVO.getId());
        validateSnUnique(updateReqVO.getId(), updateReqVO.getSn());
        WmsSnDO updateObj = BeanUtils.toBean(updateReqVO, WmsSnDO.class);
        snMapper.updateById(updateObj);
    }

    @Override
    public void deleteSn(Long id) {
        WmsSnDO sn = validateSnExists(id);
        // 仅允许删除「已生成」状态的序列号
        if (!WmsSnStatusEnum.GENERATED.getStatus().equals(sn.getStatus())) {
            throw exception(SN_STATUS_INVALID, sn.getStatus());
        }
        snMapper.deleteById(id);
    }

    @Override
    public WmsSnDO validateSnExists(Long id) {
        WmsSnDO sn = snMapper.selectById(id);
        if (sn == null) {
            throw exception(SN_NOT_EXISTS);
        }
        return sn;
    }

    @Override
    public WmsSnDO getSn(Long id) {
        return snMapper.selectById(id);
    }

    @Override
    public WmsSnDO getSnBySn(String sn) {
        return snMapper.selectBySn(sn);
    }

    @Override
    public PageResult<WmsSnDO> getSnPage(WmsSnPageReqVO pageReqVO) {
        return snMapper.selectPage(pageReqVO);
    }

    @Override
    public List<WmsSnDO> getSnList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return snMapper.selectByIds(ids);
    }

    // ==================== 生命周期（状态机） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindInventory(Long snId, Long inventoryId, Long batchId, Long warehouseId,
                              Long zoneId, Long locationId, Long inboundOrderId) {
        WmsSnDO sn = validateSnExists(snId);
        // 仅允许「已生成 / 已绑定」状态绑定
        String status = sn.getStatus();
        if (!WmsSnStatusEnum.GENERATED.getStatus().equals(status)
                && !WmsSnStatusEnum.BOUND.getStatus().equals(status)) {
            throw exception(SN_STATUS_INVALID, status);
        }
        WmsSnDO updateObj = new WmsSnDO().setId(snId)
                .setInventoryId(inventoryId)
                .setBatchId(batchId)
                .setWarehouseId(warehouseId)
                .setZoneId(zoneId)
                .setLocationId(locationId)
                .setInboundOrderId(inboundOrderId)
                .setBoundTime(LocalDateTime.now());
        // 已绑定库位视为在库，否则停留在「已绑定」状态
        updateObj.setStatus(locationId != null
                ? WmsSnStatusEnum.IN_STOCK.getStatus()
                : WmsSnStatusEnum.BOUND.getStatus());
        snMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindAndShip(Long snId, Long outboundOrderId) {
        WmsSnDO sn = validateSnExists(snId);
        String status = sn.getStatus();
        // 仅允许「在库 / 已绑定」状态出库
        if (!WmsSnStatusEnum.IN_STOCK.getStatus().equals(status)
                && !WmsSnStatusEnum.BOUND.getStatus().equals(status)) {
            if (WmsSnStatusEnum.GENERATED.getStatus().equals(status)) {
                throw exception(SN_NOT_BOUND);
            }
            throw exception(SN_STATUS_INVALID, status);
        }
        WmsSnDO updateObj = new WmsSnDO().setId(snId)
                .setOutboundOrderId(outboundOrderId)
                .setShippedTime(LocalDateTime.now())
                .setStatus(WmsSnStatusEnum.SHIPPED.getStatus());
        snMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnSn(Long snId, Long warehouseId, Long locationId) {
        WmsSnDO sn = validateSnExists(snId);
        String status = sn.getStatus();
        // 仅允许「已出库」状态退货
        if (!WmsSnStatusEnum.SHIPPED.getStatus().equals(status)) {
            throw exception(SN_STATUS_INVALID, status);
        }
        WmsSnDO updateObj = new WmsSnDO().setId(snId)
                .setWarehouseId(warehouseId)
                .setLocationId(locationId)
                .setStatus(WmsSnStatusEnum.RETURNED.getStatus());
        snMapper.updateById(updateObj);
    }

    // ==================== 追溯 ====================

    @Override
    public WmsSnTraceRespVO trace(Long snId) {
        WmsSnDO sn = validateSnExists(snId);
        WmsSnTraceRespVO respVO = BeanUtils.toBean(sn, WmsSnTraceRespVO.class);
        // 拼接商品信息
        if (sn.getProductId() != null) {
            WmsItemDO item = itemService.getItem(sn.getProductId());
            if (item != null) {
                respVO.setProductCode(item.getCode());
                respVO.setProductName(item.getName());
            }
        }
        return respVO;
    }

    // ==================== 校验方法 ====================

    private void validateSnUnique(Long id, String sn) {
        if (sn == null || sn.isEmpty()) {
            return;
        }
        WmsSnDO existing = snMapper.selectBySn(sn);
        if (existing == null) {
            return;
        }
        if (id == null || ObjectUtil.notEqual(existing.getId(), id)) {
            throw exception(SN_DUPLICATE, sn);
        }
    }

    private void validateProductExists(Long productId) {
        WmsItemDO item = itemService.getItem(productId);
        if (item == null) {
            throw exception(ITEM_NOT_EXISTS);
        }
    }

}