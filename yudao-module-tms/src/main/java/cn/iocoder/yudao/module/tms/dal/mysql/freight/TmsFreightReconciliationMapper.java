package cn.iocoder.yudao.module.tms.dal.mysql.freight;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.tms.controller.admin.freight.vo.TmsFreightReconciliationPageReqVO;
import cn.iocoder.yudao.module.tms.dal.dataobject.freight.TmsFreightReconciliationDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * TMS 运费对账 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface TmsFreightReconciliationMapper extends BaseMapperX<TmsFreightReconciliationDO> {

    default PageResult<TmsFreightReconciliationDO> selectPage(TmsFreightReconciliationPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TmsFreightReconciliationDO>()
                .eqIfPresent(TmsFreightReconciliationDO::getNo, reqVO.getNo())
                .eqIfPresent(TmsFreightReconciliationDO::getCarrierId, reqVO.getCarrierId())
                .eqIfPresent(TmsFreightReconciliationDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(TmsFreightReconciliationDO::getPeriodStart, reqVO.getPeriodStart())
                .orderByDesc(TmsFreightReconciliationDO::getCreateTime));
    }

    default TmsFreightReconciliationDO selectByNo(String no) {
        return selectOne(new LambdaQueryWrapperX<TmsFreightReconciliationDO>()
                .eq(TmsFreightReconciliationDO::getNo, no));
    }

}
