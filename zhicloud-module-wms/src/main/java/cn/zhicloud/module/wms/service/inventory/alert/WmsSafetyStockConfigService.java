package cn.zhicloud.module.wms.service.inventory.alert;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.wms.controller.admin.inventory.alert.vo.WmsSafetyStockConfigPageReqVO;
import cn.zhicloud.module.wms.controller.admin.inventory.alert.vo.WmsSafetyStockConfigSaveReqVO;
import cn.zhicloud.module.wms.dal.dataobject.inventory.WmsSafetyStockConfigDO;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;

/**
 * WMS 安全库存配置 Service 接口
 *
 * @author 智云
 */
public interface WmsSafetyStockConfigService {

    /**
     * 创建安全库存配置
     */
    Long createSafetyStockConfig(@Valid WmsSafetyStockConfigSaveReqVO createReqVO);

    /**
     * 更新安全库存配置
     */
    void updateSafetyStockConfig(@Valid WmsSafetyStockConfigSaveReqVO updateReqVO);

    /**
     * 删除安全库存配置
     */
    void deleteSafetyStockConfig(Long id);

    /**
     * 获得安全库存配置
     */
    WmsSafetyStockConfigDO getSafetyStockConfig(Long id);

    /**
     * 获得安全库存配置分页
     */
    PageResult<WmsSafetyStockConfigDO> getSafetyStockConfigPage(WmsSafetyStockConfigPageReqVO pageReqVO);

    /**
     * 获得全部安全库存配置列表（预警 Job 使用）
     */
    List<WmsSafetyStockConfigDO> getSafetyStockConfigList();

    /**
     * 根据仓库 + SKU 获取配置
     */
    WmsSafetyStockConfigDO getSafetyStockConfigByWarehouseAndProduct(Long warehouseId, Long productId);

    /**
     * 根据 SKU 集合获取配置
     */
    List<WmsSafetyStockConfigDO> getSafetyStockConfigListByProductIds(Collection<Long> productIds);

    /**
     * 校验安全库存配置存在
     */
    WmsSafetyStockConfigDO validateSafetyStockConfigExists(Long id);

}