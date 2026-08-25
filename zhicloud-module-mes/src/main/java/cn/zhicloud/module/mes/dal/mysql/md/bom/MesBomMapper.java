package cn.zhicloud.module.mes.dal.mysql.md.bom;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.mes.controller.admin.md.bom.vo.MesBomPageReqVO;
import cn.zhicloud.module.mes.dal.dataobject.md.bom.MesBomDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MES 独立 BOM Mapper
 *
 * @author 智云
 */
@Mapper
public interface MesBomMapper extends BaseMapperX<MesBomDO> {

    default PageResult<MesBomDO> selectPage(MesBomPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesBomDO>()
                .likeIfPresent(MesBomDO::getBomNo, reqVO.getBomNo())
                .eqIfPresent(MesBomDO::getProductId, reqVO.getProductId())
                .eqIfPresent(MesBomDO::getBomType, reqVO.getBomType())
                .likeIfPresent(MesBomDO::getVersion, reqVO.getVersion())
                .eqIfPresent(MesBomDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(MesBomDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MesBomDO::getId));
    }

    default MesBomDO selectByBomNo(String bomNo) {
        return selectOne(MesBomDO::getBomNo, bomNo);
    }

    /**
     * 查询产品下生效（启用）的 BOM，按版本号降序取最新一条
     */
    default MesBomDO selectActiveByProductId(Long productId) {
        return selectOne(new LambdaQueryWrapperX<MesBomDO>()
                .eq(MesBomDO::getProductId, productId)
                .eq(MesBomDO::getStatus, cn.zhicloud.framework.common.enums.CommonStatusEnum.ENABLE.getStatus())
                .orderByDesc(MesBomDO::getVersion)
                .last("LIMIT 1"));
    }

    default Long selectCountByProductId(Long productId) {
        return selectCount(MesBomDO::getProductId, productId);
    }

}