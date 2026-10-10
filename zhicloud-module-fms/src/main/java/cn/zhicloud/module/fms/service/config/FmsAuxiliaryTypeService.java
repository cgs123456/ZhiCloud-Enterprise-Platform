package cn.zhicloud.module.fms.service.config;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.fms.controller.admin.config.vo.auxiliary.FmsAuxiliaryTypeSaveReqVO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsAuxiliaryTypeDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * FMS 辅助核算类别 Service 接口
 *
 * @author 智云
 */
public interface FmsAuxiliaryTypeService {

    Long createAuxiliaryType(@Valid FmsAuxiliaryTypeSaveReqVO createReqVO);

    void updateAuxiliaryType(@Valid FmsAuxiliaryTypeSaveReqVO updateReqVO);

    void deleteAuxiliaryType(Long accountSetId, Long id);

    List<FmsAuxiliaryTypeDO> getAuxiliaryTypeList(Long accountSetId);

}
