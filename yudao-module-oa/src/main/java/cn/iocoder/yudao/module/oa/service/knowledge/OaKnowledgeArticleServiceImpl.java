package cn.iocoder.yudao.module.oa.service.knowledge;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.oa.controller.admin.knowledge.vo.OaKnowledgeArticlePageReqVO;
import cn.iocoder.yudao.module.oa.controller.admin.knowledge.vo.OaKnowledgeArticleSaveReqVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.knowledge.OaKnowledgeArticleDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.knowledge.OaKnowledgeVersionDO;
import cn.iocoder.yudao.module.oa.dal.mysql.knowledge.OaKnowledgeArticleMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.knowledge.OaKnowledgeCommentMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.knowledge.OaKnowledgeVersionMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.oa.enums.ErrorCodeConstants.OA_KNOWLEDGE_ARTICLE_NOT_EXISTS;
import static cn.iocoder.yudao.module.oa.enums.ErrorCodeConstants.OA_KNOWLEDGE_ARTICLE_STATUS_INVALID;

/**
 * OA 知识库文章 Service 实现类
 *
 * @author yudao
 */
@Service
@Validated
public class OaKnowledgeArticleServiceImpl implements OaKnowledgeArticleService {

    /**
     * 草稿状态
     */
    private static final int STATUS_DRAFT = 10;
    /**
     * 已发布状态
     */
    private static final int STATUS_PUBLISHED = 20;
    /**
     * 初始版本号
     */
    private static final int INITIAL_VERSION = 1;
    /**
     * 初始计数值
     */
    private static final int INITIAL_COUNT = 0;

    @Resource
    private OaKnowledgeArticleMapper articleMapper;
    @Resource
    private OaKnowledgeVersionMapper versionMapper;
    @Resource
    private OaKnowledgeCommentMapper commentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createArticle(OaKnowledgeArticleSaveReqVO createReqVO) {
        // 插入文章（默认草稿状态、初始版本号 1）
        OaKnowledgeArticleDO article = BeanUtils.toBean(createReqVO, OaKnowledgeArticleDO.class);
        if (article.getAuthorUserId() == null) {
            article.setAuthorUserId(SecurityFrameworkUtils.getLoginUserId());
        }
        if (article.getStatus() == null) {
            article.setStatus(STATUS_DRAFT);
        }
        if (article.getCurrentVersion() == null) {
            article.setCurrentVersion(INITIAL_VERSION);
        }
        article.setViewCount(INITIAL_COUNT);
        article.setLikeCount(INITIAL_COUNT);
        article.setCommentCount(INITIAL_COUNT);
        articleMapper.insert(article);
        // 自动记录版本
        saveVersion(article, createReqVO.getChangeLog());
        return article.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateArticle(OaKnowledgeArticleSaveReqVO updateReqVO) {
        // 校验存在
        OaKnowledgeArticleDO article = validateArticleExists(updateReqVO.getId());
        // 原子递增版本号（带乐观锁），避免并发更新导致版本号丢失自增
        int updateCount = articleMapper.incrementVersionWithOptimisticLock(updateReqVO.getId(), article.getCurrentVersion());
        if (updateCount == 0) {
            throw exception(OA_KNOWLEDGE_ARTICLE_STATUS_INVALID, "文章已被其他用户更新，请刷新后重试");
        }
        // 更新文章内容（不含版本号，版本号已原子递增）
        OaKnowledgeArticleDO updateObj = BeanUtils.toBean(updateReqVO, OaKnowledgeArticleDO.class);
        updateObj.setCurrentVersion(null); // 避免覆盖已原子递增的版本号
        articleMapper.updateById(updateObj);
        // 自动记录版本（使用最新内容与版本号）
        OaKnowledgeArticleDO latest = articleMapper.selectById(updateReqVO.getId());
        saveVersion(latest, updateReqVO.getChangeLog());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteArticle(Long id) {
        validateArticleExists(id);
        // 级联清理：版本快照 + 评论，避免孤儿数据
        versionMapper.deleteByArticleId(id);
        commentMapper.deleteByArticleId(id);
        articleMapper.deleteById(id);
    }

    @Override
    public OaKnowledgeArticleDO getArticle(Long id) {
        return articleMapper.selectById(id);
    }

    @Override
    public PageResult<OaKnowledgeArticleDO> getArticlePage(OaKnowledgeArticlePageReqVO pageReqVO) {
        return articleMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishArticle(Long id) {
        // 校验存在 & 状态（仅草稿可发布）
        OaKnowledgeArticleDO article = validateArticleExists(id);
        if (!Integer.valueOf(STATUS_DRAFT).equals(article.getStatus())) {
            throw exception(OA_KNOWLEDGE_ARTICLE_STATUS_INVALID);
        }
        OaKnowledgeArticleDO updateObj = new OaKnowledgeArticleDO();
        updateObj.setId(id);
        updateObj.setStatus(STATUS_PUBLISHED);
        articleMapper.updateById(updateObj);
    }

    @Override
    public PageResult<OaKnowledgeArticleDO> searchArticles(PageParam pageParam, String keyword) {
        return articleMapper.searchPage(pageParam, keyword);
    }

    /**
     * 保存文章版本快照
     *
     * @param article   文章（含最新内容与版本号）
     * @param changeLog 变更说明
     */
    private void saveVersion(OaKnowledgeArticleDO article, String changeLog) {
        OaKnowledgeVersionDO version = OaKnowledgeVersionDO.builder()
                .articleId(article.getId())
                .versionNo(article.getCurrentVersion())
                .title(article.getTitle())
                .content(article.getContent())
                .summary(article.getSummary())
                .changeLog(changeLog)
                .editorUserId(SecurityFrameworkUtils.getLoginUserId())
                .editorName(article.getAuthorName())
                .build();
        versionMapper.insert(version);
    }

    private OaKnowledgeArticleDO validateArticleExists(Long id) {
        OaKnowledgeArticleDO article = articleMapper.selectById(id);
        if (article == null) {
            throw exception(OA_KNOWLEDGE_ARTICLE_NOT_EXISTS);
        }
        return article;
    }

}
