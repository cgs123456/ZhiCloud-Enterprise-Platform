package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;

import java.math.BigDecimal;

/**
 * ERP 产品库存 Service 接口
 *
 * @author 芋道源码
 */
public interface ErpStockService {

    /**
     * 获得产品库存
     *
     * @param id 编号
     * @return 库存
     */
    ErpStockDO getStock(Long id);

    /**
     * 基于产品 + 仓库，获得产品库存
     *
     * @param productId 产品编号
     * @param warehouseId 仓库编号
     * @return 产品库存
     */
    ErpStockDO getStock(Long productId, Long warehouseId);

    /**
     * 获得产品库存数量
     *
     * 如果不存在库存记录，则返回 0
     *
     * @param productId 产品编号
     * @return 产品库存数量
     */
    BigDecimal getStockCount(Long productId);

    /**
     * 按产品编号查询所有仓库的库存记录
     *
     * @param productId 产品编号
     * @return 库存记录列表（可能为空）
     */
    java.util.List<ErpStockDO> getStockListByProductId(Long productId);

    /**
     * 获得产品库存分页
     *
     * @param pageReqVO 分页查询
     * @return 库存分页
     */
    PageResult<ErpStockDO> getStockPage(ErpStockPageReqVO pageReqVO);

    /**
     * 增量更新产品库存数量
     *
     * @param productId 产品编号
     * @param warehouseId 仓库编号
     * @param count 增量数量：正数，表示增加；负数，表示减少
     * @return 更新后的库存
     */
    BigDecimal updateStockCountIncrement(Long productId, Long warehouseId, BigDecimal count);

    /**
     * 锁定库存（预留）
     *
     * <p>从指定仓库的产品库存中锁定一定数量，lockedCount += count。
     * 可用库存 = count - lockedCount。
     *
     * @param productId 产品编号
     * @param warehouseId 仓库编号
     * @param count 锁定数量（正数）
     * @return 是否锁定成功（库存不足时返回 false）
     */
    boolean lockStock(Long productId, Long warehouseId, BigDecimal count);

    /**
     * 释放锁定的库存（解除预留）
     *
     * <p>释放此前锁定的库存，lockedCount -= count。
     *
     * @param productId 产品编号
     * @param warehouseId 仓库编号
     * @param count 释放数量（正数）
     * @return 是否释放成功
     */
    boolean unlockStock(Long productId, Long warehouseId, BigDecimal count);

    /**
     * 获取可用库存数量
     *
     * <p>可用库存 = 库存数量 - 锁定数量
     *
     * @param productId 产品编号
     * @param warehouseId 仓库编号
     * @return 可用库存数量（不存在记录时返回 0）
     */
    BigDecimal getAvailableCount(Long productId, Long warehouseId);

}