package cn.zhicloud.module.qms.dal.mysql.capa;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.qms.controller.admin.capa.vo.CAPADocumentPageReqVO;
import cn.zhicloud.module.qms.dal.dataobject.capa.CAPADocumentDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * QMS CAPA 文档 Mapper
 *
 * @author 智云
 */
@Mapper
public interface CAPADocumentMapper extends BaseMapperX<CAPADocumentDO> {

    default PageResult<CAPADocumentDO> selectPage(CAPADocumentPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CAPADocumentDO>()
                .likeIfPresent(CAPADocumentDO::getCapaNo, reqVO.getCapaNo())
                .eqIfPresent(CAPADocumentDO::getSource, reqVO.getSource())
                .eqIfPresent(CAPADocumentDO::getPriority, reqVO.getPriority())
                .eqIfPresent(CAPADocumentDO::getStage, reqVO.getStage())
                .eqIfPresent(CAPADocumentDO::getStatus, reqVO.getStatus())
                .eqIfPresent(CAPADocumentDO::getVerificationResult, reqVO.getVerificationResult())
                .likeIfPresent(CAPADocumentDO::getResponsiblePerson, reqVO.getResponsiblePerson())
                .betweenIfPresent(CAPADocumentDO::getDueDate, reqVO.getDueDate())
                .orderByDesc(CAPADocumentDO::getId));
    }

    default CAPADocumentDO selectByCapaNo(String capaNo) {
        return selectOne(CAPADocumentDO::getCapaNo, capaNo);
    }

}
