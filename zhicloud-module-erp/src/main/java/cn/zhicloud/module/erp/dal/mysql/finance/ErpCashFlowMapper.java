package cn.zhicloud.module.erp.dal.mysql.finance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.controller.admin.finance.vo.cashflow.ErpCashFlowPageReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpCashFlowDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

/**
 * ERP 现金流记录 Mapper（P0-3）
 *
 * @author 智云
 */
@Mapper
public interface ErpCashFlowMapper extends BaseMapperX<ErpCashFlowDO> {

    default PageResult<ErpCashFlowDO> selectPage(ErpCashFlowPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpCashFlowDO>()
                .eqIfPresent(ErpCashFlowDO::getBizType, reqVO.getBizType())
                .eqIfPresent(ErpCashFlowDO::getBankAccountId, reqVO.getBankAccountId())
                .geIfPresent(ErpCashFlowDO::getOccurDate, reqVO.getOccurDateStart())
                .leIfPresent(ErpCashFlowDO::getOccurDate, reqVO.getOccurDateEnd())
                .orderByDesc(ErpCashFlowDO::getOccurDate)
                .orderByDesc(ErpCashFlowDO::getId));
    }

    default List<ErpCashFlowDO> selectListByPeriod(LocalDate startDate, LocalDate endDate) {
        return selectList(new LambdaQueryWrapperX<ErpCashFlowDO>()
                .ge(ErpCashFlowDO::getOccurDate, startDate)
                .le(ErpCashFlowDO::getOccurDate, endDate)
                .orderByAsc(ErpCashFlowDO::getOccurDate));
    }

}