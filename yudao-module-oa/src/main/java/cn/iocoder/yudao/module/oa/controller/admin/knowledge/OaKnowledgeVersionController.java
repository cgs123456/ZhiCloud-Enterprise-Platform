package cn.iocoder.yudao.module.oa.controller.admin.knowledge;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.oa.controller.admin.knowledge.vo.OaKnowledgeVersionPageReqVO;
import cn.iocoder.yudao.module.oa.controller.admin.knowledge.vo.OaKnowledgeVersionRespVO;
import cn.iocoder.yudao.module.oa.controller.admin.knowledge.vo.OaKnowledgeVersionSaveReqVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.knowledge.OaKnowledgeVersionDO;
import cn.iocoder.yudao.module.oa.service.knowledge.OaKnowledgeVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - OA 知识库版本")
@RestController
@RequestMapping("/oa/knowledge-version")
@Validated
public class OaKnowledgeVersionController {

    @Resource
    private OaKnowledgeVersionService versionService;

    @PostMapping("/create")
    @Operation(summary = "创建知识库版本")
    @PreAuthorize("@ss.hasPermission('oa:knowledge-article:update')")
    public CommonResult<Long> createVersion(@Valid @RequestBody OaKnowledgeVersionSaveReqVO createReqVO) {
        return success(versionService.createVersion(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新知识库版本")
    @PreAuthorize("@ss.hasPermission('oa:knowledge-article:update')")
    public CommonResult<Boolean> updateVersion(@Valid @RequestBody OaKnowledgeVersionSaveReqVO updateReqVO) {
        versionService.updateVersion(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除知识库版本")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('oa:knowledge-article:update')")
    public CommonResult<Boolean> deleteVersion(@RequestParam("id") Long id) {
        versionService.deleteVersion(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得知识库版本")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('oa:knowledge-article:query')")
    public CommonResult<OaKnowledgeVersionRespVO> getVersion(@RequestParam("id") Long id) {
        OaKnowledgeVersionDO version = versionService.getVersion(id);
        return success(BeanUtils.toBean(version, OaKnowledgeVersionRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得知识库版本分页")
    @PreAuthorize("@ss.hasPermission('oa:knowledge-article:query')")
    public CommonResult<PageResult<OaKnowledgeVersionRespVO>> getVersionPage(@Valid OaKnowledgeVersionPageReqVO pageReqVO) {
        PageResult<OaKnowledgeVersionDO> pageResult = versionService.getVersionPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, OaKnowledgeVersionRespVO.class));
    }

}
