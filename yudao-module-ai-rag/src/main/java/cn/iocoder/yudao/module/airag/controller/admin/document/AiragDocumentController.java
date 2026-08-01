package cn.iocoder.yudao.module.airag.controller.admin.document;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.airag.controller.admin.document.vo.AiragDocumentPageReqVO;
import cn.iocoder.yudao.module.airag.controller.admin.document.vo.AiragDocumentRespVO;
import cn.iocoder.yudao.module.airag.controller.admin.document.vo.AiragDocumentUploadReqVO;
import cn.iocoder.yudao.module.airag.dal.dataobject.AiragDocumentDO;
import cn.iocoder.yudao.module.airag.service.document.AiragDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AI RAG 文档")
@RestController
@RequestMapping("/airag/document")
@Validated
public class AiragDocumentController {

    @Resource
    private AiragDocumentService documentService;

    @PostMapping("/upload")
    @Operation(summary = "上传文档")
    @PreAuthorize("@ss.hasPermission('airag:document:create')")
    public CommonResult<Long> uploadDocument(@RequestBody @Valid AiragDocumentUploadReqVO uploadReqVO) {
        return success(documentService.uploadDocument(uploadReqVO));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除文档")
    @Parameter(name = "id", description = "文档编号", required = true, example = "2048")
    @PreAuthorize("@ss.hasPermission('airag:document:delete')")
    public CommonResult<Boolean> deleteDocument(@RequestParam("id") Long id) {
        documentService.deleteDocument(id);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获取文档分页")
    @PreAuthorize("@ss.hasPermission('airag:document:query')")
    public CommonResult<PageResult<AiragDocumentRespVO>> getDocumentPage(
            @Valid AiragDocumentPageReqVO pageReqVO) {
        PageResult<AiragDocumentDO> pageResult = documentService.getDocumentPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, AiragDocumentRespVO.class));
    }

}
