package cn.iocoder.yudao.module.ai.service.prompt;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.ai.controller.admin.prompt.vo.AiPromptTemplatePageReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.prompt.vo.AiPromptTemplateSaveReqVO;
import cn.iocoder.yudao.module.ai.dal.dataobject.prompt.AiPromptTemplateDO;
import cn.iocoder.yudao.module.ai.dal.mysql.prompt.AiPromptTemplateMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.AI_PROMPT_TEMPLATE_CODE_EXISTS;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.AI_PROMPT_TEMPLATE_NOT_EXISTS;

/**
 * AI Prompt 模板 Service 实现类
 *
 * @author yudao
 */
@Service
@Slf4j
public class AiPromptTemplateServiceImpl implements AiPromptTemplateService {

    @Resource
    private AiPromptTemplateMapper promptTemplateMapper;

    @Override
    public Long createPromptTemplate(AiPromptTemplateSaveReqVO createReqVO) {
        // 1. 校验 code 唯一
        validateCodeUnique(null, createReqVO.getCode());
        // 2. 插入
        AiPromptTemplateDO promptTemplate = BeanUtils.toBean(createReqVO, AiPromptTemplateDO.class);
        promptTemplateMapper.insert(promptTemplate);
        return promptTemplate.getId();
    }

    @Override
    public void updatePromptTemplate(AiPromptTemplateSaveReqVO updateReqVO) {
        // 1.1 校验存在
        validatePromptTemplateExists(updateReqVO.getId());
        // 1.2 校验 code 唯一
        validateCodeUnique(updateReqVO.getId(), updateReqVO.getCode());
        // 2. 更新
        AiPromptTemplateDO updateObj = BeanUtils.toBean(updateReqVO, AiPromptTemplateDO.class);
        promptTemplateMapper.updateById(updateObj);
    }

    @Override
    public void deletePromptTemplate(Long id) {
        // 1. 校验存在
        validatePromptTemplateExists(id);
        // 2. 删除
        promptTemplateMapper.deleteById(id);
    }

    @Override
    public AiPromptTemplateDO getPromptTemplate(Long id) {
        return promptTemplateMapper.selectById(id);
    }

    @Override
    public PageResult<AiPromptTemplateDO> getPromptTemplatePage(AiPromptTemplatePageReqVO pageReqVO) {
        return promptTemplateMapper.selectPage(pageReqVO);
    }

    @Override
    public String renderTemplate(String code, Map<String, Object> variables) {
        // 1. 根据 code 查询模板
        AiPromptTemplateDO template = promptTemplateMapper.selectByCode(code);
        if (template == null) {
            throw exception(AI_PROMPT_TEMPLATE_NOT_EXISTS);
        }
        String content = template.getContent();
        if (StrUtil.isEmpty(content)) {
            return content;
        }
        if (variables == null || variables.isEmpty()) {
            return content;
        }

        // 2. 使用 Spring AI 的 PromptTemplate 替换变量占位符 {variableName}
        try {
            PromptTemplate promptTemplate = new PromptTemplate(content);
            return promptTemplate.render(variables);
        } catch (Exception ex) {
            // Spring AI 的 PromptTemplate 基于 StringTemplate，对未声明变量或语法较敏感
            // 降级为简单的字符串替换，保证渲染可用
            log.warn("[renderTemplate][Spring AI PromptTemplate 渲染失败，降级为字符串替换 code={}, err={}]",
                    code, ex.toString());
            return renderByStringReplace(content, variables);
        }
    }

    @Override
    public List<AiPromptTemplateDO> getTemplatesByCategory(String category) {
        return promptTemplateMapper.selectListByCategory(category);
    }

    /**
     * 校验模板存在
     */
    private AiPromptTemplateDO validatePromptTemplateExists(Long id) {
        AiPromptTemplateDO template = promptTemplateMapper.selectById(id);
        if (template == null) {
            throw exception(AI_PROMPT_TEMPLATE_NOT_EXISTS);
        }
        return template;
    }

    /**
     * 校验 code 唯一
     */
    private void validateCodeUnique(Long id, String code) {
        AiPromptTemplateDO template = promptTemplateMapper.selectByCode(code);
        if (template == null) {
            return;
        }
        if (id == null || !template.getId().equals(id)) {
            throw exception(AI_PROMPT_TEMPLATE_CODE_EXISTS);
        }
    }

    /**
     * 简单的字符串变量替换（fallback）
     *
     * <p>将 {variableName} 占位符替换为变量值。
     */
    private String renderByStringReplace(String content, Map<String, Object> variables) {
        String result = content;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue() == null ? "" : entry.getValue().toString();
            result = result.replace(placeholder, value);
        }
        return result;
    }

}
