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
                                                 Long reversalOfEventId) {
        return selectOne(new LambdaQueryWrapperX<ErpAccountingEventDO>()
                .eq(ErpAccountingEventDO::getEventType, eventType)
                .eq(ErpAccountingEventDO::getSourceDocType, sourceDocType)
                .eq(ErpAccountingEventDO::getSourceDocId, sourceDocId)
                .eq(ErpAccountingEventDO::getSourceDocLineId, sourceDocLineId)
                .eq(ErpAccountingEventDO::getSourceRevision, sourceRevision)
                .eq(ErpAccountingEventDO::getReversalOfEventId, reversalOfEventId));
    }

}
