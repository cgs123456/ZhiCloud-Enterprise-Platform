package cn.zhicloud.module.qms.dal.mysql.document;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.qms.controller.admin.document.vo.QmsDocumentDistributePageReqVO;
import cn.zhicloud.module.qms.dal.dataobject.document.QmsDocumentDistributeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * QMS 文档分发记录 Mapper
 *
 * @author 智云
 */
@Mapper
public interface QmsDocumentDistributeMapper extends BaseMapperX<QmsDocumentDistributeDO> {

    default PageResult<QmsDocumentDistributeDO> selectPage(QmsDocumentDistributePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<QmsDocumentDistributeDO>()
                .eqIfPresent(QmsDocumentDistributeDO::getDocumentId, reqVO.getDocumentId())
                .likeIfPresent(QmsDocumentDistributeDO::getDistributeTo, reqVO.getDistributeTo())
                .likeIfPresent(QmsDocumentDistributeDO::getReceivedBy, reqVO.getReceivedBy())
                .betweenIfPresent(QmsDocumentDistributeDO::getDistributeDate, reqVO.getDistributeDate())
                .orderByDesc(QmsDocumentDistributeDO::getId));
    }

    default List<QmsDocumentDistributeDO> selectListByDocumentId(Long documentId) {
        return selectList(QmsDocumentDistributeDO::getDocumentId, documentId);
    }

}
