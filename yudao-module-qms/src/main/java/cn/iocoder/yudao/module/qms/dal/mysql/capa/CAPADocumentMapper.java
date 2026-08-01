package cn.iocoder.yudao.module.qms.dal.mysql.capa;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.qms.controller.admin.capa.vo.CAPADocumentPageReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.capa.CAPADocumentDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * QMS CAPA 文档 Mapper
 *
 * @author 芋道源码
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
