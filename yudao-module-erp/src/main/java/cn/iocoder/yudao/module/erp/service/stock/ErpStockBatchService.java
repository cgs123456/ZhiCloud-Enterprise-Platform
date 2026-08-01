package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stockbatch.ErpStockBatchPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stockbatch.ErpStockBatchSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockBatchDO;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;

/**
 * ERP 库存批次 Service 接口
 *
 * @author 芋道源码
 */
public interface ErpStockBatchService {

    /**
     * 创建库存批次
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createStockBatch(@Valid ErpStockBatchSaveReqVO createReqVO);

    /**
     * 更新库存批次
     *
     * @param updateReqVO 更新信息
     */
    void updateStockBatch(@Valid ErpStockBatchSaveReqVO updateReqVO);

    /**
     * 删除库存批次
     *
     * @param id 编号
     */
    void deleteStockBatch(Long id);

    /**
     * 获得库存批次
     *
     * @param id 编号
     * @return 库存批次
     */
    ErpStockBatchDO getStockBatch(Long id);

    /**
     * 获得库存批次分页
     *
     * @param pageReqVO 分页查询
     * @return 库存批次分页
     */
    PageResult<ErpStockBatchDO> getStockBatchPage(ErpStockBatchPageReqVO pageReqVO);

    /**
     * 获得即将过期的批次列表
     *
     * @param date 截止日期（早于此日期的批次视为即将过期）
     * @return 批次列表
     */
    List<ErpStockBatchDO> getExpiringBatchList(LocalDate date);

}
