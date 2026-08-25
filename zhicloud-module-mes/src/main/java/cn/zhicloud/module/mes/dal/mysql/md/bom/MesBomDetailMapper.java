package cn.zhicloud.module.mes.dal.mysql.md.bom;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.mes.controller.admin.md.bom.vo.MesBomDetailPageReqVO;
import cn.zhicloud.module.mes.dal.dataobject.md.bom.MesBomDetailDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * MES BOM 明细 Mapper
 *
 * @author 智云
 */
@Mapper
public interface MesBomDetailMapper extends BaseMapperX<MesBomDetailDO> {

    default PageResult<MesBomDetailDO> selectPage(MesBomDetailPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesBomDetailDO>()
                .eqIfPresent(MesBomDetailDO::getBomId, reqVO.getBomId())
                .eqIfPresent(MesBomDetailDO::getProductId, reqVO.getProductId())
                .orderByAsc(MesBomDetailDO::getId));
    }

    default List<MesBomDetailDO> selectListByBomId(Long bomId) {
        return selectList(MesBomDetailDO::getBomId, bomId);
    }

    default List<MesBomDetailDO> selectListByBomIds(Collection<Long> bomIds) {
        return selectList(MesBomDetailDO::getBomId, bomIds);
    }

    default void deleteByBomId(Long bomId) {
        delete(MesBomDetailDO::getBomId, bomId);
    }

}