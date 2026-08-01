package cn.iocoder.yudao.module.wms.dal.mysql.order.wave;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.wave.WmsWaveOrderDetailDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * WMS 波次单明细 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface WmsWaveOrderDetailMapper extends BaseMapperX<WmsWaveOrderDetailDO> {

    default List<WmsWaveOrderDetailDO> selectListByWaveOrderId(Long waveOrderId) {
        return selectList(WmsWaveOrderDetailDO::getWaveOrderId, waveOrderId);
    }

    default int deleteByWaveOrderId(Long waveOrderId) {
        return delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WmsWaveOrderDetailDO>()
                .eq(WmsWaveOrderDetailDO::getWaveOrderId, waveOrderId));
    }

}
