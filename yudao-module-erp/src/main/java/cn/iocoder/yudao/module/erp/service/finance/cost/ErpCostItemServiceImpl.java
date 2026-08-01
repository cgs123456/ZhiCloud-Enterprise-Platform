package cn.iocoder.yudao.module.erp.service.finance.cost;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.costitem.ErpCostItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.costitem.ErpCostItemSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.cost.ErpCostItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.cost.ErpCostItemMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.COST_ITEM_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.COST_ITEM_NOT_EXISTS;

/**
 * ERP 成本项目 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpCostItemServiceImpl implements ErpCostItemService {

    @Resource
    private ErpCostItemMapper costItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCostItem(ErpCostItemSaveReqVO createReqVO) {
        // 校验编码唯一
        validateCodeUnique(null, createReqVO.getCode());
        // 插入
        ErpCostItemDO costItem = BeanUtils.toBean(createReqVO, ErpCostItemDO.class);
        costItemMapper.insert(costItem);
        return costItem.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCostItem(ErpCostItemSaveReqVO updateReqVO) {
        // 校验存在
        validateCostItemExists(updateReqVO.getId());
        // 校验编码唯一
        validateCodeUnique(updateReqVO.getId(), updateReqVO.getCode());
        // 更新
        ErpCostItemDO updateObj = BeanUtils.toBean(updateReqVO, ErpCostItemDO.class);
        costItemMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCostItem(Long id) {
        // 校验存在
        validateCostItemExists(id);
        // 删除
        costItemMapper.deleteById(id);
    }

    @Override
    public ErpCostItemDO getCostItem(Long id) {
        return costItemMapper.selectById(id);
    }

    @Override
    public PageResult<ErpCostItemDO> getCostItemPage(ErpCostItemPageReqVO pageReqVO) {
        return costItemMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpCostItemDO> getCostItemListByStatus(Integer status) {
        return costItemMapper.selectListByStatus(status);
    }

    private void validateCostItemExists(Long id) {
        if (costItemMapper.selectById(id) == null) {
            throw exception(COST_ITEM_NOT_EXISTS);
        }
    }

    private void validateCodeUnique(Long id, String code) {
        ErpCostItemDO existing = costItemMapper.selectByCode(code);
        if (existing != null && !Objects.equals(existing.getId(), id)) {
            throw exception(COST_ITEM_CODE_DUPLICATE, code);
        }
    }

}
