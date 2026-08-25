package cn.zhicloud.module.wms.dal.mysql.md.sn;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.wms.controller.admin.md.sn.vo.WmsSnPageReqVO;
import cn.zhicloud.module.wms.dal.dataobject.md.sn.WmsSnDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * WMS 序列号 Mapper
 *
 * @author 智云
 */
@Mapper
public interface WmsSnMapper extends BaseMapperX<WmsSnDO> {

    default PageResult<WmsSnDO> selectPage(WmsSnPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<WmsSnDO>()
                .likeIfPresent(WmsSnDO::getSn, reqVO.getSn())
                .eqIfPresent(WmsSnDO::getProductId, reqVO.getProductId())
                .eqIfPresent(WmsSnDO::getBatchId, reqVO.getBatchId())
                .eqIfPresent(WmsSnDO::getWarehouseId, reqVO.getWarehouseId())
                .eqIfPresent(WmsSnDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(WmsSnDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(WmsSnDO::getId));
    }

    default WmsSnDO selectBySn(String sn) {
        return selectOne(WmsSnDO::getSn, sn);
    }

    default List<WmsSnDO> selectListBySn(Collection<String> sns) {
        return selectList(WmsSnDO::getSn, sns);
    }

    default List<WmsSnDO> selectListByProductId(Long productId) {
        return selectList(WmsSnDO::getProductId, productId);
    }

    default List<WmsSnDO> selectListByInboundOrderId(Long inboundOrderId) {
        return selectList(WmsSnDO::getInboundOrderId, inboundOrderId);
    }

    default List<WmsSnDO> selectListByOutboundOrderId(Long outboundOrderId) {
        return selectList(WmsSnDO::getOutboundOrderId, outboundOrderId);
    }

}