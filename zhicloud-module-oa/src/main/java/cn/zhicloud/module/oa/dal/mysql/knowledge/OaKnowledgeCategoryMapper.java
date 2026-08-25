package cn.zhicloud.module.oa.dal.mysql.knowledge;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.oa.controller.admin.knowledge.vo.OaKnowledgeCategoryPageReqVO;
import cn.zhicloud.module.oa.dal.dataobject.knowledge.OaKnowledgeCategoryDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * OA 知识库分类 Mapper
 *
 * @author zhicloud
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
