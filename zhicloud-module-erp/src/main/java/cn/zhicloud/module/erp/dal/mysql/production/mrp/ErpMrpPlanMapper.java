package cn.zhicloud.module.erp.dal.mysql.production.mrp;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.controller.admin.production.mrp.vo.ErpMrpPlanPageReqVO;
import cn.zhicloud.module.erp.dal.dataobject.production.mrp.ErpMrpPlanDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 物料需求计划 Mapper
 *
 * @author 智云
 */
@Mapper
public interface ErpMrpPlanMapper extends BaseMapperX<ErpMrpPlanDO> {

    default PageResult<ErpMrpPlanDO> selectPage(ErpMrpPlanPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpMrpPlanDO>()
                .likeIfPresent(ErpMrpPlanDO::getNo, reqVO.getNo())
                .likeIfPresent(ErpMrpPlanDO::getPlanName, reqVO.getPlanName())
                .eqIfPresent(ErpMrpPlanDO::getMpsPlanId, reqVO.getMpsPlanId())
                .eqIfPresent(ErpMrpPlanDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(ErpMrpPlanDO::getPlanDate, reqVO.getPlanDate())
                .betweenIfPresent(ErpMrpPlanDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ErpMrpPlanDO::getId));
    }

    default ErpMrpPlanDO selectByNo(String no) {
        return selectOne(ErpMrpPlanDO::getNo, no);
    }

}
