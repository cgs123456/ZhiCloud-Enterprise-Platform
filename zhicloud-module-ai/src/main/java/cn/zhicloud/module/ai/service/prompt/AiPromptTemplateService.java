package cn.zhicloud.module.ai.service.prompt;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.ai.controller.admin.prompt.vo.AiPromptTemplatePageReqVO;
import cn.zhicloud.module.ai.controller.admin.prompt.vo.AiPromptTemplateSaveReqVO;
import cn.zhicloud.module.ai.dal.dataobject.prompt.AiPromptTemplateDO;

import java.util.List;
import java.util.Map;

/**
 * AI Prompt 模板 Service
 *
 * @author zhicloud
 */
public interface AiPromptTemplateService {

    /**
     * 创建 Prompt 模板
     *
     * @param createReqVO 创建信息
     * @return 模板编号
     */
    Long createPromptTemplate(AiPromptTemplateSaveReqVO createReqVO);

    /**
     * 更新 Prompt 模板
     *
     * @param updateReqVO 更新信息
     */
    void updatePromptTemplate(AiPromptTemplateSaveReqVO updateReqVO);

    /**
     * 删除 Prompt 模板
     *
     * @param id 编号
     */
    void deletePromptTemplate(Long id);

    /**
     * 获得 Prompt 模板
     *
     * @param id 编号
     * @return 模板
     */
    AiPromptTemplateDO getPromptTemplate(Long id);

    /**
     * 获得 Prompt 模板分页
     *
     * @param pageReqVO 分页查询
     * @return 分页结果
     */
    PageResult<AiPromptTemplateDO> getPromptTemplatePage(AiPromptTemplatePageReqVO pageReqVO);

    /**
     * 渲染模板
     *
     * <p>根据 code 查询模板，使用 Spring AI 的 PromptTemplate 替换变量占位符 {variableName}。
     *
     * @param code     模板编码
     * @param variables 变量键值对
     * @return 渲染后的 Prompt 字符串
     */
    String renderTemplate(String code, Map<String, Object> variables);

    /**
     * 按分类查询模板
     *
     * @param category 分类
     * @return 模板列表
     */
    List<AiPromptTemplateDO> getTemplatesByCategory(String category);

}
