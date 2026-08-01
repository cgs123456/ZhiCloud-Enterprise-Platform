package cn.iocoder.yudao.module.oa.service.approvaltemplate;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.oa.controller.admin.approvaltemplate.vo.OaApprovalTemplatePageReqVO;
import cn.iocoder.yudao.module.oa.controller.admin.approvaltemplate.vo.OaApprovalTemplateSaveReqVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.approvaltemplate.OaApprovalTemplateDO;
import cn.iocoder.yudao.module.oa.dal.mysql.approvaltemplate.OaApprovalTemplateMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.oa.enums.ErrorCodeConstants.OA_APPROVAL_TEMPLATE_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.oa.enums.ErrorCodeConstants.OA_APPROVAL_TEMPLATE_NOT_EXISTS;

/**
 * OA 审批模板 Service 实现类
 *
 * @author yudao
 */
@Service
@Validated
public class OaApprovalTemplateServiceImpl implements OaApprovalTemplateService {

    /**
     * 启用状态
     */
    private static final int STATUS_ENABLED = 0;

    @Resource
    private OaApprovalTemplateMapper approvalTemplateMapper;

    @Override
    public Long createApprovalTemplate(OaApprovalTemplateSaveReqVO createReqVO) {
        // 校验模板编码唯一
        validateCodeUnique(null, createReqVO.getCode());
        // 插入审批模板
        OaApprovalTemplateDO approvalTemplate = BeanUtils.toBean(createReqVO, OaApprovalTemplateDO.class);
        if (approvalTemplate.getStatus() == null) {
            approvalTemplate.setStatus(STATUS_ENABLED);
        }
        if (approvalTemplate.getSort() == null) {
            approvalTemplate.setSort(0);
        }
        if (approvalTemplate.getUsageCount() == null) {
            approvalTemplate.setUsageCount(0);
        }
        approvalTemplateMapper.insert(approvalTemplate);
        return approvalTemplate.getId();
    }

    @Override
    public void updateApprovalTemplate(OaApprovalTemplateSaveReqVO updateReqVO) {
        // 校验存在
        validateApprovalTemplateExists(updateReqVO.getId());
        // 校验模板编码唯一
        validateCodeUnique(updateReqVO.getId(), updateReqVO.getCode());
        // 更新审批模板
        OaApprovalTemplateDO updateObj = BeanUtils.toBean(updateReqVO, OaApprovalTemplateDO.class);
        approvalTemplateMapper.updateById(updateObj);
    }

    @Override
    public void deleteApprovalTemplate(Long id) {
        validateApprovalTemplateExists(id);
        approvalTemplateMapper.deleteById(id);
    }

    @Override
    public OaApprovalTemplateDO getApprovalTemplate(Long id) {
        return approvalTemplateMapper.selectById(id);
    }

    @Override
    public PageResult<OaApprovalTemplateDO> getApprovalTemplatePage(OaApprovalTemplatePageReqVO pageReqVO) {
        return approvalTemplateMapper.selectPage(pageReqVO);
    }

    @Override
    public List<OaApprovalTemplateDO> getEnabledList() {
        return approvalTemplateMapper.selectList(new LambdaQueryWrapperX<OaApprovalTemplateDO>()
                .eq(OaApprovalTemplateDO::getStatus, STATUS_ENABLED)
                .orderByAsc(OaApprovalTemplateDO::getSort));
    }

    @Override
    public void incrementUsageCount(Long id) {
        // 校验存在
        validateApprovalTemplateExists(id);
        // 累加使用次数（原子操作）
        LambdaUpdateWrapper<OaApprovalTemplateDO> updateWrapper = new LambdaUpdateWrapper<OaApprovalTemplateDO>()
                .eq(OaApprovalTemplateDO::getId, id)
                .setSql("usage_count = usage_count + 1");
        approvalTemplateMapper.update(null, updateWrapper);
    }

    private void validateCodeUnique(Long id, String code) {
        if (code == null) {
            return;
        }
        OaApprovalTemplateDO approvalTemplate = approvalTemplateMapper.selectByCode(code);
        if (approvalTemplate == null) {
            return;
        }
        if (id == null || !approvalTemplate.getId().equals(id)) {
            throw exception(OA_APPROVAL_TEMPLATE_CODE_DUPLICATE);
        }
    }

    private OaApprovalTemplateDO validateApprovalTemplateExists(Long id) {
        OaApprovalTemplateDO approvalTemplate = approvalTemplateMapper.selectById(id);
        if (approvalTemplate == null) {
            throw exception(OA_APPROVAL_TEMPLATE_NOT_EXISTS);
        }
        return approvalTemplate;
    }

}
