package cn.iocoder.yudao.module.qms.dal.mysql.document;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.qms.controller.admin.document.vo.QmsDocumentChangeRequestPageReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.document.QmsDocumentChangeRequestDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * QMS 文件变更申请 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface QmsDocumentChangeRequestMapper extends BaseMapperX<QmsDocumentChangeRequestDO> {

    default PageResult<QmsDocumentChangeRequestDO> selectPage(QmsDocumentChangeRequestPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<QmsDocumentChangeRequestDO>()
                .eqIfPresent(QmsDocumentChangeRequestDO::getDocumentId, reqVO.getDocumentId())
                .eqIfPresent(QmsDocumentChangeRequestDO::getChangeType, reqVO.getChangeType())
                .eqIfPresent(QmsDocumentChangeRequestDO::getStatus, reqVO.getStatus())
                .eqIfPresent(QmsDocumentChangeRequestDO::getApplicantId, reqVO.getApplicantId())
                .betweenIfPresent(QmsDocumentChangeRequestDO::getApplyDate, reqVO.getApplyDate())
                .orderByDesc(QmsDocumentChangeRequestDO::getId));
    }

    default List<QmsDocumentChangeRequestDO> selectListByDocumentId(Long documentId) {
        return selectList(QmsDocumentChangeRequestDO::getDocumentId, documentId);
    }

}
