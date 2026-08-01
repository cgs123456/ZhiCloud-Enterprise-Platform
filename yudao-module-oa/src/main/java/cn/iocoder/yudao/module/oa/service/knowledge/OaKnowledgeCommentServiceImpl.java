package cn.iocoder.yudao.module.oa.service.knowledge;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.oa.controller.admin.knowledge.vo.OaKnowledgeCommentPageReqVO;
import cn.iocoder.yudao.module.oa.controller.admin.knowledge.vo.OaKnowledgeCommentRespVO;
import cn.iocoder.yudao.module.oa.controller.admin.knowledge.vo.OaKnowledgeCommentSaveReqVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.knowledge.OaKnowledgeArticleDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.knowledge.OaKnowledgeCommentDO;
import cn.iocoder.yudao.module.oa.dal.mysql.knowledge.OaKnowledgeArticleMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.knowledge.OaKnowledgeCommentMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.oa.enums.ErrorCodeConstants.OA_KNOWLEDGE_COMMENT_NOT_EXISTS;

/**
 * OA 知识库评论 Service 实现类
 *
 * @author yudao
 */
@Service
@Validated
public class OaKnowledgeCommentServiceImpl implements OaKnowledgeCommentService {

    /**
     * 根评论父 ID
     */
    private static final long ROOT_PARENT_ID = 0L;
    /**
     * 正常状态
     */
    private static final int STATUS_NORMAL = 0;
    /**
     * 已删除状态
     */
    private static final int STATUS_DELETED = 1;
    /**
     * 初始计数值
     */
    private static final int INITIAL_COUNT = 0;

    @Resource
    private OaKnowledgeCommentMapper commentMapper;
    @Resource
    private OaKnowledgeArticleMapper articleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createComment(OaKnowledgeCommentSaveReqVO createReqVO) {
        OaKnowledgeCommentDO comment = BeanUtils.toBean(createReqVO, OaKnowledgeCommentDO.class);
        if (comment.getParentId() == null) {
            comment.setParentId(ROOT_PARENT_ID);
        }
        if (comment.getCommentatorUserId() == null) {
            comment.setCommentatorUserId(SecurityFrameworkUtils.getLoginUserId());
        }
        if (comment.getStatus() == null) {
            comment.setStatus(STATUS_NORMAL);
        }
        comment.setLikeCount(INITIAL_COUNT);
        commentMapper.insert(comment);
        // 文章评论数 +1
        updateCommentCount(createReqVO.getArticleId(), 1);
        return comment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateComment(OaKnowledgeCommentSaveReqVO updateReqVO) {
        validateCommentExists(updateReqVO.getId());
        OaKnowledgeCommentDO updateObj = BeanUtils.toBean(updateReqVO, OaKnowledgeCommentDO.class);
        commentMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long id) {
        // 校验存在
        OaKnowledgeCommentDO comment = validateCommentExists(id);
        // 软删除（保留父子评论结构）
        OaKnowledgeCommentDO updateObj = new OaKnowledgeCommentDO();
        updateObj.setId(id);
        updateObj.setStatus(STATUS_DELETED);
        commentMapper.updateById(updateObj);
        // 文章评论数 -1
        updateCommentCount(comment.getArticleId(), -1);
    }

    @Override
    public OaKnowledgeCommentDO getComment(Long id) {
        return commentMapper.selectById(id);
    }

    @Override
    public PageResult<OaKnowledgeCommentDO> getCommentPage(OaKnowledgeCommentPageReqVO pageReqVO) {
        return commentMapper.selectPage(pageReqVO);
    }

    @Override
    public List<OaKnowledgeCommentRespVO> getCommentTreeByArticleId(Long articleId) {
        // 查询文章的全部评论（按 id 升序）
        List<OaKnowledgeCommentDO> list = commentMapper.selectListByArticleId(articleId);
        List<OaKnowledgeCommentRespVO> voList = BeanUtils.toBean(list, OaKnowledgeCommentRespVO.class);
        // 构建评论树
        Map<Long, OaKnowledgeCommentRespVO> map = new LinkedHashMap<>();
        for (OaKnowledgeCommentRespVO vo : voList) {
            vo.setChildren(new ArrayList<>());
            map.put(vo.getId(), vo);
        }
        List<OaKnowledgeCommentRespVO> roots = new ArrayList<>();
        for (OaKnowledgeCommentRespVO vo : voList) {
            Long parentId = vo.getParentId();
            if (parentId == null || parentId == ROOT_PARENT_ID) {
                roots.add(vo);
            } else {
                OaKnowledgeCommentRespVO parent = map.get(parentId);
                if (parent != null) {
                    parent.getChildren().add(vo);
                } else {
                    // 父评论不存在，作为根评论
                    roots.add(vo);
                }
            }
        }
        return roots;
    }

    /**
     * 更新文章评论数
     *
     * @param articleId 文章 ID
     * @param delta     增量（正数增加，负数减少）
     */
    private void updateCommentCount(Long articleId, int delta) {
        if (articleId == null) {
            return;
        }
        OaKnowledgeArticleDO article = articleMapper.selectById(articleId);
        if (article == null) {
            return;
        }
        OaKnowledgeArticleDO updateObj = new OaKnowledgeArticleDO();
        updateObj.setId(articleId);
        updateObj.setCommentCount((article.getCommentCount() == null ? 0 : article.getCommentCount()) + delta);
        articleMapper.updateById(updateObj);
    }

    private OaKnowledgeCommentDO validateCommentExists(Long id) {
        OaKnowledgeCommentDO comment = commentMapper.selectById(id);
        if (comment == null) {
            throw exception(OA_KNOWLEDGE_COMMENT_NOT_EXISTS);
        }
        return comment;
    }

}
