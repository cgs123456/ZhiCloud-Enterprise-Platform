package cn.iocoder.yudao.module.oa.controller.admin.document;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.oa.controller.admin.document.vo.OaDocumentAttachmentVO;
import cn.iocoder.yudao.module.oa.controller.admin.document.vo.OaDocumentPageReqVO;
import cn.iocoder.yudao.module.oa.controller.admin.document.vo.OaDocumentRespVO;
import cn.iocoder.yudao.module.oa.controller.admin.document.vo.OaDocumentSaveReqVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.document.OaDocumentAttachmentDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.document.OaDocumentDO;
import cn.iocoder.yudao.module.oa.dal.mysql.document.OaDocumentAttachmentMapper;
import cn.iocoder.yudao.module.oa.service.document.OaDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.pojo.PageParam.PAGE_SIZE_NONE;

@Tag(name = "管理后台 - OA 公文管理")
@RestController
@RequestMapping("/oa/document")
@Validated
public class OaDocumentController {

    @Resource
    private OaDocumentService documentService;
    @Resource
    private OaDocumentAttachmentMapper documentAttachmentMapper;

    @PostMapping("/create")
    @Operation(summary = "创建公文")
    @PreAuthorize("@ss.hasPermission('oa:document:create')")
    public CommonResult<Long> createDocument(@Valid @RequestBody OaDocumentSaveReqVO createReqVO) {
        return success(documentService.createDocument(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新公文")
    @PreAuthorize("@ss.hasPermission('oa:document:update')")
    public CommonResult<Boolean> updateDocument(@Valid @RequestBody OaDocumentSaveReqVO updateReqVO) {
        documentService.updateDocument(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除公文")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('oa:document:delete')")
    public CommonResult<Boolean> deleteDocument(@RequestParam("id") Long id) {
        documentService.deleteDocument(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得公文")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('oa:document:query')")
    public CommonResult<OaDocumentRespVO> getDocument(@RequestParam("id") Long id) {
        OaDocumentDO document = documentService.getDocument(id);
        OaDocumentRespVO respVO = BeanUtils.toBean(document, OaDocumentRespVO.class);
        if (respVO != null) {
            List<OaDocumentAttachmentDO> attachments = documentAttachmentMapper.selectListByDocumentId(id);
            respVO.setAttachments(BeanUtils.toBean(attachments, OaDocumentAttachmentVO.class));
        }
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得公文分页")
    @PreAuthorize("@ss.hasPermission('oa:document:query')")
    public CommonResult<PageResult<OaDocumentRespVO>> getDocumentPage(@Valid OaDocumentPageReqVO pageReqVO) {
        PageResult<OaDocumentDO> pageResult = documentService.getDocumentPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, OaDocumentRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出公文 Excel")
    @PreAuthorize("@ss.hasPermission('oa:document:export')")
    public void exportDocumentExcel(@Valid OaDocumentPageReqVO exportReqVO,
                                    HttpServletResponse response) throws IOException {
        exportReqVO.setPageSize(PAGE_SIZE_NONE);
        List<OaDocumentDO> list = documentService.getDocumentPage(exportReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "公文.xls", "数据", OaDocumentRespVO.class,
                BeanUtils.toBean(list, OaDocumentRespVO.class));
    }

    @PutMapping("/submit")
    @Operation(summary = "提交公文审核")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('oa:document:update')")
    public CommonResult<Boolean> submitDocument(@RequestParam("id") Long id) {
        documentService.submitDocument(id);
        return success(true);
    }

    @PutMapping("/publish")
    @Operation(summary = "发布公文")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('oa:document:update')")
    public CommonResult<Boolean> publishDocument(@RequestParam("id") Long id) {
        documentService.publishDocument(id);
        return success(true);
    }

    @PutMapping("/void")
    @Operation(summary = "废止公文")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('oa:document:update')")
    public CommonResult<Boolean> voidDocument(@RequestParam("id") Long id) {
        documentService.voidDocument(id);
        return success(true);
    }

    @PutMapping("/review-pass")
    @Operation(summary = "核稿通过")
    @Parameter(name = "id", description = "编号", required = true)
    @Parameter(name = "opinion", description = "核稿意见")
    @PreAuthorize("@ss.hasPermission('oa:document:review')")
    public CommonResult<Boolean> reviewPassDocument(@RequestParam("id") Long id,
                                                     @RequestParam(value = "opinion", required = false) String opinion) {
        documentService.reviewPassDocument(id, opinion);
        return success(true);
    }

    @PutMapping("/review-reject")
    @Operation(summary = "核稿驳回")
    @Parameter(name = "id", description = "编号", required = true)
    @Parameter(name = "opinion", description = "核稿意见")
    @PreAuthorize("@ss.hasPermission('oa:document:review')")
    public CommonResult<Boolean> reviewRejectDocument(@RequestParam("id") Long id,
                                                       @RequestParam(value = "opinion", required = false) String opinion) {
        documentService.reviewRejectDocument(id, opinion);
        return success(true);
    }

    @PutMapping("/sign")
    @Operation(summary = "签发公文")
    @Parameter(name = "id", description = "编号", required = true)
    @Parameter(name = "opinion", description = "签发意见")
    @PreAuthorize("@ss.hasPermission('oa:document:sign')")
    public CommonResult<Boolean> signDocument(@RequestParam("id") Long id,
                                              @RequestParam(value = "opinion", required = false) String opinion) {
        documentService.signDocument(id, opinion);
        return success(true);
    }

    @PutMapping("/archive")
    @Operation(summary = "归档公文")
    @Parameter(name = "id", description = "编号", required = true)
    @Parameter(name = "archiveNo", description = "归档编号", required = true)
    @PreAuthorize("@ss.hasPermission('oa:document:archive')")
    public CommonResult<Boolean> archiveDocument(@RequestParam("id") Long id,
                                                 @RequestParam("archiveNo") String archiveNo) {
        documentService.archiveDocument(id, archiveNo);
        return success(true);
    }

    @PutMapping("/increment-read")
    @Operation(summary = "增加阅读量")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('oa:document:update')")
    public CommonResult<Boolean> incrementReadCount(@RequestParam("id") Long id) {
        documentService.incrementReadCount(id);
        return success(true);
    }

}
