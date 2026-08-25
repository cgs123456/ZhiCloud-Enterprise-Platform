package cn.zhicloud.module.wms.service.order.crossdock;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.zhicloud.framework.common.util.number.MoneyUtils;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.wms.controller.admin.order.crossdock.vo.WmsCrossDockOrderSaveReqVO;
import cn.zhicloud.module.wms.dal.dataobject.order.crossdock.WmsCrossDockOrderDetailDO;
import cn.zhicloud.module.wms.dal.mysql.order.crossdock.WmsCrossDockOrderDetailMapper;
import cn.zhicloud.module.wms.service.md.item.WmsItemSkuService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.framework.common.util.collection.CollectionUtils.convertList;
import static cn.zhicloud.framework.common.util.collection.CollectionUtils.diffList;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.*;

/**
 * WMS 越库单明细 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class WmsCrossDockOrderDetailServiceImpl implements WmsCrossDockOrderDetailService {

    @Resource
    private WmsCrossDockOrderDetailMapper crossDockOrderDetailMapper;
    @Resource
    private WmsItemSkuService itemSkuService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createCrossDockOrderDetailList(Long orderId, WmsCrossDockOrderSaveReqVO reqVO) {
        List<WmsCrossDockOrderDetailDO> list = buildCrossDockOrderDetailList(reqVO);
        if (CollUtil.isEmpty(list)) {
            return;
        }
        list.forEach(detail -> detail.setId(null).setOrderId(orderId));
        crossDockOrderDetailMapper.insertBatch(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCrossDockOrderDetailList(Long orderId, WmsCrossDockOrderSaveReqVO reqVO) {
        // 第一步，对比新老数据，获得添加、修改、删除的列表
        List<WmsCrossDockOrderDetailDO> oldList = crossDockOrderDetailMapper.selectListByOrderId(orderId);
        List<WmsCrossDockOrderDetailDO> list = buildCrossDockOrderDetailList(reqVO);
        List<WmsCrossDockOrderDetailDO> newList = CollUtil.isEmpty(list) ? ListUtil.of() : list;
        List<List<WmsCrossDockOrderDetailDO>> diffList = diffList(oldList, newList, // id 不同，就认为是不同的记录
                (oldVal, newVal) -> ObjectUtil.equal(oldVal.getId(), newVal.getId()));

        // 第二步，批量添加、修改、删除
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            if (CollUtil.isNotEmpty(convertList(diffList.get(0), WmsCrossDockOrderDetailDO::getId))) {
                throw exception(CROSS_DOCK_ORDER_DETAIL_NOT_EXISTS);
            }
            diffList.get(0).forEach(detail -> detail.setOrderId(orderId));
            crossDockOrderDetailMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            diffList.get(1).forEach(detail -> detail.setOrderId(orderId));
            crossDockOrderDetailMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            crossDockOrderDetailMapper.deleteByIds(convertList(diffList.get(2), WmsCrossDockOrderDetailDO::getId));
        }
    }

    @Override
    public void deleteCrossDockOrderDetailListByOrderId(Long orderId) {
        crossDockOrderDetailMapper.deleteByOrderId(orderId);
    }

    @Override
    public List<WmsCrossDockOrderDetailDO> getCrossDockOrderDetailList(Long orderId) {
        return crossDockOrderDetailMapper.selectListByOrderId(orderId);
    }

    @Override
    public List<WmsCrossDockOrderDetailDO> getCrossDockOrderDetailList(Collection<Long> orderIds) {
        if (CollUtil.isEmpty(orderIds)) {
            return ListUtil.of();
        }
        return crossDockOrderDetailMapper.selectListByOrderIds(orderIds);
    }

    @Override
    public List<WmsCrossDockOrderDetailDO> validateCrossDockOrderDetailListExists(Long orderId) {
        List<WmsCrossDockOrderDetailDO> details = crossDockOrderDetailMapper.selectListByOrderId(orderId);
        if (CollUtil.isEmpty(details)) {
            throw exception(CROSS_DOCK_ORDER_DETAIL_REQUIRED);
        }
        return details;
    }

    private List<WmsCrossDockOrderDetailDO> buildCrossDockOrderDetailList(WmsCrossDockOrderSaveReqVO reqVO) {
        if (CollUtil.isEmpty(reqVO.getDetails())) {
            return ListUtil.of();
        }
        return convertList(reqVO.getDetails(), detail -> {
            // 校验 SKU 存在
            itemSkuService.validateItemSkuExists(detail.getSkuId());
            // 构建对象
            WmsCrossDockOrderDetailDO detailDO = BeanUtils.toBean(detail, WmsCrossDockOrderDetailDO.class);
            fillDetailAmount(detailDO);
            return detailDO;
        });
    }

    private static void fillDetailAmount(WmsCrossDockOrderDetailDO detail) {
        if (detail.getAmount() != null || detail.getQuantity() == null || detail.getUnitPrice() == null) {
            return;
        }
        detail.setAmount(MoneyUtils.priceMultiply(detail.getUnitPrice(), detail.getQuantity()));
    }

}
