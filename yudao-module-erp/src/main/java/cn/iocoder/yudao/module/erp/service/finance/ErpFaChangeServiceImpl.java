package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.fachange.ErpFaChangePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.fachange.ErpFaChangeSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFaChangeDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFixedAssetDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFaChangeMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFixedAssetMapper;
import cn.iocoder.yudao.module.erp.enums.finance.ErpFaChangeStatusEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpFaChangeTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 固定资产变动 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class ErpFaChangeServiceImpl implements ErpFaChangeService {

    @Resource
    private ErpFaChangeMapper faChangeMapper;
    @Resource
    private ErpFixedAssetMapper fixedAssetMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFaChange(ErpFaChangeSaveReqVO createReqVO) {
        // 1. 校验固定资产存在
        validateAssetExists(createReqVO.getAssetId());
        // 2. 构建对象，状态初始化为待审核
        ErpFaChangeDO faChange = BeanUtils.toBean(createReqVO, ErpFaChangeDO.class);
        faChange.setStatus(ErpFaChangeStatusEnum.PENDING.getStatus());
        faChangeMapper.insert(faChange);
        return faChange.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFaChange(ErpFaChangeSaveReqVO updateReqVO) {
        // 1. 校验存在
        ErpFaChangeDO existing = validateFaChangeExists(updateReqVO.getId());
        // 2. 校验状态为待审核
        if (!Objects.equals(existing.getStatus(), ErpFaChangeStatusEnum.PENDING.getStatus())) {
            throw exception(FA_CHANGE_STATUS_INVALID);
        }
        // 3. 校验固定资产存在
        validateAssetExists(updateReqVO.getAssetId());
        // 4. 更新
        ErpFaChangeDO updateObj = BeanUtils.toBean(updateReqVO, ErpFaChangeDO.class);
        faChangeMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFaChange(Long id) {
        // 1. 校验存在
        ErpFaChangeDO existing = validateFaChangeExists(id);
        // 2. 校验状态为待审核
        if (!Objects.equals(existing.getStatus(), ErpFaChangeStatusEnum.PENDING.getStatus())) {
            throw exception(FA_CHANGE_STATUS_INVALID);
        }
        // 3. 删除
        faChangeMapper.deleteById(id);
    }

    @Override
    public ErpFaChangeDO getFaChange(Long id) {
        return faChangeMapper.selectById(id);
    }

    @Override
    public PageResult<ErpFaChangeDO> getFaChangePage(ErpFaChangePageReqVO pageReqVO) {
        return faChangeMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveFaChange(Long id) {
        // 1. 校验存在
        ErpFaChangeDO faChange = validateFaChangeExists(id);
        // 2. 校验状态为待审核
        if (!Objects.equals(faChange.getStatus(), ErpFaChangeStatusEnum.PENDING.getStatus())) {
            throw exception(FA_CHANGE_STATUS_INVALID);
        }
        // 3. 校验关联固定资产存在
        ErpFixedAssetDO asset = fixedAssetMapper.selectById(faChange.getAssetId());
        if (asset == null) {
            throw exception(FA_CHANGE_ASSET_NOT_EXISTS);
        }
        // 4. 根据变动类型，实际更新固定资产对应字段
        applyChangeToAsset(faChange, asset);
        // 5. 更新变动记录状态为已审核，记录审批人
        ErpFaChangeDO updateObj = new ErpFaChangeDO();
        updateObj.setId(id);
        updateObj.setStatus(ErpFaChangeStatusEnum.APPROVED.getStatus());
        updateObj.setApproverId(SecurityFrameworkUtils.getLoginUserId());
        faChangeMapper.updateById(updateObj);
        log.info("[approveFaChange][资产变动记录 {} 审核通过，已更新资产 {} 字段]", id, faChange.getAssetId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectFaChange(Long id, String reason) {
        // 1. 校验存在
        ErpFaChangeDO faChange = validateFaChangeExists(id);
        // 2. 校验状态为待审核
        if (!Objects.equals(faChange.getStatus(), ErpFaChangeStatusEnum.PENDING.getStatus())) {
            throw exception(FA_CHANGE_STATUS_INVALID);
        }
        // 3. 更新状态为已驳回
        ErpFaChangeDO updateObj = new ErpFaChangeDO();
        updateObj.setId(id);
        updateObj.setStatus(ErpFaChangeStatusEnum.REJECTED.getStatus());
        updateObj.setRejectReason(reason);
        updateObj.setApproverId(SecurityFrameworkUtils.getLoginUserId());
        faChangeMapper.updateById(updateObj);
        log.info("[rejectFaChange][资产变动记录 {} 已驳回，原因：{}]", id, reason);
    }

    @Override
    public List<ErpFaChangeDO> listByAssetId(Long assetId) {
        return faChangeMapper.selectListByAssetId(assetId);
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 校验资产变动记录存在
     */
    private ErpFaChangeDO validateFaChangeExists(Long id) {
        if (id == null) {
            throw exception(FA_CHANGE_NOT_EXISTS);
        }
        ErpFaChangeDO faChange = faChangeMapper.selectById(id);
        if (faChange == null) {
            throw exception(FA_CHANGE_NOT_EXISTS);
        }
        return faChange;
    }

    /**
     * 校验固定资产存在
     */
    private void validateAssetExists(Long assetId) {
        if (assetId == null || fixedAssetMapper.selectById(assetId) == null) {
            throw exception(FA_CHANGE_ASSET_NOT_EXISTS);
        }
    }

    /**
     * 根据变动类型，实际更新固定资产对应字段
     *
     * @param faChange 变动记录
     * @param asset    固定资产
     */
    private void applyChangeToAsset(ErpFaChangeDO faChange, ErpFixedAssetDO asset) {
        ErpFixedAssetDO updateAsset = new ErpFixedAssetDO();
        updateAsset.setId(asset.getId());
        String afterValue = faChange.getAfterValue();
        Integer changeType = faChange.getChangeType();

        if (Objects.equals(changeType, ErpFaChangeTypeEnum.DEPARTMENT_TRANSFER.getType())) {
            // 部门转移：更新 departmentId
            updateAsset.setDepartmentId(Long.valueOf(afterValue));
        } else if (Objects.equals(changeType, ErpFaChangeTypeEnum.STATUS_CHANGE.getType())) {
            // 状态变动：更新 status
            updateAsset.setStatus(Integer.valueOf(afterValue));
        } else if (Objects.equals(changeType, ErpFaChangeTypeEnum.ORIGINAL_VALUE_ADJUST.getType())) {
            // 原值调整：更新 originalValue，并重算 netBookValue = originalValue - accumulatedDepreciation
            BigDecimal newOriginalValue = new BigDecimal(afterValue);
            updateAsset.setOriginalValue(newOriginalValue);
            BigDecimal accumulatedDep = asset.getAccumulatedDepreciation() == null
                    ? BigDecimal.ZERO : asset.getAccumulatedDepreciation();
            updateAsset.setNetBookValue(newOriginalValue.subtract(accumulatedDep));
        } else if (Objects.equals(changeType, ErpFaChangeTypeEnum.USEFUL_LIFE_ADJUST.getType())) {
            // 使用年限调整：更新 usefulLifeMonths
            updateAsset.setUsefulLifeMonths(Integer.valueOf(afterValue));
        } else if (Objects.equals(changeType, ErpFaChangeTypeEnum.SALVAGE_VALUE_ADJUST.getType())) {
            // 残值调整：更新 salvageValue
            updateAsset.setSalvageValue(new BigDecimal(afterValue));
        } else if (Objects.equals(changeType, ErpFaChangeTypeEnum.DEPRECIATION_METHOD_CHANGE.getType())) {
            // 折旧方法变更：更新 depreciationMethod
            updateAsset.setDepreciationMethod(Integer.valueOf(afterValue));
        }
        fixedAssetMapper.updateById(updateAsset);
    }

}
