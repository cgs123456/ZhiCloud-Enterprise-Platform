package cn.iocoder.yudao.module.mes.dal.mysql.pro.rework;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.rework.MesProReworkOrderDetailDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES 返工工单明细 Mapper
 *
 * @author 芋道源码
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
