package cn.iocoder.yudao.module.wms.dal.mysql.order.wave;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.wms.controller.admin.order.wave.vo.order.WmsWaveOrderPageReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.wave.WmsWaveOrderDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * WMS 波次单 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface WmsWaveOrderMapper extends BaseMapperX<WmsWaveOrderDO> {

    default PageResult<WmsWaveOrderDO> selectPage(WmsWaveOrderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<WmsWaveOrderDO>()
                .likeIfPresent(WmsWaveOrderDO::getNo, reqVO.getNo())
                .eqIfPresent(WmsWaveOrderDO::getWarehouseId, reqVO.getWarehouseId())
                .eqIfPresent(WmsWaveOrderDO::getStrategy, reqVO.getStrategy())
                .eqIfPresent(WmsWaveOrderDO::getStatus, reqVO.getStatus())
                .likeIfPresent(WmsWaveOrderDO::getPicker, reqVO.getPicker())
                .betweenIfPresent(WmsWaveOrderDO::getOrderTime, reqVO.getOrderTime())
                .betweenIfPresent(WmsWaveOrderDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(WmsWaveOrderDO::getId));
    }

    default WmsWaveOrderDO selectByNo(String no) {
        return selectOne(WmsWaveOrderDO::getNo, no);
    }

}
