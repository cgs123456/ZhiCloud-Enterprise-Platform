package cn.zhicloud.module.wms.service.inventory.batch;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.wms.controller.admin.inventory.batch.vo.WmsInventoryBatchPageReqVO;
import cn.zhicloud.module.wms.controller.admin.inventory.batch.vo.WmsInventoryBatchSaveReqVO;
import cn.zhicloud.module.wms.controller.admin.inventory.batch.vo.WmsInventoryBatchStrategyRespVO;
import cn.zhicloud.module.wms.dal.dataobject.inventory.WmsInventoryBatchDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * WMS 库存批次 Service 接口
 *
 * @author 智云
 */
public interface WmsInventoryBatchService {

    /**
     * 创建库存批次
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createBatch(@Valid WmsInventoryBatchSaveReqVO createReqVO);

    /**
     * 更新库存批次
     *
     * @param updateReqVO 更新信息
     */
    void updateBatch(@Valid WmsInventoryBatchSaveReqVO updateReqVO);

    /**
     * 删除库存批次
     *
     * @param id 编号
     */
    void deleteBatch(Long id);

    /**
     * 获得库存批次
     *
     * @param id 编号
     * @return 库存批次
     */
    WmsInventoryBatchDO getBatch(Long id);

    /**
     * 获得库存批次的分页
     *
     * @param pageReqVO 分页查询
     * @return 库存批次分页
     */
    PageResult<WmsInventoryBatchDO> getBatchPage(WmsInventoryBatchPageReqVO pageReqVO);

    /**
     * 按库存编号查询批次明细列表
     *
     * @param inventoryId 库存编号
     * @return 批次列表
     */
    List<WmsInventoryBatchDO> getBatchesByInventoryId(Long inventoryId);

    /**
     * 查询即将过期的批次（预警）
     *
     * @param days 临期天数（在当前日期 + days 天内过期的批次）
     * @return 批次列表
     */
    List<WmsInventoryBatchDO> getExpiringBatches(Integer days);

    /**
     * 查询已过期批次
     *
     * @return 批次列表
     */
    List<WmsInventoryBatchDO> getExpiredBatches();

    /**
     * 应用 FIFO 先进先出策略
     *
     * @param inventoryId 库存编号
     * @param quantity 需求数量
     * @return 批次出库顺序
     */
    WmsInventoryBatchStrategyRespVO applyFifoStrategy(Long inventoryId, java.math.BigDecimal quantity);

    /**
     * 应用 FEFO 先到期先出策略
     *
     * @param inventoryId 库存编号
     * @param quantity 需求数量
     * @return 批次出库顺序
     */
    WmsInventoryBatchStrategyRespVO applyFefoStrategy(Long inventoryId, java.math.BigDecimal quantity);

}
