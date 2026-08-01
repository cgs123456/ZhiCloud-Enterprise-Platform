package cn.iocoder.yudao.module.ai.dal.mysql.prompt;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.ai.controller.admin.prompt.vo.AiPromptTemplatePageReqVO;
import cn.iocoder.yudao.module.ai.dal.dataobject.prompt.AiPromptTemplateDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * AI Prompt 模板 Mapper
 *
 * @author yudao
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
