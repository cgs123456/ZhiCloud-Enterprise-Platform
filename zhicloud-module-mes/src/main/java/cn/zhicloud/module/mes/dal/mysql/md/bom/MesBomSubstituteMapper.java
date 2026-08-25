package cn.zhicloud.module.mes.dal.mysql.md.bom;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.mes.controller.admin.md.bom.substitute.vo.MesBomSubstitutePageReqVO;
import cn.zhicloud.module.mes.dal.dataobject.md.bom.MesBomSubstituteDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES BOM 替代料 Mapper
 *
 * @author 智云
 */
@Mapper
public interface MesBomSubstituteMapper extends BaseMapperX<MesBomSubstituteDO> {

    default PageResult<MesBomSubstituteDO> selectPage(MesBomSubstitutePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesBomSubstituteDO>()
                .eqIfPresent(MesBomSubstituteDO::getBomId, reqVO.getBomId())
                .eqIfPresent(MesBomSubstituteDO::getBomDetailId, reqVO.getBomDetailId())
                .eqIfPresent(MesBomSubstituteDO::getSubstituteItemId, reqVO.getSubstituteItemId())
                .eqIfPresent(MesBomSubstituteDO::getStatus, reqVO.getStatus())
                .orderByAsc(MesBomSubstituteDO::getPriority)
                .orderByAsc(MesBomSubstituteDO::getId));
    }

    default List<MesBomSubstituteDO> selectListByBomId(Long bomId) {
        return selectList(MesBomSubstituteDO::getBomId, bomId);
    }

    /**
     * 按 BOM 明细 ID 查询替代料，按 priority 升序返回（首选在前）
     */
    default List<MesBomSubstituteDO> selectListByBomDetailId(Long bomDetailId) {
        return selectList(new LambdaQueryWrapperX<MesBomSubstituteDO>()
                .eq(MesBomSubstituteDO::getBomDetailId, bomDetailId)
                .orderByAsc(MesBomSubstituteDO::getPriority)
                .orderByAsc(MesBomSubstituteDO::getId));
    }

    default void deleteByBomId(Long bomId) {
        delete(MesBomSubstituteDO::getBomId, bomId);
    }

    default void deleteByBomDetailId(Long bomDetailId) {
        delete(MesBomSubstituteDO::getBomDetailId, bomDetailId);
    }

}