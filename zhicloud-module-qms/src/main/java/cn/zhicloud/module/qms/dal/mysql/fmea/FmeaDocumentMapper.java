package cn.zhicloud.module.qms.dal.mysql.fmea;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.qms.controller.admin.fmea.vo.FmeaDocumentPageReqVO;
import cn.zhicloud.module.qms.dal.dataobject.fmea.FmeaDocumentDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * QMS FMEA 文档 Mapper
 *
 * @author 智云
 */
@Mapper
public interface FmeaDocumentMapper extends BaseMapperX<FmeaDocumentDO> {

    default PageResult<FmeaDocumentDO> selectPage(FmeaDocumentPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<FmeaDocumentDO>()
                .likeIfPresent(FmeaDocumentDO::getFmeaNo, reqVO.getFmeaNo())
                .eqIfPresent(FmeaDocumentDO::getFmeaType, reqVO.getFmeaType())
                .eqIfPresent(FmeaDocumentDO::getStatus, reqVO.getStatus())
                .eqIfPresent(FmeaDocumentDO::getProductId, reqVO.getProductId())
                .orderByDesc(FmeaDocumentDO::getId));
    }

    default FmeaDocumentDO selectByFmeaNo(String fmeaNo) {
        return selectOne(FmeaDocumentDO::getFmeaNo, fmeaNo);
    }

}
