package cn.zhicloud.module.tms.dal.mysql.carrier;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.tms.controller.admin.carrier.vo.TmsCarrierPageReqVO;
import cn.zhicloud.module.tms.dal.dataobject.carrier.TmsCarrierDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * TMS 承运商 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface TmsCarrierMapper extends BaseMapperX<TmsCarrierDO> {

    default PageResult<TmsCarrierDO> selectPage(TmsCarrierPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TmsCarrierDO>()
                .likeIfPresent(TmsCarrierDO::getName, reqVO.getName())
                .eqIfPresent(TmsCarrierDO::getCode, reqVO.getCode())
                .eqIfPresent(TmsCarrierDO::getServiceType, reqVO.getServiceType())
                .eqIfPresent(TmsCarrierDO::getStatus, reqVO.getStatus())
                .orderByDesc(TmsCarrierDO::getId));
    }

    default TmsCarrierDO selectByCode(String code) {
        return selectOne(TmsCarrierDO::getCode, code);
    }

}
