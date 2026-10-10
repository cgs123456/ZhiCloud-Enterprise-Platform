package cn.zhicloud.module.fms.service.config;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.fms.controller.admin.config.vo.auxiliary.FmsAuxiliaryItemSaveReqVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.auxiliary.FmsAuxiliaryItemPageReqVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.auxiliary.FmsAuxiliaryItemImportRespVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.auxiliary.FmsAuxiliaryItemImportExcelVO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsAuxiliaryItemDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * FMS 辅助核算项目 Service 接口
 *
 * @author 智云
 */
public interface FmsAuxiliaryItemService {

    Long createAuxiliaryItem(@Valid FmsAuxiliaryItemSaveReqVO createReqVO);

    void updateAuxiliaryItem(@Valid FmsAuxiliaryItemSaveReqVO updateReqVO);

    void deleteAuxiliaryItem(Long accountSetId, Long id);

    void deleteAuxiliaryItemList(Long accountSetId, List<Long> ids);

    PageResult<FmsAuxiliaryItemDO> getAuxiliaryItemPage(FmsAuxiliaryItemPageReqVO pageReqVO);

    void updateAuxiliaryItemStatus(Long accountSetId, Long id, Integer status);

    List<FmsAuxiliaryItemDO> getAuxiliaryItemList(Long accountSetId);

    FmsAuxiliaryItemImportRespVO importAuxiliaryItemList(Long accountSetId, Long auxiliaryTypeId, List<FmsAuxiliaryItemImportExcelVO> list, boolean updateSupport);

}
