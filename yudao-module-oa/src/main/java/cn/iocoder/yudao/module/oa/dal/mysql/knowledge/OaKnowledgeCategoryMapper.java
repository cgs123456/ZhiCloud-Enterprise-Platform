package cn.iocoder.yudao.module.oa.dal.mysql.knowledge;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.oa.controller.admin.knowledge.vo.OaKnowledgeCategoryPageReqVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.knowledge.OaKnowledgeCategoryDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * OA 知识库分类 Mapper
 *
 * @author yudao
 */
@Mapper
public interface OaKnowledgeCategoryMapper extends BaseMapperX<OaKnowledgeCategoryDO> {

    default PageResult<OaKnowledgeCategoryDO> selectPage(OaKnowledgeCategoryPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OaKnowledgeCategoryDO>()
                .eqIfPresent(OaKnowledgeCategoryDO::getParentId, reqVO.getParentId())
                .likeIfPresent(OaKnowledgeCategoryDO::getName, reqVO.getName())
                .eqIfPresent(OaKnowledgeCategoryDO::getStatus, reqVO.getStatus())
                .orderByAsc(OaKnowledgeCategoryDO::getSort)
                .orderByDesc(OaKnowledgeCategoryDO::getId));
    }

    default OaKnowledgeCategoryDO selectByParentIdAndName(Long parentId, String name) {
        return selectOne(new LambdaQueryWrapperX<OaKnowledgeCategoryDO>()
                .eq(OaKnowledgeCategoryDO::getParentId, parentId)
                .eq(OaKnowledgeCategoryDO::getName, name));
    }

    default Long selectCountByParentId(Long parentId) {
        return selectCount(OaKnowledgeCategoryDO::getParentId, parentId);
    }

}
