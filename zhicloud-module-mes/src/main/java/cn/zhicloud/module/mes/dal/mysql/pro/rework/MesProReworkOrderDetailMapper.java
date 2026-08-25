package cn.zhicloud.module.mes.dal.mysql.pro.rework;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.mes.dal.dataobject.pro.rework.MesProReworkOrderDetailDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES 返工工单明细 Mapper
 *
 * @author 智云
 */
@Mapper
public interface MesProReworkOrderDetailMapper extends BaseMapperX<MesProReworkOrderDetailDO> {

    default List<MesProReworkOrderDetailDO> selectListByReworkOrderId(Long reworkOrderId) {
        return selectList(new LambdaQueryWrapperX<MesProReworkOrderDetailDO>()
                .eq(MesProReworkOrderDetailDO::getReworkOrderId, reworkOrderId)
                .orderByAsc(MesProReworkOrderDetailDO::getSort));
    }

    default void deleteByReworkOrderId(Long reworkOrderId) {
        delete(MesProReworkOrderDetailDO::getReworkOrderId, reworkOrderId);
    }

}
