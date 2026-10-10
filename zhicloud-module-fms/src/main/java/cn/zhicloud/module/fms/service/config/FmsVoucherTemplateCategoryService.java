package cn.zhicloud.module.fms.service.config;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.fms.controller.admin.config.vo.vouchertemplate.FmsVoucherTemplateCategorySaveReqVO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsVoucherTemplateCategoryDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * FMS 凭证模板分类 Service 接口
 *
 * @author 智云
 */
public interface FmsVoucherTemplateCategoryService {

    Long createVoucherTemplateCategory(@Valid FmsVoucherTemplateCategorySaveReqVO createReqVO);

    void updateVoucherTemplateCategory(@Valid FmsVoucherTemplateCategorySaveReqVO updateReqVO);

    void deleteVoucherTemplateCategory(Long accountSetId, Long id);

    List<FmsVoucherTemplateCategoryDO> getVoucherTemplateCategoryList(Long accountSetId);

}
