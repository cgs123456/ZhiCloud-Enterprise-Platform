package cn.zhicloud.module.ai.dal.mysql.prompt;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.ai.controller.admin.prompt.vo.AiPromptTemplatePageReqVO;
import cn.zhicloud.module.ai.dal.dataobject.prompt.AiPromptTemplateDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * AI Prompt 模板 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface AiPromptTemplateMapper extends BaseMapperX<AiPromptTemplateDO> {

    default PageResult<AiPromptTemplateDO> selectPage(AiPromptTemplatePageReqVO pageReqVO) {
        return selectPage(pageReqVO, new LambdaQueryWrapperX<AiPromptTemplateDO>()
                .likeIfPresent(AiPromptTemplateDO::getName, pageReqVO.getName())
                .eqIfPresent(AiPromptTemplateDO::getCode, pageReqVO.getCode())
                .eqIfPresent(AiPromptTemplateDO::getCategory, pageReqVO.getCategory())
                .eqIfPresent(AiPromptTemplateDO::getStatus, pageReqVO.getStatus())
                .betweenIfPresent(AiPromptTemplateDO::getCreateTime, pageReqVO.getCreateTime())
                .orderByDesc(AiPromptTemplateDO::getId));
    }

    default AiPromptTemplateDO selectByCode(String code) {
        return selectOne(AiPromptTemplateDO::getCode, code);
    }

    default List<AiPromptTemplateDO> selectListByCategory(String category) {
        return selectList(AiPromptTemplateDO::getCategory, category);
    }

}
