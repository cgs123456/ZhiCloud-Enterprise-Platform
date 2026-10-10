package cn.zhicloud.module.fms.service.config;

import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.fms.controller.admin.config.vo.auxiliary.FmsAuxiliaryTypeSaveReqVO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsAuxiliaryTypeDO;
import cn.zhicloud.module.fms.dal.mysql.config.FmsAuxiliaryTypeMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.fms.enums.ErrorCodeConstants.AUXILIARY_TYPE_NOT_EXISTS;
import static cn.zhicloud.module.fms.enums.ErrorCodeConstants.AUXILIARY_TYPE_SYSTEM_PRESET_DELETE;

/**
 * FMS 辅助核算类别 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class FmsAuxiliaryTypeServiceImpl implements FmsAuxiliaryTypeService {

    @Resource
    private FmsAuxiliaryTypeMapper auxiliaryTypeMapper;

    @Override
    public Long createAuxiliaryType(FmsAuxiliaryTypeSaveReqVO createReqVO) {
        FmsAuxiliaryTypeDO entity = BeanUtils.toBean(createReqVO, FmsAuxiliaryTypeDO.class);
        auxiliaryTypeMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void updateAuxiliaryType(FmsAuxiliaryTypeSaveReqVO updateReqVO) {
        validateExists(updateReqVO.getAccountSetId(), updateReqVO.getId());
        FmsAuxiliaryTypeDO updateObj = BeanUtils.toBean(updateReqVO, FmsAuxiliaryTypeDO.class);
        auxiliaryTypeMapper.updateById(updateObj);
    }

    @Override
    public void deleteAuxiliaryType(Long accountSetId, Long id) {
        validateExists(accountSetId, id);
        FmsAuxiliaryTypeDO entity = auxiliaryTypeMapper.selectByAccountSetAndId(accountSetId, id);
        if (Boolean.TRUE.equals(entity.getSystemPreset())) {
            throw exception(AUXILIARY_TYPE_SYSTEM_PRESET_DELETE);
        }
        auxiliaryTypeMapper.deleteById(id);
    }

    @Override
    public List<FmsAuxiliaryTypeDO> getAuxiliaryTypeList(Long accountSetId) {
        return auxiliaryTypeMapper.selectListByAccountSet(accountSetId);
    }

    private void validateExists(Long accountSetId, Long id) {
        if (auxiliaryTypeMapper.selectByAccountSetAndId(accountSetId, id) == null) {
            throw exception(AUXILIARY_TYPE_NOT_EXISTS);
        }
    }

}
