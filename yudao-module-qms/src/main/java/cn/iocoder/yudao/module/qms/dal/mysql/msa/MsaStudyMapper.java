package cn.iocoder.yudao.module.qms.dal.mysql.msa;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.qms.controller.admin.msa.vo.MsaStudyPageReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.msa.MsaStudyDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * QMS MSA 研究 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface MsaStudyMapper extends BaseMapperX<MsaStudyDO> {

    default PageResult<MsaStudyDO> selectPage(MsaStudyPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MsaStudyDO>()
                .likeIfPresent(MsaStudyDO::getStudyNo, reqVO.getStudyNo())
                .eqIfPresent(MsaStudyDO::getStudyType, reqVO.getStudyType())
                .eqIfPresent(MsaStudyDO::getStatus, reqVO.getStatus())
                .orderByDesc(MsaStudyDO::getId));
    }

    default MsaStudyDO selectByStudyNo(String studyNo) {
        return selectOne(MsaStudyDO::getStudyNo, studyNo);
    }

}
