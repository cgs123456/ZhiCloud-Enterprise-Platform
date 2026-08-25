package cn.zhicloud.module.erp.service.finance.cost;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.finance.vo.standardcost.ErpStandardCostPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.standardcost.ErpStandardCostSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.cost.ErpStandardCostDO;
import cn.zhicloud.module.erp.dal.mysql.finance.cost.ErpStandardCostMapper;
import cn.zhicloud.module.erp.enums.finance.cost.ErpStandardCostStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.STANDARD_COST_DATE_INVALID;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.STANDARD_COST_EXISTS;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.STANDARD_COST_NOT_EXISTS;

/**
 * ERP 标准成本 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class ErpStandardCostServiceImpl implements ErpStandardCostService {

    @Resource
    private ErpStandardCostMapper standardCostMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createStandardCost(ErpStandardCostSaveReqVO createReqVO) {
        // 校验生效日期
        validateDateRange(createReqVO.getEffectiveDate(), createReqVO.getExpiryDate());
        // 校验唯一性
        validateUnique(null, createReqVO.getProductId(), createReqVO.getCostItemId(), createReqVO.getEffectiveDate());
        // 插入
        ErpStandardCostDO standardCost = BeanUtils.toBean(createReqVO, ErpStandardCostDO.class);
        if (standardCost.getStatus() == null) {
            standardCost.setStatus(ErpStandardCostStatusEnum.DRAFT.getStatus());
        }
        standardCostMapper.insert(standardCost);
        return standardCost.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStandardCost(ErpStandardCostSaveReqVO updateReqVO) {
        // 校验存在
        validateStandardCostExists(updateReqVO.getId());
        // 校验生效日期
        validateDateRange(updateReqVO.getEffectiveDate(), updateReqVO.getExpiryDate());
        // 校验唯一性
        validateUnique(updateReqVO.getId(), updateReqVO.getProductId(), updateReqVO.getCostItemId(), updateReqVO.getEffectiveDate());
        // 更新
        ErpStandardCostDO updateObj = BeanUtils.toBean(updateReqVO, ErpStandardCostDO.class);
        standardCostMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStandardCost(Long id) {
        // 校验存在
        validateStandardCostExists(id);
        // 删除
        standardCostMapper.deleteById(id);
    }

    @Override
    public ErpStandardCostDO getStandardCost(Long id) {
        return standardCostMapper.selectById(id);
    }

    @Override
    public PageResult<ErpStandardCostDO> getStandardCostPage(ErpStandardCostPageReqVO pageReqVO) {
        return standardCostMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpStandardCostDO> getEffectiveStandardCostList(Long productId, LocalDate date) {
        return standardCostMapper.selectEffectiveListByProduct(productId, date);
    }

    private void validateStandardCostExists(Long id) {
        if (standardCostMapper.selectById(id) == null) {
            throw exception(STANDARD_COST_NOT_EXISTS);
        }
    }

    private void validateDateRange(LocalDate effectiveDate, LocalDate expiryDate) {
        if (effectiveDate != null && expiryDate != null && effectiveDate.isAfter(expiryDate)) {
            throw exception(STANDARD_COST_DATE_INVALID);
        }
    }

    private void validateUnique(Long id, Long productId, Long costItemId, LocalDate effectiveDate) {
        if (productId == null || costItemId == null || effectiveDate == null) {
            return;
        }
        ErpStandardCostDO existing = standardCostMapper.selectByProductAndCostItem(productId, costItemId, effectiveDate);
        if (existing != null && !Objects.equals(existing.getId(), id)) {
            throw exception(STANDARD_COST_EXISTS);
        }
    }

}
