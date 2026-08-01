package cn.iocoder.yudao.module.tms.dal.mysql.shipment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.tms.controller.admin.shipment.vo.TmsShipmentPageReqVO;
import cn.iocoder.yudao.module.tms.dal.dataobject.shipment.TmsShipmentDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * TMS 运单 Mapper
 *
 * @author yudao
 */
@Mapper
public interface TmsShipmentMapper extends BaseMapperX<TmsShipmentDO> {

    default PageResult<TmsShipmentDO> selectPage(TmsShipmentPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TmsShipmentDO>()
                .likeIfPresent(TmsShipmentDO::getNo, reqVO.getNo())
                .eqIfPresent(TmsShipmentDO::getCarrierId, reqVO.getCarrierId())
                .eqIfPresent(TmsShipmentDO::getShipmentType, reqVO.getShipmentType())
                .eqIfPresent(TmsShipmentDO::getStatus, reqVO.getStatus())
                .likeIfPresent(TmsShipmentDO::getSourceOrderNo, reqVO.getSourceOrderNo())
                .orderByDesc(TmsShipmentDO::getId));
    }

    default TmsShipmentDO selectByNo(String no) {
        return selectOne(TmsShipmentDO::getNo, no);
    }

}
