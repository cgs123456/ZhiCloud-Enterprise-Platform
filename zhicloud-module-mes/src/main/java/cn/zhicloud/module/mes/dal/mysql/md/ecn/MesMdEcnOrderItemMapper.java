package cn.zhicloud.module.mes.dal.mysql.md.ecn;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.mes.dal.dataobject.md.ecn.MesMdEcnOrderItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * MES ECN 工程变更明细 Mapper
 *
 * @author 智云
 */
@Mapper
public interface MesMdEcnOrderItemMapper extends BaseMapperX<MesMdEcnOrderItemDO> {

    default List<MesMdEcnOrderItemDO> selectListByEcnOrderId(Long ecnOrderId) {
        return selectList(new LambdaQueryWrapperX<MesMdEcnOrderItemDO>()
                .eq(MesMdEcnOrderItemDO::getEcnOrderId, ecnOrderId)
                .orderByAsc(MesMdEcnOrderItemDO::getId));
    }

    default List<MesMdEcnOrderItemDO> selectListByEcnOrderIds(Collection<Long> ecnOrderIds) {
        return selectList(new LambdaQueryWrapperX<MesMdEcnOrderItemDO>()
                .in(MesMdEcnOrderItemDO::getEcnOrderId, ecnOrderIds)
                .orderByAsc(MesMdEcnOrderItemDO::getId));
    }

    default int deleteByEcnOrderId(Long ecnOrderId) {
        return delete(new LambdaQueryWrapperX<MesMdEcnOrderItemDO>()
                .eq(MesMdEcnOrderItemDO::getEcnOrderId, ecnOrderId));
    }

}
