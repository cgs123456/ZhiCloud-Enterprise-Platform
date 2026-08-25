package cn.zhicloud.module.erp.service.stock;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.stock.vo.stockbatch.ErpStockBatchPageReqVO;
import cn.zhicloud.module.erp.controller.admin.stock.vo.stockbatch.ErpStockBatchSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.stock.ErpStockBatchDO;
import cn.zhicloud.module.erp.dal.mysql.stock.ErpStockBatchMapper;
import cn.zhicloud.module.erp.enums.stock.ErpStockBatchStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 库存批次 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class ErpStockBatchServiceImpl implements ErpStockBatchService {

    @Resource
    private ErpStockBatchMapper stockBatchMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createStockBatch(ErpStockBatchSaveReqVO createReqVO) {
        // 校验批次号唯一
        validateBatchNoUnique(null, createReqVO.getBatchNo());
        // 插入
        ErpStockBatchDO batch = BeanUtils.toBean(createReqVO, ErpStockBatchDO.class);
        if (batch.getStatus() == null) {
            batch.setStatus(ErpStockBatchStatusEnum.AVAILABLE.getStatus());
        }
        stockBatchMapper.insert(batch);
        return batch.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStockBatch(ErpStockBatchSaveReqVO updateReqVO) {
        // 校验存在
        validateStockBatchExists(updateReqVO.getId());
        // 校验批次号唯一
        validateBatchNoUnique(updateReqVO.getId(), updateReqVO.getBatchNo());
        // 更新
        ErpStockBatchDO updateObj = BeanUtils.toBean(updateReqVO, ErpStockBatchDO.class);
        stockBatchMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStockBatch(Long id) {
        // 校验存在
        validateStockBatchExists(id);
        // 删除
        stockBatchMapper.deleteById(id);
    }

    @Override
    public ErpStockBatchDO getStockBatch(Long id) {
        return stockBatchMapper.selectById(id);
    }

    @Override
    public PageResult<ErpStockBatchDO> getStockBatchPage(ErpStockBatchPageReqVO pageReqVO) {
        return stockBatchMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpStockBatchDO> getExpiringBatchList(LocalDate date) {
        LocalDate target = date == null ? LocalDate.now() : date;
        return stockBatchMapper.selectListByExpiryDate(target);
    }

    private void validateStockBatchExists(Long id) {
        if (stockBatchMapper.selectById(id) == null) {
            throw exception(STOCK_BATCH_NOT_EXISTS);
        }
    }

    private void validateBatchNoUnique(Long id, String batchNo) {
        ErpStockBatchDO existing = stockBatchMapper.selectByBatchNo(batchNo);
        if (existing != null && !Objects.equals(existing.getId(), id)) {
            throw exception(STOCK_BATCH_NO_DUPLICATE, batchNo);
        }
    }

}
