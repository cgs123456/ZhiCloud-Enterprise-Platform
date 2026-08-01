package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.costallocation.ErpCostAllocationPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.costallocation.ErpCostAllocationSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpCostAllocationDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpCostCenterDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpCostAllocationMapper;
import cn.iocoder.yudao.module.erp.enums.finance.ErpCostAllocationTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 成本分摊 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpCostAllocationServiceImpl implements ErpCostAllocationService {

    @Resource
    private ErpCostAllocationMapper costAllocationMapper;
    @Resource
    private ErpCostCenterService costCenterService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCostAllocation(ErpCostAllocationSaveReqVO createReqVO) {
        // 校验源/目标成本中心存在
        validateCostCenter(createReqVO.getCostCenterId());
        validateCostCenter(createReqVO.getTargetCostCenterId());
        // 校验源与目标不能相同
        if (Objects.equals(createReqVO.getCostCenterId(), createReqVO.getTargetCostCenterId())) {
            throw exception(COST_ALLOCATION_TARGET_SAME);
        }
        // 校验金额
        if (createReqVO.getAmount() == null || createReqVO.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(COST_ALLOCATION_AMOUNT_INVALID);
        }
        // 插入
        ErpCostAllocationDO allocation = BeanUtils.toBean(createReqVO, ErpCostAllocationDO.class);
        costAllocationMapper.insert(allocation);
        return allocation.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCostAllocation(ErpCostAllocationSaveReqVO updateReqVO) {
        // 校验存在
        validateCostAllocationExists(updateReqVO.getId());
        // 校验源/目标成本中心存在
        validateCostCenter(updateReqVO.getCostCenterId());
        validateCostCenter(updateReqVO.getTargetCostCenterId());
        // 校验源与目标不能相同
        if (Objects.equals(updateReqVO.getCostCenterId(), updateReqVO.getTargetCostCenterId())) {
            throw exception(COST_ALLOCATION_TARGET_SAME);
        }
        // 校验金额
        if (updateReqVO.getAmount() == null || updateReqVO.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(COST_ALLOCATION_AMOUNT_INVALID);
        }
        // 更新
        ErpCostAllocationDO updateObj = BeanUtils.toBean(updateReqVO, ErpCostAllocationDO.class);
        costAllocationMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCostAllocation(Long id) {
        // 校验存在
        validateCostAllocationExists(id);
        // 删除
        costAllocationMapper.deleteById(id);
    }

    @Override
    public ErpCostAllocationDO getCostAllocation(Long id) {
        return costAllocationMapper.selectById(id);
    }

    @Override
    public PageResult<ErpCostAllocationDO> getCostAllocationPage(ErpCostAllocationPageReqVO pageReqVO) {
        return costAllocationMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long allocateCost(Long costCenterId, Long targetId, BigDecimal amount, LocalDate date) {
        // 校验金额
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(COST_ALLOCATION_AMOUNT_INVALID);
        }
        // 校验源与目标不能相同
        if (Objects.equals(costCenterId, targetId)) {
            throw exception(COST_ALLOCATION_TARGET_SAME);
        }
        // 校验源/目标成本中心存在
        validateCostCenter(costCenterId);
        validateCostCenter(targetId);
        // 生成手工分摊记录
        ErpCostAllocationDO allocation = ErpCostAllocationDO.builder()
                .costCenterId(costCenterId)
                .targetCostCenterId(targetId)
                .allocationType(ErpCostAllocationTypeEnum.MANUAL.getType())
                .amount(amount)
                .allocationDate(date == null ? LocalDate.now() : date)
                .build();
        costAllocationMapper.insert(allocation);
        return allocation.getId();
    }

    private void validateCostAllocationExists(Long id) {
        if (costAllocationMapper.selectById(id) == null) {
            throw exception(COST_ALLOCATION_NOT_EXISTS);
        }
    }

    private void validateCostCenter(Long id) {
        ErpCostCenterDO center = costCenterService.getCostCenter(id);
        if (center == null) {
            throw exception(COST_CENTER_NOT_EXISTS);
        }
    }

}
