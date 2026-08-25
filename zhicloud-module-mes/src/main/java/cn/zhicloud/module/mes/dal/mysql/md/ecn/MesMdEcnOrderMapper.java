package cn.zhicloud.module.mes.dal.mysql.md.ecn;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.mes.controller.admin.md.ecn.vo.MesMdEcnOrderPageReqVO;
import cn.zhicloud.module.mes.dal.dataobject.md.ecn.MesMdEcnOrderDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MES ECN 工程变更单 Mapper
 *
 * @author 智云
 */
@Mapper
public interface MesMdEcnOrderMapper extends BaseMapperX<MesMdEcnOrderDO> {

    default PageResult<MesMdEcnOrderDO> selectPage(MesMdEcnOrderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesMdEcnOrderDO>()
                .likeIfPresent(MesMdEcnOrderDO::getNo, reqVO.getNo())
                .likeIfPresent(MesMdEcnOrderDO::getEcnName, reqVO.getEcnName())
                .eqIfPresent(MesMdEcnOrderDO::getChangeType, reqVO.getChangeType())
                .eqIfPresent(MesMdEcnOrderDO::getBomId, reqVO.getBomId())
                .eqIfPresent(MesMdEcnOrderDO::getStatus, reqVO.getStatus())
                .eqIfPresent(MesMdEcnOrderDO::getApplicantUserId, reqVO.getApplicantUserId())
                .betweenIfPresent(MesMdEcnOrderDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MesMdEcnOrderDO::getId));
    }

    default MesMdEcnOrderDO selectByNo(String no) {
        return selectOne(MesMdEcnOrderDO::getNo, no);
    }

}
