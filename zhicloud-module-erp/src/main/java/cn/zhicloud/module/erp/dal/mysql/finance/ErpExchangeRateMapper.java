package cn.zhicloud.module.erp.dal.mysql.finance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.controller.admin.finance.vo.exchangerate.ErpExchangeRatePageReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpExchangeRateDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

/**
 * ERP 汇率 Mapper
 *
 * @author 智云
 */
@Mapper
public interface ErpExchangeRateMapper extends BaseMapperX<ErpExchangeRateDO> {

    default PageResult<ErpExchangeRateDO> selectPage(ErpExchangeRatePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpExchangeRateDO>()
                .eqIfPresent(ErpExchangeRateDO::getFromCurrencyId, reqVO.getFromCurrencyId())
                .eqIfPresent(ErpExchangeRateDO::getToCurrencyId, reqVO.getToCurrencyId())
                .orderByDesc(ErpExchangeRateDO::getEffectiveDate));
    }

    /**
     * 查询某币种对在指定日期有效的最新汇率
     */
    default ErpExchangeRateDO selectLatestRate(Long fromCurrencyId, Long toCurrencyId, LocalDate date) {
        LocalDate target = date == null ? LocalDate.now() : date;
        return selectOne(new LambdaQueryWrapperX<ErpExchangeRateDO>()
                .eq(ErpExchangeRateDO::getFromCurrencyId, fromCurrencyId)
                .eq(ErpExchangeRateDO::getToCurrencyId, toCurrencyId)
                .le(ErpExchangeRateDO::getEffectiveDate, target)
                .and(w -> w.ge(ErpExchangeRateDO::getExpiryDate, target)
                        .or().isNull(ErpExchangeRateDO::getExpiryDate))
                .orderByDesc(ErpExchangeRateDO::getEffectiveDate)
                .last("LIMIT 1"));
    }

    default List<ErpExchangeRateDO> selectListByCurrency(Long fromCurrencyId, Long toCurrencyId) {
        return selectList(new LambdaQueryWrapperX<ErpExchangeRateDO>()
                .eqIfPresent(ErpExchangeRateDO::getFromCurrencyId, fromCurrencyId)
                .eqIfPresent(ErpExchangeRateDO::getToCurrencyId, toCurrencyId)
                .orderByDesc(ErpExchangeRateDO::getEffectiveDate));
    }

}
