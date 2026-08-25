package cn.zhicloud.module.qms.dal.mysql.audit;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.qms.controller.admin.audit.vo.QmsAuditReportPageReqVO;
import cn.zhicloud.module.qms.dal.dataobject.audit.QmsAuditReportDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * QMS 审核报告 Mapper
 *
 * @author 智云
 */
@Mapper
public interface QmsAuditReportMapper extends BaseMapperX<QmsAuditReportDO> {

    default PageResult<QmsAuditReportDO> selectPage(QmsAuditReportPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<QmsAuditReportDO>()
                .eqIfPresent(QmsAuditReportDO::getPlanId, reqVO.getPlanId())
                .likeIfPresent(QmsAuditReportDO::getReportNo, reqVO.getReportNo())
                .eqIfPresent(QmsAuditReportDO::getConclusion, reqVO.getConclusion())
                .orderByDesc(QmsAuditReportDO::getId));
    }

    default QmsAuditReportDO selectByReportNo(String reportNo) {
        return selectOne(QmsAuditReportDO::getReportNo, reportNo);
    }

}
