package cn.zhicloud.module.qms.dal.mysql.document;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.qms.controller.admin.document.vo.QmsDocumentPageReqVO;
import cn.zhicloud.module.qms.dal.dataobject.document.QmsDocumentDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * QMS 受控文档 Mapper
 *
 * @author 智云
 */
@Mapper
public interface QmsDocumentMapper extends BaseMapperX<QmsDocumentDO> {

    default PageResult<QmsDocumentDO> selectPage(QmsDocumentPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<QmsDocumentDO>()
                .likeIfPresent(QmsDocumentDO::getDocNo, reqVO.getDocNo())
                .likeIfPresent(QmsDocumentDO::getTitle, reqVO.getTitle())
                .eqIfPresent(QmsDocumentDO::getDocType, reqVO.getDocType())
                .eqIfPresent(QmsDocumentDO::getStatus, reqVO.getStatus())
                .eqIfPresent(QmsDocumentDO::getOwnerDeptId, reqVO.getOwnerDeptId())
                .betweenIfPresent(QmsDocumentDO::getEffectiveDate, reqVO.getEffectiveDate())
                .orderByAsc(QmsDocumentDO::getSort)
                .orderByDesc(QmsDocumentDO::getId));
    }

    default QmsDocumentDO selectByDocNo(String docNo) {
        return selectOne(QmsDocumentDO::getDocNo, docNo);
    }

}
