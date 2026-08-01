package cn.iocoder.yudao.module.wms.service.inventory.slotting;

import cn.iocoder.yudao.module.wms.dal.dataobject.inventory.WmsInventoryDO;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * WMS 上架 Slotting Service 接口
 *
 * <p>负责为待上架商品推荐目标仓库/库存行/批次：
 * <ol>
 *   <li>优先合并到指定仓库已有同 SKU 的库存行</li>
 *   <li>若该库存行下存在相同 batchNo 的批次 → 合并到该批次（量增）</li>
 *   <li>否则 → 在该库存行下新建批次</li>
 *   <li>若仓库无该 SKU 库存行 → 新建库存行 + 新建批次</li>
 * </ol>
 *
 * <p><b>注意</b>：本服务只做<b>推荐</b>，不修改任何数据。实际库存/批次的写入由调用方完成。
 *
 * @author 芋道源码
 */
public interface WmsSlottingService {

    /**
     * 上架推荐
     *
     * @param warehouseId    目标仓库编号
     * @param skuId          商品 SKU 编号
     * @param batchNo        批次号（可为空，表示无批次管理）
     * @param quantity       上架数量
     * @param productionDate 生产日期（可为空）
     * @param expiryDate     过期日期（可为空，表示无保质期管理）
     * @return 推荐结果，包含目标库存行/批次 ID 与推荐理由
     */
    WmsSlottingRecommendationRespVO recommendPutaway(Long warehouseId, Long skuId, String batchNo,
                                                     BigDecimal quantity, LocalDate productionDate,
                                                     LocalDate expiryDate);

    /**
     * 校验上架批次是否有效（未过期）
     *
     * @param batchNo    批次号
     * @param expiryDate 过期日期
     * @throws cn.iocoder.yudao.framework.common.exception.ServiceException 若批次已过期
     */
    void validateBatchNotExpired(String batchNo, LocalDate expiryDate);

    /**
     * 执行批次合并或新建（事务性操作，由调用方调用）
     *
     * <p>合并策略：
     * <ul>
     *   <li>若目标库存行已有相同 batchNo 的批次 → 数量累加到该批次</li>
     *   <li>否则 → 在目标库存行下新建批次</li>
     * </ul>
     *
     * @param inventory     目标库存行（由调用方先确保存在）
     * @param batchNo       批次号
     * @param productionDate 生产日期
     * @param expiryDate     过期日期
     * @param quantity       本次上架数量
     * @return 新建或更新的批次编号
     */
    Long mergeOrCreateBatch(WmsInventoryDO inventory, String batchNo, LocalDate productionDate,
                            LocalDate expiryDate, BigDecimal quantity);

}
