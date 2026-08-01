package cn.iocoder.yudao.module.erp.dal.mysql.collaboration.cpfr;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.collaboration.cpfr.vo.ErpCpfrForecastPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.collaboration.cpfr.ErpCpfrForecastDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP CPFR 预测 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpCpfrForecastMapper extends BaseMapperX<ErpCpfrForecastDO> {

    default PageResult<ErpCpfrForecastDO> selectPage(ErpCpfrForecastPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpCpfrForecastDO>()
                .likeIfPresent(ErpCpfrForecastDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpCpfrForecastDO::getPartnerType, reqVO.getPartnerType())
                .eqIfPresent(ErpCpfrForecastDO::getPartnerId, reqVO.getPartnerId())
                .eqIfPresent(ErpCpfrForecastDO::getProductId, reqVO.getProductId())
                .likeIfPresent(ErpCpfrForecastDO::getProductName, reqVO.getProductName())
                .eqIfPresent(ErpCpfrForecastDO::getForecastPeriod, reqVO.getForecastPeriod())
                .orderByDesc(ErpCpfrForecastDO::getId));
    }

    default ErpCpfrForecastDO selectByNo(String no) {
        return selectOne(ErpCpfrForecastDO::getNo, no);
    }

}
