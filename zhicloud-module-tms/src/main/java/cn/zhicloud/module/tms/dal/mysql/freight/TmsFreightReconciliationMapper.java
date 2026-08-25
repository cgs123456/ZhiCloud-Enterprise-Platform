package cn.zhicloud.module.tms.dal.mysql.freight;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.tms.controller.admin.freight.vo.TmsFreightReconciliationPageReqVO;
import cn.zhicloud.module.tms.dal.dataobject.freight.TmsFreightReconciliationDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * TMS 运费对账 Mapper
 *
 * @author 智云
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
