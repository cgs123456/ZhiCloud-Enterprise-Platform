package cn.iocoder.yudao.module.qms.dal.mysql.ncr;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.qms.controller.admin.ncr.vo.NcrDocumentPageReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.ncr.NcrDocumentDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * QMS 不合格品报告 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface NcrDocumentMapper extends BaseMapperX<NcrDocumentDO> {

    default PageResult<NcrDocumentDO> selectPage(NcrDocumentPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<NcrDocumentDO>()
                .likeIfPresent(NcrDocumentDO::getNcrNo, reqVO.getNcrNo())
                .eqIfPresent(NcrDocumentDO::getSource, reqVO.getSource())
                .eqIfPresent(NcrDocumentDO::getDefectLevel, reqVO.getDefectLevel())
                .eqIfPresent(NcrDocumentDO::getDisposition, reqVO.getDisposition())
                .eqIfPresent(NcrDocumentDO::getStatus, reqVO.getStatus())
                .eqIfPresent(NcrDocumentDO::getProductId, reqVO.getProductId())
                .eqIfPresent(NcrDocumentDO::getSupplierId, reqVO.getSupplierId())
                .orderByDesc(NcrDocumentDO::getId));
    }

    default NcrDocumentDO selectByNcrNo(String ncrNo) {
        return selectOne(NcrDocumentDO::getNcrNo, ncrNo);
    }

}
