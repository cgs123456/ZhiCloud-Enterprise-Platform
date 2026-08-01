package cn.iocoder.yudao.module.oa.service.knowledge;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.controller.admin.knowledge.vo.OaKnowledgeCommentPageReqVO;
import cn.iocoder.yudao.module.oa.controller.admin.knowledge.vo.OaKnowledgeCommentRespVO;
import cn.iocoder.yudao.module.oa.controller.admin.knowledge.vo.OaKnowledgeCommentSaveReqVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.knowledge.OaKnowledgeCommentDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * OA 知识库评论 Service 接口
 *
 * @author yudao
 */
public interface OaKnowledgeCommentService {

    /**
     * 创建知识库评论
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createComment(@Valid OaKnowledgeCommentSaveReqVO createReqVO);

    /**
     * 更新知识库评论
     *
     * @param updateReqVO 更新信息
     */
    void updateComment(@Valid OaKnowledgeCommentSaveReqVO updateReqVO);

    /**
     * 删除知识库评论
     *
     * @param id 编号
     */
    void deleteComment(Long id);

    /**
     * 获得知识库评论
     *
     * @param id 编号
     * @return 知识库评论
     */
    OaKnowledgeCommentDO getComment(Long id);

    /**
     * 获得知识库评论分页
     *
     * @param pageReqVO 分页查询
     * @return 知识库评论分页
     */
    PageResult<OaKnowledgeCommentDO> getCommentPage(OaKnowledgeCommentPageReqVO pageReqVO);

    /**
     * 获得某文章的评论树（含子评论）
     *
     * @param articleId 文章 ID
     * @return 评论树
     */
    List<OaKnowledgeCommentRespVO> getCommentTreeByArticleId(Long articleId);

}
