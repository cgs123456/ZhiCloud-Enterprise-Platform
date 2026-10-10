package cn.zhicloud.module.fms.service.config;

import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.fms.controller.admin.config.vo.auxiliary.FmsAuxiliaryItemSaveReqVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.auxiliary.FmsAuxiliaryItemPageReqVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.auxiliary.FmsAuxiliaryItemImportExcelVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.auxiliary.FmsAuxiliaryItemImportRespVO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsAuxiliaryItemDO;
import cn.zhicloud.module.fms.dal.mysql.config.FmsAuxiliaryItemMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.fms.enums.ErrorCodeConstants.AUXILIARY_ITEM_NOT_EXISTS;
import static cn.zhicloud.module.fms.enums.ErrorCodeConstants.AUXILIARY_ITEM_CODE_DUPLICATE;

/**
 * FMS 辅助核算项目 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class FmsAuxiliaryItemServiceImpl implements FmsAuxiliaryItemService {

    @Resource
    private FmsAuxiliaryItemMapper auxiliaryItemMapper;

    @Override
    public Long createAuxiliaryItem(FmsAuxiliaryItemSaveReqVO createReqVO) {
        // 校验编码/名称在账套内唯一
        if (auxiliaryItemMapper.selectByAccountSetAndUnique(createReqVO.getAccountSetId(), createReqVO.getCode()) != null) {
            throw exception(AUXILIARY_ITEM_CODE_DUPLICATE);
        }
        FmsAuxiliaryItemDO entity = BeanUtils.toBean(createReqVO, FmsAuxiliaryItemDO.class);
        auxiliaryItemMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void updateAuxiliaryItem(FmsAuxiliaryItemSaveReqVO updateReqVO) {
        validateExists(updateReqVO.getAccountSetId(), updateReqVO.getId());
        FmsAuxiliaryItemDO same = auxiliaryItemMapper.selectByAccountSetAndUnique(updateReqVO.getAccountSetId(), updateReqVO.getCode());
        if (same != null && !same.getId().equals(updateReqVO.getId())) {
            throw exception(AUXILIARY_ITEM_CODE_DUPLICATE);
        }
        FmsAuxiliaryItemDO updateObj = BeanUtils.toBean(updateReqVO, FmsAuxiliaryItemDO.class);
        auxiliaryItemMapper.updateById(updateObj);
    }

    @Override
    public void deleteAuxiliaryItem(Long accountSetId, Long id) {
        validateExists(accountSetId, id);
        auxiliaryItemMapper.deleteById(id);
    }

    @Override
    public void deleteAuxiliaryItemList(Long accountSetId, List<Long> ids) {
        ids.forEach(id -> deleteAuxiliaryItem(accountSetId, id));
    }

    @Override
    public List<FmsAuxiliaryItemDO> getAuxiliaryItemList(Long accountSetId) {
        return auxiliaryItemMapper.selectListByAccountSet(accountSetId);
    }

    @Override
    public PageResult<FmsAuxiliaryItemDO> getAuxiliaryItemPage(FmsAuxiliaryItemPageReqVO pageReqVO) {
        return auxiliaryItemMapper.selectPage(pageReqVO);
    }

    @Override
    public void updateAuxiliaryItemStatus(Long accountSetId, Long id, Integer status) {
        validateExists(accountSetId, id);
        FmsAuxiliaryItemDO updateObj = new FmsAuxiliaryItemDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        auxiliaryItemMapper.updateById(updateObj);
    }

    @Override
    public FmsAuxiliaryItemImportRespVO importAuxiliaryItemList(Long accountSetId, Long auxiliaryTypeId,
            List<FmsAuxiliaryItemImportExcelVO> list, boolean updateSupport) {
        List<String> successItemCodes = new ArrayList<>();
        Map<String, String> failureReasons = new LinkedHashMap<>();
        for (FmsAuxiliaryItemImportExcelVO importItem : list) {
            FmsAuxiliaryItemDO exists = auxiliaryItemMapper.selectByAccountSetAndUnique(accountSetId, importItem.getCode());
            if (exists == null) {
                FmsAuxiliaryItemDO entity = BeanUtils.toBean(importItem, FmsAuxiliaryItemDO.class);
                entity.setAccountSetId(accountSetId);
                entity.setAuxiliaryTypeId(auxiliaryTypeId);
                auxiliaryItemMapper.insert(entity);
                successItemCodes.add(importItem.getCode());
            } else if (updateSupport) {
                FmsAuxiliaryItemDO updateObj = BeanUtils.toBean(importItem, FmsAuxiliaryItemDO.class);
                updateObj.setId(exists.getId());
                updateObj.setAccountSetId(accountSetId);
                auxiliaryItemMapper.updateById(updateObj);
                successItemCodes.add(importItem.getCode());
            } else {
                failureReasons.put(importItem.getCode(), "编码已存在");
            }
        }
        return FmsAuxiliaryItemImportRespVO.builder()
                .totalCount(list.size())
                .successItemCodes(successItemCodes)
                .failureReasons(failureReasons)
                .build();
    }

    private void validateExists(Long accountSetId, Long id) {
        if (auxiliaryItemMapper.selectByAccountSetAndId(accountSetId, id) == null) {
            throw exception(AUXILIARY_ITEM_NOT_EXISTS);
        }
    }

}
