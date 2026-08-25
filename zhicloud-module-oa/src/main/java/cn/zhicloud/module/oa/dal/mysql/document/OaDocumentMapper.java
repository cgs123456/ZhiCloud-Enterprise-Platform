package cn.zhicloud.module.oa.dal.mysql.document;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.oa.controller.admin.document.vo.OaDocumentPageReqVO;
import cn.zhicloud.module.oa.dal.dataobject.document.OaDocumentDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * OA 公文 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface OaDocumentMapper extends BaseMapperX<OaDocumentDO> {

    default PageResult<OaDocumentDO> selectPage(OaDocumentPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OaDocumentDO>()
                .eqIfPresent(OaDocumentDO::getNo, reqVO.getNo())
                .likeIfPresent(OaDocumentDO::getTitle, reqVO.getTitle())
                .eqIfPresent(OaDocumentDO::getDocumentType, reqVO.getDocumentType())
                .eqIfPresent(OaDocumentDO::getUrgency, reqVO.getUrgency())
                .eqIfPresent(OaDocumentDO::getConfidentiality, reqVO.getConfidentiality())
                .eqIfPresent(OaDocumentDO::getStatus, reqVO.getStatus())
                .eqIfPresent(OaDocumentDO::getIssuerUserId, reqVO.getIssuerUserId())
                .eqIfPresent(OaDocumentDO::getReviewerUserId, reqVO.getReviewerUserId())
                .eqIfPresent(OaDocumentDO::getSignerUserId, reqVO.getSignerUserId())
                .eqIfPresent(OaDocumentDO::getArchiverUserId, reqVO.getArchiverUserId())
                .likeIfPresent(OaDocumentDO::getArchiveNo, reqVO.getArchiveNo())
                .betweenIfPresent(OaDocumentDO::getIssueDate, reqVO.getIssueDate())
                .orderByDesc(OaDocumentDO::getId));
    }

    default OaDocumentDO selectByNo(String no) {
        return selectOne(OaDocumentDO::getNo, no);
    }

}
