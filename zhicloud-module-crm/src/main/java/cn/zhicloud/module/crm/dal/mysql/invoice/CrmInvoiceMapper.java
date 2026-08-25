package cn.zhicloud.module.crm.dal.mysql.invoice;

import cn.hutool.core.collection.CollUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.crm.controller.admin.invoice.vo.CrmInvoicePageReqVO;
import cn.zhicloud.module.crm.dal.dataobject.invoice.CrmInvoiceDO;
import cn.zhicloud.module.crm.enums.common.CrmAuditStatusEnum;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.zhicloud.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * CRM 开票 Mapper
 *
 * @author 智云
 */
@Mapper
public interface CrmInvoiceMapper extends BaseMapperX<CrmInvoiceDO> {

    default CrmInvoiceDO selectByNo(String no) {
        return selectOne(CrmInvoiceDO::getNo, no);
    }

    default PageResult<CrmInvoiceDO> selectPage(CrmInvoicePageReqVO pageReqVO) {
        return selectPage(pageReqVO, new LambdaQueryWrapperX<CrmInvoiceDO>()
                .eqIfPresent(CrmInvoiceDO::getNo, pageReqVO.getNo())
                .eqIfPresent(CrmInvoiceDO::getCustomerId, pageReqVO.getCustomerId())
                .eqIfPresent(CrmInvoiceDO::getContractId, pageReqVO.getContractId())
                .eqIfPresent(CrmInvoiceDO::getInvoiceType, pageReqVO.getInvoiceType())
                .eqIfPresent(CrmInvoiceDO::getAuditStatus, pageReqVO.getAuditStatus())
                .likeIfPresent(CrmInvoiceDO::getBuyerName, pageReqVO.getBuyerName())
                .orderByDesc(CrmInvoiceDO::getId));
    }

    default PageResult<CrmInvoiceDO> selectPageByContractId(CrmInvoicePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CrmInvoiceDO>()
                .eq(CrmInvoiceDO::getContractId, reqVO.getContractId()) // 必须传递
                .eqIfPresent(CrmInvoiceDO::getNo, reqVO.getNo())
                .eqIfPresent(CrmInvoiceDO::getAuditStatus, reqVO.getAuditStatus())
                .orderByDesc(CrmInvoiceDO::getId));
    }

    default List<CrmInvoiceDO> selectListByContractIdAndStatus(Long contractId, Collection<Integer> auditStatuses) {
        return selectList(new LambdaQueryWrapperX<CrmInvoiceDO>()
                .eq(CrmInvoiceDO::getContractId, contractId)
                .in(CrmInvoiceDO::getAuditStatus, auditStatuses));
    }

    default Map<Long, BigDecimal> selectInvoicePriceMapByContractId(Collection<Long> contractIds) {
        if (CollUtil.isEmpty(contractIds)) {
            return Collections.emptyMap();
        }
        // SQL sum 查询
        List<Map<String, Object>> result = selectMaps(new QueryWrapper<CrmInvoiceDO>()
                .select("contract_id, SUM(amount_with_tax) AS total_price")
                .in("audit_status", CrmAuditStatusEnum.DRAFT.getStatus(), // 草稿 + 审批中 + 审批通过
                        CrmAuditStatusEnum.PROCESS.getStatus(), CrmAuditStatusEnum.APPROVE.getStatus())
                .groupBy("contract_id")
                .in("contract_id", contractIds));
        // 获得金额
        return convertMap(result, obj -> (Long) obj.get("contract_id"), obj -> (BigDecimal) obj.get("total_price"));
    }

    default Long selectCountByContractId(Long contractId) {
        return selectCount(CrmInvoiceDO::getContractId, contractId);
    }

}
