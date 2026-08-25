package cn.zhicloud.module.oa.controller.admin.knowledge;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.oa.controller.admin.knowledge.vo.OaKnowledgeCategoryPageReqVO;
import cn.zhicloud.module.oa.controller.admin.knowledge.vo.OaKnowledgeCategoryRespVO;
import cn.zhicloud.module.oa.controller.admin.knowledge.vo.OaKnowledgeCategorySaveReqVO;
import cn.zhicloud.module.oa.dal.dataobject.knowledge.OaKnowledgeCategoryDO;
import cn.zhicloud.module.oa.service.knowledge.OaKnowledgeCategoryService;
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

@Tag(name = "管理后台 - OA 知识库分类")
@RestController
@RequestMapping("/oa/knowledge-category")
@Validated
public class OaKnowledgeCategoryController {

    @Resource
    private OaKnowledgeCategoryService categoryService;

    @PostMapping("/create")
    @Operation(summary = "创建知识库分类")
    @PreAuthorize("@ss.hasPermission('oa:knowledge-category:create')")
    public CommonResult<Long> createCategory(@Valid @RequestBody OaKnowledgeCategorySaveReqVO createReqVO) {
        return success(categoryService.createCategory(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新知识库分类")
    @PreAuthorize("@ss.hasPermission('oa:knowledge-category:update')")
    public CommonResult<Boolean> updateCategory(@Valid @RequestBody OaKnowledgeCategorySaveReqVO updateReqVO) {
        categoryService.updateCategory(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除知识库分类")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('oa:knowledge-category:delete')")
    public CommonResult<Boolean> deleteCategory(@RequestParam("id") Long id) {
        categoryService.deleteCategory(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得知识库分类")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('oa:knowledge-category:query')")
    public CommonResult<OaKnowledgeCategoryRespVO> getCategory(@RequestParam("id") Long id) {
        OaKnowledgeCategoryDO category = categoryService.getCategory(id);
        return success(BeanUtils.toBean(category, OaKnowledgeCategoryRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得知识库分类分页")
    @PreAuthorize("@ss.hasPermission('oa:knowledge-category:query')")
    public CommonResult<PageResult<OaKnowledgeCategoryRespVO>> getCategoryPage(@Valid OaKnowledgeCategoryPageReqVO pageReqVO) {
        PageResult<OaKnowledgeCategoryDO> pageResult = categoryService.getCategoryPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, OaKnowledgeCategoryRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得知识库分类列表（用于构建树）")
    @PreAuthorize("@ss.hasPermission('oa:knowledge-category:query')")
    public CommonResult<List<OaKnowledgeCategoryRespVO>> getCategoryList() {
        List<OaKnowledgeCategoryDO> list = categoryService.getCategoryList();
        return success(BeanUtils.toBean(list, OaKnowledgeCategoryRespVO.class));
    }

}
