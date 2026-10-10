package cn.zhicloud.module.fms.service.config;

import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.fms.controller.admin.config.vo.vouchertemplate.FmsVoucherTemplateCategorySaveReqVO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsVoucherTemplateCategoryDO;
import cn.zhicloud.module.fms.dal.mysql.config.FmsVoucherTemplateCategoryMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.fms.enums.ErrorCodeConstants.VOUCHER_TEMPLATE_CATEGORY_NOT_EXISTS;

/**
 * FMS 凭证模板分类 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class FmsVoucherTemplateCategoryServiceImpl implements FmsVoucherTemplateCategoryService {

    @Resource
    private FmsVoucherTemplateCategoryMapper voucherTemplateCategoryMapper;

    @Override
    public Long createVoucherTemplateCategory(FmsVoucherTemplateCategorySaveReqVO createReqVO) {
        FmsVoucherTemplateCategoryDO entity = BeanUtils.toBean(createReqVO, FmsVoucherTemplateCategoryDO.class);
        voucherTemplateCategoryMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void updateVoucherTemplateCategory(FmsVoucherTemplateCategorySaveReqVO updateReqVO) {
        validateExists(updateReqVO.getAccountSetId(), updateReqVO.getId());
        FmsVoucherTemplateCategoryDO updateObj = BeanUtils.toBean(updateReqVO, FmsVoucherTemplateCategoryDO.class);
        voucherTemplateCategoryMapper.updateById(updateObj);
    }

    @Override
    public void deleteVoucherTemplateCategory(Long accountSetId, Long id) {
        validateExists(accountSetId, id);
        voucherTemplateCategoryMapper.deleteById(id);
    }

    @Override
    public List<FmsVoucherTemplateCategoryDO> getVoucherTemplateCategoryList(Long accountSetId) {
        return voucherTemplateCategoryMapper.selectListByAccountSet(accountSetId);
    }

    private void validateExists(Long accountSetId, Long id) {
        if (voucherTemplateCategoryMapper.selectByAccountSetAndId(accountSetId, id) == null) {
            throw exception(VOUCHER_TEMPLATE_CATEGORY_NOT_EXISTS);
        }
    }

}
