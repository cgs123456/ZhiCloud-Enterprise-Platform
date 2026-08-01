package cn.iocoder.yudao.module.tms.dal.mysql.freight;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.tms.controller.admin.freight.vo.TmsFreightPageReqVO;
import cn.iocoder.yudao.module.tms.dal.dataobject.freight.TmsFreightDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * TMS 运费结算单 Mapper
 *
 * @author yudao
 */
@Mapper
public interface TmsFreightMapper extends BaseMapperX<TmsFreightDO> {

    default PageResult<TmsFreightDO> selectPage(TmsFreightPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TmsFreightDO>()
                .likeIfPresent(TmsFreightDO::getNo, reqVO.getNo())
                .eqIfPresent(TmsFreightDO::getShipmentId, reqVO.getShipmentId())
                .eqIfPresent(TmsFreightDO::getCarrierId, reqVO.getCarrierId())
                .eqIfPresent(TmsFreightDO::getBillingMethod, reqVO.getBillingMethod())
                .eqIfPresent(TmsFreightDO::getStatus, reqVO.getStatus())
                .orderByDesc(TmsFreightDO::getId));
    }

    default TmsFreightDO selectByNo(String no) {
        return selectOne(TmsFreightDO::getNo, no);
    }

}
