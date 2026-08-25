package cn.zhicloud.module.erp.service.stock;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.stock.vo.stockserial.ErpStockSerialPageReqVO;
import cn.zhicloud.module.erp.controller.admin.stock.vo.stockserial.ErpStockSerialSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.stock.ErpStockSerialDO;
import cn.zhicloud.module.erp.dal.mysql.stock.ErpStockSerialMapper;
import cn.zhicloud.module.erp.enums.stock.ErpStockSerialStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 库存序列号 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class ErpStockSerialServiceImpl implements ErpStockSerialService {

    @Resource
    private ErpStockSerialMapper stockSerialMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createStockSerial(ErpStockSerialSaveReqVO createReqVO) {
        // 校验序列号唯一
        validateSerialNoUnique(null, createReqVO.getSerialNo());
        // 插入
        ErpStockSerialDO serial = BeanUtils.toBean(createReqVO, ErpStockSerialDO.class);
        if (serial.getStatus() == null) {
            serial.setStatus(ErpStockSerialStatusEnum.INSTOCK.getStatus());
        }
        stockSerialMapper.insert(serial);
        return serial.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStockSerial(ErpStockSerialSaveReqVO updateReqVO) {
        // 校验存在
        validateStockSerialExists(updateReqVO.getId());
        // 校验序列号唯一
        validateSerialNoUnique(updateReqVO.getId(), updateReqVO.getSerialNo());
        // 更新
        ErpStockSerialDO updateObj = BeanUtils.toBean(updateReqVO, ErpStockSerialDO.class);
        stockSerialMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStockSerial(Long id) {
        // 校验存在
        validateStockSerialExists(id);
        // 删除
        stockSerialMapper.deleteById(id);
    }

    @Override
    public ErpStockSerialDO getStockSerial(Long id) {
        return stockSerialMapper.selectById(id);
    }

    @Override
    public ErpStockSerialDO getStockSerialBySerialNo(String serialNo) {
        return stockSerialMapper.selectBySerialNo(serialNo);
    }

    @Override
    public PageResult<ErpStockSerialDO> getStockSerialPage(ErpStockSerialPageReqVO pageReqVO) {
        return stockSerialMapper.selectPage(pageReqVO);
    }

    private void validateStockSerialExists(Long id) {
        if (stockSerialMapper.selectById(id) == null) {
            throw exception(STOCK_SERIAL_NOT_EXISTS);
        }
    }

    private void validateSerialNoUnique(Long id, String serialNo) {
        ErpStockSerialDO existing = stockSerialMapper.selectBySerialNo(serialNo);
        if (existing != null && !Objects.equals(existing.getId(), id)) {
            throw exception(STOCK_SERIAL_NO_DUPLICATE, serialNo);
        }
    }

}
