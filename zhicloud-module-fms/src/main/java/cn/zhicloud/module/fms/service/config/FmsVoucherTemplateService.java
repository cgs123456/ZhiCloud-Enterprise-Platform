package cn.zhicloud.module.fms.service.config;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.fms.controller.admin.config.vo.vouchertemplate.FmsVoucherTemplateSaveReqVO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsVoucherTemplateDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * FMS 凭证模板 Service 接口
 *
 * @author 智云
 */
public interface FmsVoucherTemplateService {

    Long createVoucherTemplate(@Valid FmsVoucherTemplateSaveReqVO createReqVO);

    void updateVoucherTemplate(@Valid FmsVoucherTemplateSaveReqVO updateReqVO);

    void deleteVoucherTemplate(Long accountSetId, Long id);

    List<FmsVoucherTemplateDO> getVoucherTemplateList(Long accountSetId);

}
