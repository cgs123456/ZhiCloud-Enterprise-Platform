package cn.zhicloud.module.wms.service.inventory.alert;

import cn.hutool.core.util.ObjectUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.wms.controller.admin.inventory.alert.vo.WmsSafetyStockConfigPageReqVO;
import cn.zhicloud.module.wms.controller.admin.inventory.alert.vo.WmsSafetyStockConfigSaveReqVO;
import cn.zhicloud.module.wms.dal.dataobject.inventory.WmsSafetyStockConfigDO;
import cn.zhicloud.module.wms.dal.mysql.inventory.WmsSafetyStockConfigMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.SAFETY_STOCK_CONFIG_DUPLICATE;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.SAFETY_STOCK_CONFIG_NOT_EXISTS;

/**
 * WMS 安全库存配置 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class WmsSafetyStockConfigServiceImpl implements WmsSafetyStockConfigService {

    @Resource
    private WmsSafetyStockConfigMapper safetyStockConfigMapper;

    @Override
    public Long createSafetyStockConfig(WmsSafetyStockConfigSaveReqVO createReqVO) {
        // 校验同一仓库 + SKU 唯一
        validateWarehouseProductUnique(null, createReqVO.getWarehouseId(), createReqVO.getProductId());
        // 插入
        WmsSafetyStockConfigDO config = BeanUtils.toBean(createReqVO, WmsSafetyStockConfigDO.class);
        safetyStockConfigMapper.insert(config);
        return config.getId();
    }

    @Override
    public void updateSafetyStockConfig(WmsSafetyStockConfigSaveReqVO updateReqVO) {
        validateSafetyStockConfigExists(updateReqVO.getId());
        validateWarehouseProductUnique(updateReqVO.getId(), updateReqVO.getWarehouseId(), updateReqVO.getProductId());
        WmsSafetyStockConfigDO updateObj = BeanUtils.toBean(updateReqVO, WmsSafetyStockConfigDO.class);
        safetyStockConfigMapper.updateById(updateObj);
    }

    @Override
    public void deleteSafetyStockConfig(Long id) {
        validateSafetyStockConfigExists(id);
        safetyStockConfigMapper.deleteById(id);
    }

    @Override
    public WmsSafetyStockConfigDO getSafetyStockConfig(Long id) {
        return safetyStockConfigMapper.selectById(id);
    }

    @Override
    public PageResult<WmsSafetyStockConfigDO> getSafetyStockConfigPage(WmsSafetyStockConfigPageReqVO pageReqVO) {
        return safetyStockConfigMapper.selectPage(pageReqVO);
    }

    @Override
    public List<WmsSafetyStockConfigDO> getSafetyStockConfigList() {
        return safetyStockConfigMapper.selectList();
    }

    @Override
    public WmsSafetyStockConfigDO getSafetyStockConfigByWarehouseAndProduct(Long warehouseId, Long productId) {
        return safetyStockConfigMapper.selectByWarehouseIdAndProductId(warehouseId, productId);
    }

    @Override
    public List<WmsSafetyStockConfigDO> getSafetyStockConfigListByProductIds(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyList();
        }
        return safetyStockConfigMapper.selectListByProductIds(productIds);
    }

    @Override
    public WmsSafetyStockConfigDO validateSafetyStockConfigExists(Long id) {
        WmsSafetyStockConfigDO config = safetyStockConfigMapper.selectById(id);
        if (config == null) {
            throw exception(SAFETY_STOCK_CONFIG_NOT_EXISTS);
        }
        return config;
    }

    private void validateWarehouseProductUnique(Long id, Long warehouseId, Long productId) {
        WmsSafetyStockConfigDO existing = safetyStockConfigMapper.selectByWarehouseIdAndProductId(warehouseId, productId);
        if (existing == null) {
            return;
        }
        if (id == null || ObjectUtil.notEqual(existing.getId(), id)) {
            throw exception(SAFETY_STOCK_CONFIG_DUPLICATE);
        }
    }

}