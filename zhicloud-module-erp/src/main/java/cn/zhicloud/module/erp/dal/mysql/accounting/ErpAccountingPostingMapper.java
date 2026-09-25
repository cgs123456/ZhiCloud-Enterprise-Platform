package cn.zhicloud.module.erp.dal.mysql.accounting;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.dal.dataobject.accounting.ErpAccountingPostingDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 会计过账结果 Mapper。
 *
 * @author 智云
 */
@Mapper
public interface ErpAccountingPostingMapper extends BaseMapperX<ErpAccountingPostingDO> {

    default List<ErpAccountingPostingDO> selectListByEventId(String eventId) {
        return selectList(ErpAccountingPostingDO::getEventId, eventId);
    }

    default List<ErpAccountingPostingDO> selectListByVoucherId(Long voucherId) {
        return selectList(ErpAccountingPostingDO::getVoucherId, voucherId);
    }

}
