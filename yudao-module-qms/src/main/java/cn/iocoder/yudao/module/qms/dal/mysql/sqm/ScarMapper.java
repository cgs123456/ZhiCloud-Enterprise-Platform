package cn.iocoder.yudao.module.qms.dal.mysql.sqm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.qms.controller.admin.sqm.vo.ScarPageReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.sqm.ScarDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * QMS SCAR Mapper
 *
 * @author yudao
 */
@Mapper
public interface ScarMapper extends BaseMapperX<ScarDO> {

    default PageResult<ScarDO> selectPage(ScarPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ScarDO>()
                .likeIfPresent(ScarDO::getScarNo, reqVO.getScarNo())
                .eqIfPresent(ScarDO::getSupplierId, reqVO.getSupplierId())
                .eqIfPresent(ScarDO::getStatus, reqVO.getStatus())
                .orderByDesc(ScarDO::getId));
    }

    default ScarDO selectByScarNo(String scarNo) {
        return selectOne(ScarDO::getScarNo, scarNo);
    }

}