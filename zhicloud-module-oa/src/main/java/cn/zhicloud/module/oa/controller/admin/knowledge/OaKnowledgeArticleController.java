package cn.zhicloud.module.oa.controller.admin.knowledge;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageParam;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.oa.controller.admin.knowledge.vo.OaKnowledgeArticlePageReqVO;
import cn.zhicloud.module.oa.controller.admin.knowledge.vo.OaKnowledgeArticleRespVO;
import cn.zhicloud.module.oa.controller.admin.knowledge.vo.OaKnowledgeArticleSaveReqVO;
import cn.zhicloud.module.oa.controller.admin.knowledge.vo.OaKnowledgeVersionRespVO;
import cn.zhicloud.module.oa.dal.dataobject.knowledge.OaKnowledgeArticleDO;
import cn.zhicloud.module.oa.dal.dataobject.knowledge.OaKnowledgeVersionDO;
import cn.zhicloud.module.oa.service.knowledge.OaKnowledgeArticleService;
import cn.zhicloud.module.oa.service.knowledge.OaKnowledgeVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - OA 知识库文章")
@RestController
@RequestMapping("/oa/knowledge-article")
@Validated
public class OaKnowledgeArticleController {

    @Resource
    private OaKnowledgeArticleService articleService;
    @Resource
    private OaKnowledgeVersionService versionService;

    @PostMapping("/create")
    @Operation(summary = "创建知识库文章")
    @PreAuthorize("@ss.hasPermission('oa:knowledge-article:create')")
    public CommonResult<Long> createArticle(@Valid @RequestBody OaKnowledgeArticleSaveReqVO createReqVO) {
        return success(articleService.createArticle(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新知识库文章")
    @PreAuthorize("@ss.hasPermission('oa:knowledge-article:update')")
    public CommonResult<Boolean> updateArticle(@Valid @RequestBody OaKnowledgeArticleSaveReqVO updateReqVO) {
        articleService.updateArticle(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除知识库文章")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('oa:knowledge-article:delete')")
    public CommonResult<Boolean> deleteArticle(@RequestParam("id") Long id) {
        articleService.deleteArticle(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得知识库文章")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('oa:knowledge-article:query')")
    public CommonResult<OaKnowledgeArticleRespVO> getArticle(@RequestParam("id") Long id) {
        OaKnowledgeArticleDO article = articleService.getArticle(id);
        return success(BeanUtils.toBean(article, OaKnowledgeArticleRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得知识库文章分页")
    @PreAuthorize("@ss.hasPermission('oa:knowledge-article:query')")
    public CommonResult<PageResult<OaKnowledgeArticleRespVO>> getArticlePage(@Valid OaKnowledgeArticlePageReqVO pageReqVO) {
        PageResult<OaKnowledgeArticleDO> pageResult = articleService.getArticlePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, OaKnowledgeArticleRespVO.class));
    }

    @PutMapping("/publish")
    @Operation(summary = "发布知识库文章")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('oa:knowledge-article:publish')")
    public CommonResult<Boolean> publishArticle(@RequestParam("id") Long id) {
        articleService.publishArticle(id);
        return success(true);
    }

    @GetMapping("/search")
    @Operation(summary = "全文检索知识库文章")
    @Parameter(name = "keyword", description = "关键词", example = "Spring")
    @PreAuthorize("@ss.hasPermission('oa:knowledge-article:query')")
    public CommonResult<PageResult<OaKnowledgeArticleRespVO>> searchArticles(@Valid PageParam pageParam,
                                                                              @RequestParam(value = "keyword", required = false) String keyword) {
        PageResult<OaKnowledgeArticleDO> pageResult = articleService.searchArticles(pageParam, keyword);
        return success(BeanUtils.toBean(pageResult, OaKnowledgeArticleRespVO.class));
    }

    @GetMapping("/versions")
    @Operation(summary = "获得知识库文章的所有版本")
    @Parameter(name = "articleId", description = "文章编号", required = true)
    @PreAuthorize("@ss.hasPermission('oa:knowledge-article:query')")
    public CommonResult<List<OaKnowledgeVersionRespVO>> getArticleVersions(@RequestParam("articleId") Long articleId) {
        List<OaKnowledgeVersionDO> list = versionService.getVersionListByArticleId(articleId);
        return success(BeanUtils.toBean(list, OaKnowledgeVersionRespVO.class));
    }

}
