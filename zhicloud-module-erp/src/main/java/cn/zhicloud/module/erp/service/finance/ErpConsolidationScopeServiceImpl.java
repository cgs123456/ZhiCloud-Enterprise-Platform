package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.finance.vo.consolidation.ErpConsolidationScopePageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.consolidation.ErpConsolidationScopeSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpConsolidationScopeDO;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpConsolidationScopeMapper;
import cn.zhicloud.module.erp.enums.finance.ErpConsolidationScopeStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Objects;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 合并范围 Service 实现类（P1-合并报表引擎）
 *
 * @author 智云
 */
@Service
@Validated
public class ErpConsolidationScopeServiceImpl implements ErpConsolidationScopeService {

    @Resource
    private ErpConsolidationScopeMapper consolidationScopeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createScope(ErpConsolidationScopeSaveReqVO createReqVO) {
        // 校验母子公司唯一
        validateParentSubsidiaryUnique(null, createReqVO.getParentCompanyId(), createReqVO.getSubsidiaryCompanyId());
        // 插入
        ErpConsolidationScopeDO scope = BeanUtils.toBean(createReqVO, ErpConsolidationScopeDO.class);
        if (scope.getStatus() == null) {
            scope.setStatus(ErpConsolidationScopeStatusEnum.ENABLED.getStatus());
        }
        consolidationScopeMapper.insert(scope);
        return scope.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateScope(ErpConsolidationScopeSaveReqVO updateReqVO) {
        // 校验存在
        validateScopeExists(updateReqVO.getId());
        // 校验母子公司唯一
        validateParentSubsidiaryUnique(updateReqVO.getId(), updateReqVO.getParentCompanyId(), updateReqVO.getSubsidiaryCompanyId());
        // 更新
        ErpConsolidationScopeDO updateObj = BeanUtils.toBean(updateReqVO, ErpConsolidationScopeDO.class);
        consolidationScopeMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteScope(Long id) {
        validateScopeExists(id);
        consolidationScopeMapper.deleteById(id);
    }

    @Override
    public ErpConsolidationScopeDO getScope(Long id) {
        return consolidationScopeMapper.selectById(id);
    }

    @Override
    public PageResult<ErpConsolidationScopeDO> getScopePage(ErpConsolidationScopePageReqVO pageReqVO) {
        return consolidationScopeMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpConsolidationScopeDO> getEnabledScopeListByParent(Long parentCompanyId) {
        return consolidationScopeMapper.selectEnabledListByParent(parentCompanyId);
    }

    @Override
    public List<ErpConsolidationScopeDO> getEnabledScopeList() {
        return consolidationScopeMapper.selectListByStatus(ErpConsolidationScopeStatusEnum.ENABLED.getStatus());
    }

    // ==================== 内部辅助方法 ====================

    private ErpConsolidationScopeDO validateScopeExists(Long id) {
        if (id == null) {
            throw exception(CONSOLIDATION_SCOPE_NOT_EXISTS);
        }
        ErpConsolidationScopeDO scope = consolidationScopeMapper.selectById(id);
        if (scope == null) {
            throw exception(CONSOLIDATION_SCOPE_NOT_EXISTS);
        }
        return scope;
    }

    private void validateParentSubsidiaryUnique(Long id, Long parentCompanyId, Long subsidiaryCompanyId) {
        ErpConsolidationScopeDO existing = consolidationScopeMapper.selectByParentAndSubsidiary(
                parentCompanyId, subsidiaryCompanyId);
        if (existing != null && !Objects.equals(existing.getId(), id)) {
            throw exception(CONSOLIDATION_SCOPE_DUPLICATE);
        }
    }

}
