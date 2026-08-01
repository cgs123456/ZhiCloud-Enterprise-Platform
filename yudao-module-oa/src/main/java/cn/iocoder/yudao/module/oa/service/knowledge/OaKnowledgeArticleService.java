package cn.iocoder.yudao.module.oa.service.knowledge;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.controller.admin.knowledge.vo.OaKnowledgeArticlePageReqVO;
import cn.iocoder.yudao.module.oa.controller.admin.knowledge.vo.OaKnowledgeArticleSaveReqVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.knowledge.OaKnowledgeArticleDO;
import jakarta.validation.Valid;

/**
 * OA 知识库文章 Service 接口
 *
 * @author yudao
 */
public interface OaKnowledgeArticleService {

    /**
     * 创建知识库文章
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createArticle(@Valid OaKnowledgeArticleSaveReqVO createReqVO);

    /**
     * 更新知识库文章
     *
     * @param updateReqVO 更新信息
     */
    void updateArticle(@Valid OaKnowledgeArticleSaveReqVO updateReqVO);

    /**
     * 删除知识库文章
     *
     * @param id 编号
     */
    void deleteArticle(Long id);

    /**
     * 获得知识库文章
     *
     * @param id 编号
     * @return 知识库文章
     */
    OaKnowledgeArticleDO getArticle(Long id);

    /**
     * 获得知识库文章分页
     *
     * @param pageReqVO 分页查询
     * @return 知识库文章分页
     */
    PageResult<OaKnowledgeArticleDO> getArticlePage(OaKnowledgeArticlePageReqVO pageReqVO);

    /**
     * 发布知识库文章：草稿 -> 已发布
     *
     * @param id 编号
     */
    void publishArticle(Long id);

    /**
     * 全文检索知识库文章
     *
     * @param pageParam 分页参数
     * @param keyword   关键词
     * @return 文章分页
     */
    PageResult<OaKnowledgeArticleDO> searchArticles(PageParam pageParam, String keyword);

}
