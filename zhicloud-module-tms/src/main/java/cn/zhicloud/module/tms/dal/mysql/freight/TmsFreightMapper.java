package cn.zhicloud.module.tms.dal.mysql.freight;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.tms.controller.admin.freight.vo.TmsFreightPageReqVO;
import cn.zhicloud.module.tms.dal.dataobject.freight.TmsFreightDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * TMS 运费结算单 Mapper
 *
 * @author zhicloud
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
