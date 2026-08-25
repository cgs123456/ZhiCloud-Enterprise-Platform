package cn.zhicloud.module.mes.dal.mysql.wm.productproduce;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.module.mes.dal.dataobject.wm.productproduce.MesWmProductProduceDetailDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES 生产入库明细 Mapper
 *
 * @author 智云
 */
@Mapper
public interface MesWmProductProduceDetailMapper extends BaseMapperX<MesWmProductProduceDetailDO> {

    default List<MesWmProductProduceDetailDO> selectListByLineId(Long lineId) {
        return selectList(MesWmProductProduceDetailDO::getLineId, lineId);
    }

}
