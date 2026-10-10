package cn.zhicloud.module.fms.service.config;

import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.fms.controller.admin.config.vo.vouchertemplate.FmsVoucherTemplateSaveReqVO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsVoucherTemplateDO;
import cn.zhicloud.module.fms.dal.mysql.config.FmsVoucherTemplateMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.fms.enums.ErrorCodeConstants.VOUCHER_TEMPLATE_NOT_EXISTS;

/**
 * FMS 凭证模板 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class FmsVoucherTemplateServiceImpl implements FmsVoucherTemplateService {

    @Resource
    private FmsVoucherTemplateMapper voucherTemplateMapper;

    @Override
    public Long createVoucherTemplate(FmsVoucherTemplateSaveReqVO createReqVO) {
        FmsVoucherTemplateDO entity = BeanUtils.toBean(createReqVO, FmsVoucherTemplateDO.class);
        voucherTemplateMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void updateVoucherTemplate(FmsVoucherTemplateSaveReqVO updateReqVO) {
        validateExists(updateReqVO.getAccountSetId(), updateReqVO.getId());
        FmsVoucherTemplateDO updateObj = BeanUtils.toBean(updateReqVO, FmsVoucherTemplateDO.class);
        voucherTemplateMapper.updateById(updateObj);
    }

    @Override
    public void deleteVoucherTemplate(Long accountSetId, Long id) {
        validateExists(accountSetId, id);
        voucherTemplateMapper.deleteById(id);
    }

    @Override
    public List<FmsVoucherTemplateDO> getVoucherTemplateList(Long accountSetId) {
        return voucherTemplateMapper.selectListByAccountSet(accountSetId);
    }

    private void validateExists(Long accountSetId, Long id) {
        if (voucherTemplateMapper.selectByAccountSetAndId(accountSetId, id) == null) {
            throw exception(VOUCHER_TEMPLATE_NOT_EXISTS);
        }
    }

}
