package cn.zhicloud.module.erp.dal.mysql.production.mps;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.controller.admin.production.mps.vo.ErpMpsPlanPageReqVO;
import cn.zhicloud.module.erp.dal.dataobject.production.mps.ErpMpsPlanDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 主生产计划 Mapper
 *
 * @author 智云
 */
@Mapper
public interface ErpMpsPlanMapper extends BaseMapperX<ErpMpsPlanDO> {

    default PageResult<ErpMpsPlanDO> selectPage(ErpMpsPlanPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpMpsPlanDO>()
                .likeIfPresent(ErpMpsPlanDO::getPlanNo, reqVO.getPlanNo())
                .eqIfPresent(ErpMpsPlanDO::getProductId, reqVO.getProductId())
                .eqIfPresent(ErpMpsPlanDO::getPlanPeriod, reqVO.getPlanPeriod())
                .eqIfPresent(ErpMpsPlanDO::getPlanType, reqVO.getPlanType())
                .eqIfPresent(ErpMpsPlanDO::getSource, reqVO.getSource())
                .eqIfPresent(ErpMpsPlanDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(ErpMpsPlanDO::getDemandDate, reqVO.getDemandDate())
                .betweenIfPresent(ErpMpsPlanDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ErpMpsPlanDO::getId));
    }

    default ErpMpsPlanDO selectByPlanNo(String planNo) {
        return selectOne(ErpMpsPlanDO::getPlanNo, planNo);
    }

    default ErpMpsPlanDO selectByProductAndPeriod(Long productId, String planPeriod) {
        return selectOne(ErpMpsPlanDO::getProductId, productId,
                ErpMpsPlanDO::getPlanPeriod, planPeriod);
    }

}