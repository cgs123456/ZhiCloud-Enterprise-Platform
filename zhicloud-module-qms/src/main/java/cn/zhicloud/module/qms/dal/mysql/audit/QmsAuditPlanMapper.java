package cn.zhicloud.module.qms.dal.mysql.audit;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.qms.controller.admin.audit.vo.QmsAuditPlanPageReqVO;
import cn.zhicloud.module.qms.dal.dataobject.audit.QmsAuditPlanDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * QMS 审核计划 Mapper
 *
 * @author 智云
 */
@Mapper
public interface QmsAuditPlanMapper extends BaseMapperX<QmsAuditPlanDO> {

    default PageResult<QmsAuditPlanDO> selectPage(QmsAuditPlanPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<QmsAuditPlanDO>()
                .likeIfPresent(QmsAuditPlanDO::getPlanNo, reqVO.getPlanNo())
                .likeIfPresent(QmsAuditPlanDO::getTitle, reqVO.getTitle())
                .eqIfPresent(QmsAuditPlanDO::getAuditType, reqVO.getAuditType())
                .eqIfPresent(QmsAuditPlanDO::getStatus, reqVO.getStatus())
                .eqIfPresent(QmsAuditPlanDO::getLeadAuditorId, reqVO.getLeadAuditorId())
                .betweenIfPresent(QmsAuditPlanDO::getAuditStartDate, reqVO.getAuditStartDate())
                .orderByDesc(QmsAuditPlanDO::getId));
    }

    default QmsAuditPlanDO selectByPlanNo(String planNo) {
        return selectOne(QmsAuditPlanDO::getPlanNo, planNo);
    }

}
