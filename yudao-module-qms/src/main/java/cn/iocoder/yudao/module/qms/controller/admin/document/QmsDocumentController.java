package cn.iocoder.yudao.module.qms.controller.admin.document;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.document.vo.*;
import cn.iocoder.yudao.module.qms.dal.dataobject.document.QmsDocumentDO;
import cn.iocoder.yudao.module.qms.service.document.QmsDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * QMS 受控文档 Controller
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - QMS 受控文档")
@RestController
@RequestMapping("/qms/document")
@Validated
public class QmsDocumentController {

    @Resource
    private QmsDocumentService documentService;

    @PostMapping("/create")
    @Operation(summary = "创建受控文档")
    @PreAuthorize("@ss.hasPermission('qms:document:create')")
    public CommonResult<Long> createDocument(@Valid @RequestBody QmsDocumentSaveReqVO createReqVO) {
        return success(documentService.createDocument(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新受控文档")
    @PreAuthorize("@ss.hasPermission('qms:document:update')")
    public CommonResult<Boolean> updateDocument(@Valid @RequestBody QmsDocumentSaveReqVO updateReqVO) {
        documentService.updateDocument(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除受控文档")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('qms:document:delete')")
    public CommonResult<Boolean> deleteDocument(@RequestParam("id") Long id) {
        documentService.deleteDocument(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得受控文档")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:document:query')")
    public CommonResult<QmsDocumentRespVO> getDocument(@RequestParam("id") Long id) {
        QmsDocumentDO document = documentService.getDocument(id);
        return success(BeanUtils.toBean(document, QmsDocumentRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得受控文档分页")
    @PreAuthorize("@ss.hasPermission('qms:document:query')")
    public CommonResult<PageResult<QmsDocumentRespVO>> getDocumentPage(@Valid QmsDocumentPageReqVO pageReqVO) {
        PageResult<QmsDocumentDO> pageResult = documentService.getDocumentPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, QmsDocumentRespVO.class));
    }

    @PutMapping("/submit")
    @Operation(summary = "提交审核", description = "仅在状态为「草稿」时允许，提交后流转为「待审」")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:document:approve')")
    public CommonResult<Boolean> submitDocument(@RequestParam("id") Long id) {
        documentService.submitDocument(id);
        return success(true);
    }

    @PutMapping("/approve")
    @Operation(summary = "审核通过并发布", description = "仅在状态为「待审」时允许，发布后 version + 1")
    @PreAuthorize("@ss.hasPermission('qms:document:approve')")
    public CommonResult<Boolean> approveDocument(@RequestParam("id") Long id,
                                                 @RequestParam(value = "fileUrl", required = false) String fileUrl) {
        documentService.approveDocument(id, fileUrl);
        return success(true);
    }

    @PutMapping("/reject")
    @Operation(summary = "审核驳回", description = "仅在状态为「待审」时允许，驳回后流转回「草稿」")
    @PreAuthorize("@ss.hasPermission('qms:document:approve')")
    public CommonResult<Boolean> rejectDocument(@Valid @RequestBody QmsDocumentRejectReqVO reqVO) {
        documentService.rejectDocument(reqVO.getId(), reqVO.getReason());
        return success(true);
    }

    @PutMapping("/revoke")
    @Operation(summary = "作废文档", description = "仅在状态为「已发布」时允许，作废后流转为「已作废」")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:document:revoke')")
    public CommonResult<Boolean> revokeDocument(@RequestParam("id") Long id) {
        documentService.revokeDocument(id);
        return success(true);
    }

}
