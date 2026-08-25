package cn.zhicloud.module.oa.dal.mysql.document;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.oa.dal.dataobject.document.OaDocumentAttachmentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * OA 公文附件 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface OaDocumentAttachmentMapper extends BaseMapperX<OaDocumentAttachmentDO> {

    default List<OaDocumentAttachmentDO> selectListByDocumentId(Long documentId) {
        return selectList(OaDocumentAttachmentDO::getDocumentId, documentId);
    }

    default int deleteByDocumentId(Long documentId) {
        return delete(new LambdaQueryWrapperX<OaDocumentAttachmentDO>()
                .eq(OaDocumentAttachmentDO::getDocumentId, documentId));
    }

}
