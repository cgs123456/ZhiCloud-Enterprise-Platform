package cn.iocoder.yudao.module.erp.dal.mysql.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.consolidation.ErpConsolidationWorksheetPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpConsolidationWorksheetDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 合并工作底稿 Mapper（P1-合并报表引擎）
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpConsolidationWorksheetMapper extends BaseMapperX<ErpConsolidationWorksheetDO> {

    default List<ErpConsolidationWorksheetDO> selectListByPeriod(String consolidationPeriod) {
        return selectList(ErpConsolidationWorksheetDO::getConsolidationPeriod, consolidationPeriod);
    }

    default List<ErpConsolidationWorksheetDO> selectListByPeriodAndStatus(String consolidationPeriod, Integer status) {
        return selectList(new LambdaQueryWrapperX<ErpConsolidationWorksheetDO>()
                .eq(ErpConsolidationWorksheetDO::getConsolidationPeriod, consolidationPeriod)
                .eq(ErpConsolidationWorksheetDO::getStatus, status)
                .orderByAsc(ErpConsolidationWorksheetDO::getEliminationType)
                .orderByAsc(ErpConsolidationWorksheetDO::getId));
    }

    default List<ErpConsolidationWorksheetDO> selectListByPeriodAndParent(String consolidationPeriod, Long parentCompanyId) {
        return selectList(new LambdaQueryWrapperX<ErpConsolidationWorksheetDO>()
                .eq(ErpConsolidationWorksheetDO::getConsolidationPeriod, consolidationPeriod)
                .eq(ErpConsolidationWorksheetDO::getParentCompanyId, parentCompanyId)
                .orderByAsc(ErpConsolidationWorksheetDO::getEliminationType)
                .orderByAsc(ErpConsolidationWorksheetDO::getId));
    }

    default int deleteByPeriodAndParentAndSubsidiary(String consolidationPeriod, Long parentCompanyId, Long subsidiaryCompanyId) {
        return delete(new LambdaQueryWrapperX<ErpConsolidationWorksheetDO>()
                .eq(ErpConsolidationWorksheetDO::getConsolidationPeriod, consolidationPeriod)
                .eq(ErpConsolidationWorksheetDO::getParentCompanyId, parentCompanyId)
                .eq(ErpConsolidationWorksheetDO::getSubsidiaryCompanyId, subsidiaryCompanyId));
    }

    default PageResult<ErpConsolidationWorksheetDO> selectPage(ErpConsolidationWorksheetPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpConsolidationWorksheetDO>()
                .eqIfPresent(ErpConsolidationWorksheetDO::getConsolidationPeriod, reqVO.getConsolidationPeriod())
                .eqIfPresent(ErpConsolidationWorksheetDO::getParentCompanyId, reqVO.getParentCompanyId())
                .eqIfPresent(ErpConsolidationWorksheetDO::getSubsidiaryCompanyId, reqVO.getSubsidiaryCompanyId())
                .eqIfPresent(ErpConsolidationWorksheetDO::getEliminationType, reqVO.getEliminationType())
                .eqIfPresent(ErpConsolidationWorksheetDO::getStatus, reqVO.getStatus())
                .orderByAsc(ErpConsolidationWorksheetDO::getConsolidationPeriod)
                .orderByAsc(ErpConsolidationWorksheetDO::getEliminationType)
                .orderByDesc(ErpConsolidationWorksheetDO::getId));
    }

}
