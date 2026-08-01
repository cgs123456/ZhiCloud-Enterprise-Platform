package cn.iocoder.yudao.module.oa.controller.admin.knowledge;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.oa.controller.admin.knowledge.vo.OaKnowledgeCommentPageReqVO;
import cn.iocoder.yudao.module.oa.controller.admin.knowledge.vo.OaKnowledgeCommentRespVO;
import cn.iocoder.yudao.module.oa.controller.admin.knowledge.vo.OaKnowledgeCommentSaveReqVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.knowledge.OaKnowledgeCommentDO;
import cn.iocoder.yudao.module.oa.service.knowledge.OaKnowledgeCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - OA 知识库评论")
@RestController
@RequestMapping("/oa/knowledge-comment")
@Validated
public class OaKnowledgeCommentController {

    @Resource
    private OaKnowledgeCommentService commentService;

    @PostMapping("/create")
    @Operation(summary = "创建知识库评论")
    @PreAuthorize("@ss.hasPermission('oa:knowledge-comment:create')")
    public CommonResult<Long> createComment(@Valid @RequestBody OaKnowledgeCommentSaveReqVO createReqVO) {
        return success(commentService.createComment(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新知识库评论")
    @PreAuthorize("@ss.hasPermission('oa:knowledge-comment:update')")
    public CommonResult<Boolean> updateComment(@Valid @RequestBody OaKnowledgeCommentSaveReqVO updateReqVO) {
        commentService.updateComment(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除知识库评论")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('oa:knowledge-comment:delete')")
    public CommonResult<Boolean> deleteComment(@RequestParam("id") Long id) {
        commentService.deleteComment(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得知识库评论")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('oa:knowledge-comment:query')")
    public CommonResult<OaKnowledgeCommentRespVO> getComment(@RequestParam("id") Long id) {
        OaKnowledgeCommentDO comment = commentService.getComment(id);
        return success(BeanUtils.toBean(comment, OaKnowledgeCommentRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得知识库评论分页")
    @PreAuthorize("@ss.hasPermission('oa:knowledge-comment:query')")
    public CommonResult<PageResult<OaKnowledgeCommentRespVO>> getCommentPage(@Valid OaKnowledgeCommentPageReqVO pageReqVO) {
        PageResult<OaKnowledgeCommentDO> pageResult = commentService.getCommentPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, OaKnowledgeCommentRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得知识库文章的评论树")
    @Parameter(name = "articleId", description = "文章编号", required = true)
    @PreAuthorize("@ss.hasPermission('oa:knowledge-comment:query')")
    public CommonResult<List<OaKnowledgeCommentRespVO>> getCommentList(@RequestParam("articleId") Long articleId) {
        return success(commentService.getCommentTreeByArticleId(articleId));
    }

}
