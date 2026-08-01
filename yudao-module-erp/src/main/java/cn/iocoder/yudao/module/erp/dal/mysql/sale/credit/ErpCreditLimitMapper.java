package cn.iocoder.yudao.module.erp.dal.mysql.sale.credit;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.credit.vo.ErpCreditLimitPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.credit.ErpCreditLimitDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * ERP 客户信用额度 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpCreditLimitMapper extends BaseMapperX<ErpCreditLimitDO> {

    default PageResult<ErpCreditLimitDO> selectPage(ErpCreditLimitPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpCreditLimitDO>()
                .eqIfPresent(ErpCreditLimitDO::getCustomerId, reqVO.getCustomerId())
                .eqIfPresent(ErpCreditLimitDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(ErpCreditLimitDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ErpCreditLimitDO::getId));
    }

    default ErpCreditLimitDO selectByCustomerId(Long customerId) {
        return selectOne(ErpCreditLimitDO::getCustomerId, customerId);
    }

    /**
     * 原子冻结信用额度：used_amount += amount, available_amount -= amount
     * 仅当未超出信用额度（credit_limit <= 0 视为无限）且非冻结状态时更新成功
     *
     * @param id 信用额度记录编号
     * @param amount 冻结金额（正数）
     * @return 影响行数（0 表示额度不足或已冻结或并发冲突）
     */
    @Update("UPDATE erp_credit_limit SET used_amount = COALESCE(used_amount, 0) + #{amount}, " +
            "available_amount = GREATEST(COALESCE(credit_limit, 0) - (COALESCE(used_amount, 0) + #{amount}), 0) " +
            "WHERE id = #{id} AND deleted = 0 AND status != 30 " +
            "AND (COALESCE(credit_limit, 0) <= 0 OR COALESCE(used_amount, 0) + #{amount} <= credit_limit)")
    int updateUsedAmountIncrement(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /**
     * 原子释放信用额度：used_amount -= amount, available_amount += amount
     * used_amount 不低于 0
     *
     * @param id 信用额度记录编号
     * @param amount 释放金额（正数）
     * @return 影响行数
     */
    @Update("UPDATE erp_credit_limit SET used_amount = CASE WHEN COALESCE(used_amount, 0) >= #{amount} " +
            "THEN COALESCE(used_amount, 0) - #{amount} ELSE 0 END, " +
            "available_amount = CASE WHEN COALESCE(used_amount, 0) >= #{amount} " +
            "THEN COALESCE(available_amount, 0) + #{amount} ELSE COALESCE(credit_limit, 0) END " +
            "WHERE id = #{id} AND deleted = 0")
    int updateUsedAmountDecrement(@Param("id") Long id, @Param("amount") BigDecimal amount);

}
