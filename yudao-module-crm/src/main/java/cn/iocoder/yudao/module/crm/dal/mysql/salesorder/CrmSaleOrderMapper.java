package cn.iocoder.yudao.module.crm.dal.mysql.salesorder;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.crm.controller.admin.salesorder.vo.CrmSaleOrderPageReqVO;
import cn.iocoder.yudao.module.crm.dal.dataobject.salesorder.CrmSaleOrderDO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

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
