package cn.zhicloud.module.crm.dal.mysql.salesorder;

import cn.hutool.core.collection.CollUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.crm.controller.admin.salesorder.vo.CrmSaleOrderPageReqVO;
import cn.zhicloud.module.crm.dal.dataobject.salesorder.CrmSaleOrderDO;
import cn.zhicloud.module.crm.enums.salesorder.CrmSaleOrderStatusEnum;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.zhicloud.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * CRM 销售订单 Mapper
 *
 * @author dhb52
 */
@Mapper
public interface CrmSaleOrderMapper extends BaseMapperX<CrmSaleOrderDO> {

    default CrmSaleOrderDO selectByNo(String no) {
        return selectOne(CrmSaleOrderDO::getNo, no);
    }

    /**
     * 条件更新（CAS）：仅当当前状态在 {@code fromStatuses} 内时才更新，返回受影响行数
     */
    default int updateByIdAndStatus(Long id, Collection<Integer> fromStatuses, CrmSaleOrderDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<CrmSaleOrderDO>()
                .eq(CrmSaleOrderDO::getId, id)
                .in(CrmSaleOrderDO::getStatus, fromStatuses));
    }

    /**
     * 按合同批量刷新未取消订单的付款状态（§12.260 D4）：已取消订单与同值订单跳过，
     * 返回受影响行数（0 行 = 无订单/全部相同/全已取消，均为合法出口）
     */
    default int updatePaymentStatusByContractId(Long contractId, Integer paymentStatus) {
        return update(new CrmSaleOrderDO().setPaymentStatus(paymentStatus), new LambdaUpdateWrapper<CrmSaleOrderDO>()
                .eq(CrmSaleOrderDO::getContractId, contractId)
                .ne(CrmSaleOrderDO::getStatus, CrmSaleOrderStatusEnum.CANCELED.getStatus())
                .and(wrapper -> wrapper.isNull(CrmSaleOrderDO::getPaymentStatus)
                        .or().ne(CrmSaleOrderDO::getPaymentStatus, paymentStatus)));
    }

    default PageResult<CrmSaleOrderDO> selectPage(CrmSaleOrderPageReqVO pageReqVO) {
        return selectPage(pageReqVO, new LambdaQueryWrapperX<CrmSaleOrderDO>()
                .likeIfPresent(CrmSaleOrderDO::getNo, pageReqVO.getNo())
                .eqIfPresent(CrmSaleOrderDO::getCustomerId, pageReqVO.getCustomerId())
                .eqIfPresent(CrmSaleOrderDO::getContractId, pageReqVO.getContractId())
                .eqIfPresent(CrmSaleOrderDO::getBusinessId, pageReqVO.getBusinessId())
                .eqIfPresent(CrmSaleOrderDO::getStatus, pageReqVO.getStatus())
                .eqIfPresent(CrmSaleOrderDO::getPaymentStatus, pageReqVO.getPaymentStatus())
                .betweenIfPresent(CrmSaleOrderDO::getCreateTime, pageReqVO.getCreateTime())
                .orderByDesc(CrmSaleOrderDO::getId));
    }

    default PageResult<CrmSaleOrderDO> selectPageByContractId(CrmSaleOrderPageReqVO pageReqVO) {
        return selectPage(pageReqVO, new LambdaQueryWrapperX<CrmSaleOrderDO>()
                .eq(CrmSaleOrderDO::getContractId, pageReqVO.getContractId())
                .likeIfPresent(CrmSaleOrderDO::getNo, pageReqVO.getNo())
                .eqIfPresent(CrmSaleOrderDO::getStatus, pageReqVO.getStatus())
                .orderByDesc(CrmSaleOrderDO::getId));
    }

    default PageResult<CrmSaleOrderDO> selectPageByCustomerId(CrmSaleOrderPageReqVO pageReqVO) {
        return selectPage(pageReqVO, new LambdaQueryWrapperX<CrmSaleOrderDO>()
                .eq(CrmSaleOrderDO::getCustomerId, pageReqVO.getCustomerId())
                .likeIfPresent(CrmSaleOrderDO::getNo, pageReqVO.getNo())
                .eqIfPresent(CrmSaleOrderDO::getStatus, pageReqVO.getStatus())
                .orderByDesc(CrmSaleOrderDO::getId));
    }

    default List<CrmSaleOrderDO> selectListByContractId(Long contractId) {
        return selectList(CrmSaleOrderDO::getContractId, contractId);
    }

    default Long selectCountByContractId(Long contractId) {
        return selectCount(CrmSaleOrderDO::getContractId, contractId);
    }

    /**
     * 根据合同编号，获得已下单金额汇总 Map
     *
     * @param contractIds 合同编号集合
     * @return Map<合同编号, 已下单金额>
     */
    default Map<Long, BigDecimal> selectSaleOrderPriceMapByContractId(Collection<Long> contractIds) {
        if (CollUtil.isEmpty(contractIds)) {
            return Collections.emptyMap();
        }
        // SQL sum 查询
        List<Map<String, Object>> result = selectMaps(new QueryWrapper<CrmSaleOrderDO>()
                .select("contract_id, SUM(final_amount) AS total_amount")
                .in("contract_id", contractIds)
                .groupBy("contract_id"));
        return convertMap(result, obj -> (Long) obj.get("contract_id"), obj -> (BigDecimal) obj.get("total_amount"));
    }

}
