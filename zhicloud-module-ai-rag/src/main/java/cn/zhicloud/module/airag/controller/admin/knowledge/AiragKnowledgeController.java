package cn.zhicloud.module.airag.controller.admin.knowledge;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.airag.controller.admin.knowledge.vo.AiragKnowledgePageReqVO;
import cn.zhicloud.module.airag.controller.admin.knowledge.vo.AiragKnowledgeRespVO;
import cn.zhicloud.module.airag.controller.admin.knowledge.vo.AiragKnowledgeSaveReqVO;
import cn.zhicloud.module.airag.dal.dataobject.AiragKnowledgeDO;
import cn.zhicloud.module.airag.service.knowledge.AiragKnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AI RAG 知识库")
@RestController
@RequestMapping("/airag/knowledge")
@Validated
public class AiragKnowledgeController {

    @Resource
    private AiragKnowledgeService knowledgeService;

    @PostMapping("/create")
    @Operation(summary = "创建知识库")
    @PreAuthorize("@ss.hasPermission('airag:knowledge:create')")
    public CommonResult<Long> createKnowledge(@RequestBody @Valid AiragKnowledgeSaveReqVO createReqVO) {
        return success(knowledgeService.createKnowledge(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新知识库")
    @PreAuthorize("@ss.hasPermission('airag:knowledge:update')")
    public CommonResult<Boolean> updateKnowledge(@RequestBody @Valid AiragKnowledgeSaveReqVO updateReqVO) {
        knowledgeService.updateKnowledge(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除知识库")
    @Parameter(name = "id", description = "知识库编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('airag:knowledge:delete')")
    public CommonResult<Boolean> deleteKnowledge(@RequestParam("id") Long id) {
        knowledgeService.deleteKnowledge(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取知识库")
    @Parameter(name = "id", description = "知识库编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('airag:knowledge:query')")
    public CommonResult<AiragKnowledgeRespVO> getKnowledge(@RequestParam("id") Long id) {
        AiragKnowledgeDO knowledge = knowledgeService.getKnowledge(id);
        return success(BeanUtils.toBean(knowledge, AiragKnowledgeRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获取知识库分页")
    @PreAuthorize("@ss.hasPermission('airag:knowledge:query')")
    public CommonResult<PageResult<AiragKnowledgeRespVO>> getKnowledgePage(
            @Valid AiragKnowledgePageReqVO pageReqVO) {
        PageResult<AiragKnowledgeDO> pageResult = knowledgeService.getKnowledgePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, AiragKnowledgeRespVO.class));
    }

}
