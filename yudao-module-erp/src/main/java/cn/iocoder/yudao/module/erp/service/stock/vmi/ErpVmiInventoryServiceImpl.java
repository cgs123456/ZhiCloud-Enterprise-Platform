package cn.iocoder.yudao.module.erp.service.stock.vmi;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vmi.vo.ErpVmiInventoryPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vmi.vo.ErpVmiInventorySaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.vmi.ErpVmiInventoryDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.vmi.ErpVmiInventoryMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.VMI_INVENTORY_NOT_EXISTS;

/**
 * ERP VMI 供应商管理库存 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpVmiInventoryServiceImpl implements ErpVmiInventoryService {

    @Resource
    private ErpVmiInventoryMapper vmiInventoryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createVmiInventory(ErpVmiInventorySaveReqVO createReqVO) {
        ErpVmiInventoryDO inventory = BeanUtils.toBean(createReqVO, ErpVmiInventoryDO.class);
        // 计算可用库存 = 当前库存 - 锁定库存
        calcAvailableQuantity(inventory);
        vmiInventoryMapper.insert(inventory);
        return inventory.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVmiInventory(ErpVmiInventorySaveReqVO updateReqVO) {
        validateVmiInventoryExists(updateReqVO.getId());
        ErpVmiInventoryDO updateObj = BeanUtils.toBean(updateReqVO, ErpVmiInventoryDO.class);
        calcAvailableQuantity(updateObj);
        vmiInventoryMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteVmiInventory(Long id) {
        validateVmiInventoryExists(id);
        vmiInventoryMapper.deleteById(id);
    }

    private void validateVmiInventoryExists(Long id) {
        if (vmiInventoryMapper.selectById(id) == null) {
            throw exception(VMI_INVENTORY_NOT_EXISTS);
        }
    }

    @Override
    public ErpVmiInventoryDO getVmiInventory(Long id) {
        return vmiInventoryMapper.selectById(id);
    }

    @Override
    public PageResult<ErpVmiInventoryDO> getVmiInventoryPage(ErpVmiInventoryPageReqVO pageReqVO) {
        return vmiInventoryMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpVmiInventoryDO> checkReplenishment() {
        // 查询可用库存 <= 补货点的库存记录
        return vmiInventoryMapper.selectReplenishmentList();
    }

    /**
     * 计算可用库存数量 = 当前库存数量 - 锁定库存数量
     */
    private void calcAvailableQuantity(ErpVmiInventoryDO inventory) {
        BigDecimal quantity = inventory.getQuantity() == null ? BigDecimal.ZERO : inventory.getQuantity();
        BigDecimal locked = inventory.getLockedQuantity() == null ? BigDecimal.ZERO : inventory.getLockedQuantity();
        inventory.setAvailableQuantity(quantity.subtract(locked));
    }

}
