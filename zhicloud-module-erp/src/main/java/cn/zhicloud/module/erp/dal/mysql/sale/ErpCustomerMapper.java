package cn.zhicloud.module.erp.dal.mysql.sale;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.controller.admin.sale.vo.customer.ErpCustomerPageReqVO;
import cn.zhicloud.module.erp.dal.dataobject.sale.ErpCustomerDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

/**
 * ERP 客户 Mapper
 *
 * @author 智云
 */
@Mapper
public interface ErpCustomerMapper extends BaseMapperX<ErpCustomerDO> {

    default PageResult<ErpCustomerDO> selectPage(ErpCustomerPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpCustomerDO>()
                .likeIfPresent(ErpCustomerDO::getName, reqVO.getName())
                .eqIfPresent(ErpCustomerDO::getMobile, reqVO.getMobile())
                .eqIfPresent(ErpCustomerDO::getTelephone, reqVO.getTelephone())
                .orderByDesc(ErpCustomerDO::getId));
    }

    default List<ErpCustomerDO> selectListByStatus(Integer status) {
        return selectList(ErpCustomerDO::getStatus, status);
    }

    /**
     * 原子冻结客户信用：used_credit = used_credit + amount
     * 仅当未超出信用额度（credit_limit <= 0 视为无限额度）时才更新成功，避免并发超额
     *
     * @param id 客户编号
     * @param amount 冻结金额（正数）
     * @return 影响行数（0 表示额度不足或并发冲突）
     */
    @Update("UPDATE erp_customer SET used_credit = COALESCE(used_credit, 0) + #{amount} " +
            "WHERE id = #{id} AND deleted = 0 " +
            "AND (COALESCE(credit_limit, 0) <= 0 OR COALESCE(used_credit, 0) + #{amount} <= credit_limit)")
    int updateUsedCreditIncrement(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /**
     * 原子释放客户信用：used_credit = used_credit - amount，且不低于 0
     *
     * @param id 客户编号
     * @param amount 释放金额（正数）
     * @return 影响行数
     */
    @Update("UPDATE erp_customer SET used_credit = CASE WHEN COALESCE(used_credit, 0) >= #{amount} " +
            "THEN COALESCE(used_credit, 0) - #{amount} ELSE 0 END " +
            "WHERE id = #{id} AND deleted = 0")
    int updateUsedCreditDecrement(@Param("id") Long id, @Param("amount") BigDecimal amount);

}