package cn.iocoder.yudao.module.erp.dal.mysql.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.consolidation.ErpConsolidationScopePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpConsolidationScopeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 合并范围 Mapper（P1-合并报表引擎）
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpConsolidationScopeMapper extends BaseMapperX<ErpConsolidationScopeDO> {

    default ErpConsolidationScopeDO selectByParentAndSubsidiary(Long parentCompanyId, Long subsidiaryCompanyId) {
        return selectOne(new LambdaQueryWrapperX<ErpConsolidationScopeDO>()
                .eq(ErpConsolidationScopeDO::getParentCompanyId, parentCompanyId)
                .eq(ErpConsolidationScopeDO::getSubsidiaryCompanyId, subsidiaryCompanyId));
    }

    default List<ErpConsolidationScopeDO> selectListByParent(Long parentCompanyId) {
        return selectList(ErpConsolidationScopeDO::getParentCompanyId, parentCompanyId);
    }

    default List<ErpConsolidationScopeDO> selectListByStatus(Integer status) {
        return selectList(ErpConsolidationScopeDO::getStatus, status);
    }

    default List<ErpConsolidationScopeDO> selectEnabledListByParent(Long parentCompanyId) {
        return selectList(new LambdaQueryWrapperX<ErpConsolidationScopeDO>()
                .eq(ErpConsolidationScopeDO::getParentCompanyId, parentCompanyId)
                .eq(ErpConsolidationScopeDO::getStatus, 10));
    }

    default PageResult<ErpConsolidationScopeDO> selectPage(ErpConsolidationScopePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpConsolidationScopeDO>()
                .eqIfPresent(ErpConsolidationScopeDO::getParentCompanyId, reqVO.getParentCompanyId())
                .eqIfPresent(ErpConsolidationScopeDO::getSubsidiaryCompanyId, reqVO.getSubsidiaryCompanyId())
                .eqIfPresent(ErpConsolidationScopeDO::getConsolidationMethod, reqVO.getConsolidationMethod())
                .eqIfPresent(ErpConsolidationScopeDO::getStatus, reqVO.getStatus())
                .orderByDesc(ErpConsolidationScopeDO::getId));
    }

}
