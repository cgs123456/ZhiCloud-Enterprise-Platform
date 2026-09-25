package cn.zhicloud.module.erp.dal.mysql.accounting;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.dal.dataobject.accounting.ErpAccountingEventDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 会计事件 Mapper。
 *
 * @author 智云
 */
@Mapper
public interface ErpAccountingEventMapper extends BaseMapperX<ErpAccountingEventDO> {

    default ErpAccountingEventDO selectByEventId(String eventId) {
        return selectOne(ErpAccountingEventDO::getEventId, eventId);
    }

    default ErpAccountingEventDO selectByIdempotencyKey(String idempotencyKey) {
        return selectOne(ErpAccountingEventDO::getIdempotencyKey, idempotencyKey);
    }

    default List<ErpAccountingEventDO> selectListByStatus(Integer status) {
        return selectList(ErpAccountingEventDO::getStatus, status);
    }

    /**
     * 查询指定来源的原始会计事件（非冲销事件）。
     */
    default ErpAccountingEventDO selectBySource(String eventType, String sourceDocType, Long sourceDocId,
                                                 Long sourceDocLineId, Integer sourceRevision,
                                                 Integer sequenceNo, Long reversalOfEventId) {
        return selectOne(new LambdaQueryWrapperX<ErpAccountingEventDO>()
                .eq(ErpAccountingEventDO::getEventType, eventType)
                .eq(ErpAccountingEventDO::getSourceDocType, sourceDocType)
                .eq(ErpAccountingEventDO::getSourceDocId, sourceDocId)
                .eq(ErpAccountingEventDO::getSourceDocLineId, sourceDocLineId)
                .eq(ErpAccountingEventDO::getSourceRevision, sourceRevision)
                .eq(ErpAccountingEventDO::getSequenceNo, sequenceNo)
                .eq(ErpAccountingEventDO::getReversalOfEventId, reversalOfEventId));
    }


    /**
     * 查询同一来源单据、同一事件类型下的全部事件（含冲销事件），用于金额勾稽。
     *
     * <p>必须包含冲销事件：带符号求和依赖原事件与其冲销事件相互抵消来释放额度。
     * 若在此过滤掉冲销事件，反审核后重新审核会被误判为「同一额度被占用两次」。
     *
     * <p>查询条件 (tenant_id, event_type, source_doc_type, source_doc_id) 是
     * uk_erp_accounting_event_source 的最左前缀，已有索引覆盖，无需额外建索引。
     */
    default List<ErpAccountingEventDO> selectListForReconciliation(String eventType, String sourceDocType,
                                                                  Long sourceDocId) {
        return selectList(new LambdaQueryWrapperX<ErpAccountingEventDO>()
                .eq(ErpAccountingEventDO::getEventType, eventType)
                .eq(ErpAccountingEventDO::getSourceDocType, sourceDocType)
                .eq(ErpAccountingEventDO::getSourceDocId, sourceDocId));
    }
}
