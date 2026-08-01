package cn.iocoder.yudao.module.qms.dal.mysql.audit;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.qms.controller.admin.audit.vo.QmsAuditReportPageReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.audit.QmsAuditReportDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * QMS 审核报告 Mapper
 *
 * @author 芋道源码
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
