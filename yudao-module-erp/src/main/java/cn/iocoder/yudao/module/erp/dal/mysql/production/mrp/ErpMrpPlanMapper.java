package cn.iocoder.yudao.module.erp.dal.mysql.production.mrp;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.production.mrp.vo.ErpMrpPlanPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.mrp.ErpMrpPlanDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 物料需求计划 Mapper
 *
 * @author 芋道源码
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
