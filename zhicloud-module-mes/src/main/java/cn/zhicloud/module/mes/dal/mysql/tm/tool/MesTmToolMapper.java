package cn.zhicloud.module.mes.dal.mysql.tm.tool;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.mes.controller.admin.tm.tool.vo.MesTmToolPageReqVO;
import cn.zhicloud.module.mes.dal.dataobject.tm.tool.MesTmToolDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MES 工具台账 Mapper
 *
 * @author 智云
 */
@Mapper
public interface MesTmToolMapper extends BaseMapperX<MesTmToolDO> {

    default PageResult<MesTmToolDO> selectPage(MesTmToolPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesTmToolDO>()
                .likeIfPresent(MesTmToolDO::getCode, reqVO.getCode())
                .likeIfPresent(MesTmToolDO::getName, reqVO.getName())
                .eqIfPresent(MesTmToolDO::getToolTypeId, reqVO.getToolTypeId())
                .likeIfPresent(MesTmToolDO::getBrand, reqVO.getBrand())
                .likeIfPresent(MesTmToolDO::getSpecification, reqVO.getSpecification())
                .eqIfPresent(MesTmToolDO::getStatus, reqVO.getStatus())
                .orderByDesc(MesTmToolDO::getId));
    }

    default MesTmToolDO selectByCode(String code) {
        return selectOne(MesTmToolDO::getCode, code);
    }

    default Long selectCountByToolTypeId(Long toolTypeId) {
        return selectCount(MesTmToolDO::getToolTypeId, toolTypeId);
    }

}
