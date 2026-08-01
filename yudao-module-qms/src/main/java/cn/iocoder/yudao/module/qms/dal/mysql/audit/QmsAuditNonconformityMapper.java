package cn.iocoder.yudao.module.qms.dal.mysql.audit;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.qms.controller.admin.audit.vo.QmsAuditNonconformityPageReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.audit.QmsAuditNonconformityDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * QMS 审核不符合项 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface QmsAuditNonconformityMapper extends BaseMapperX<QmsAuditNonconformityDO> {

    default PageResult<QmsAuditNonconformityDO> selectPage(QmsAuditNonconformityPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<QmsAuditNonconformityDO>()
                .eqIfPresent(QmsAuditNonconformityDO::getReportId, reqVO.getReportId())
                .likeIfPresent(QmsAuditNonconformityDO::getNcNo, reqVO.getNcNo())
                .eqIfPresent(QmsAuditNonconformityDO::getSeverity, reqVO.getSeverity())
                .eqIfPresent(QmsAuditNonconformityDO::getStatus, reqVO.getStatus())
                .eqIfPresent(QmsAuditNonconformityDO::getResponsibleDeptId, reqVO.getResponsibleDeptId())
                .orderByDesc(QmsAuditNonconformityDO::getId));
    }

    default QmsAuditNonconformityDO selectByNcNo(String ncNo) {
        return selectOne(QmsAuditNonconformityDO::getNcNo, ncNo);
    }

    default List<QmsAuditNonconformityDO> selectListByReportId(Long reportId) {
        return selectList(QmsAuditNonconformityDO::getReportId, reportId);
    }

    default Long selectCountByReportId(Long reportId) {
        return selectCount(QmsAuditNonconformityDO::getReportId, reportId);
    }

}
