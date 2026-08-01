package cn.iocoder.yudao.module.erp.dal.mysql.collaboration.cpfr;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.collaboration.cpfr.vo.ErpCpfrExceptionPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.collaboration.cpfr.ErpCpfrExceptionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP CPFR 异常 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpCpfrExceptionMapper extends BaseMapperX<ErpCpfrExceptionDO> {

    default PageResult<ErpCpfrExceptionDO> selectPage(ErpCpfrExceptionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpCpfrExceptionDO>()
                .eqIfPresent(ErpCpfrExceptionDO::getForecastId, reqVO.getForecastId())
                .eqIfPresent(ErpCpfrExceptionDO::getExceptionType, reqVO.getExceptionType())
                .eqIfPresent(ErpCpfrExceptionDO::getHandlingStatus, reqVO.getHandlingStatus())
                .orderByDesc(ErpCpfrExceptionDO::getId));
    }

    default List<ErpCpfrExceptionDO> selectListByForecastId(Long forecastId) {
        return selectList(new LambdaQueryWrapperX<ErpCpfrExceptionDO>()
                .eq(ErpCpfrExceptionDO::getForecastId, forecastId));
    }

}
