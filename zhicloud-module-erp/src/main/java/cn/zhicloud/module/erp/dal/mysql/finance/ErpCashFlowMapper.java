package cn.zhicloud.module.erp.dal.mysql.finance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.controller.admin.finance.vo.cashflow.ErpCashFlowPageReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpCashFlowDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.Collection;
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

    /**
     * 按期间 + 账户集合查流水（V195 账套视角现金流量表）
     *
     * <p>erp_cash_flow 无账簿关联，按账套查流水 = 账簿 → 账户集合 → 流水 IN 查询
     * （见 ErpBankAccountMapper#selectListByAccountBookIds）。空集合由调用方短路，
     * 不进本方法（IN(空集合) 语义歧义）。
     */
    default List<ErpCashFlowDO> selectListByPeriodAndBankAccountIds(
            LocalDate startDate, LocalDate endDate, Collection<Long> bankAccountIds) {
        // inIfPresent 必须排在 ge/le 之前：ge/le 是父类型 LambdaQueryWrapper 的方法，
        // 调用后链式返回父类型，会丢掉 LambdaQueryWrapperX 的 inIfPresent。
        return selectList(new LambdaQueryWrapperX<ErpCashFlowDO>()
                .inIfPresent(ErpCashFlowDO::getBankAccountId, bankAccountIds)
                .ge(ErpCashFlowDO::getOccurDate, startDate)
                .le(ErpCashFlowDO::getOccurDate, endDate)
                .orderByAsc(ErpCashFlowDO::getOccurDate));
    }

}