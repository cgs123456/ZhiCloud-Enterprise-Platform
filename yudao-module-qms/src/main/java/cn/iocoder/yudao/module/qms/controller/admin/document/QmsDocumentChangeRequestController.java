package cn.iocoder.yudao.module.qms.controller.admin.document;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.document.vo.*;
import cn.iocoder.yudao.module.qms.dal.dataobject.document.QmsDocumentChangeRequestDO;
import cn.iocoder.yudao.module.qms.framework.electronicsignature.ElectronicSignature;
import cn.iocoder.yudao.module.qms.service.document.QmsDocumentChangeRequestService;
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

/**
 * QMS 文件变更申请 Controller
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - QMS 文件变更申请")
@RestController
@RequestMapping("/qms/document-change-request")
@Validated
public class QmsDocumentChangeRequestController {

    @Resource
    private QmsDocumentChangeRequestService changeRequestService;

    @PostMapping("/create")
    @Operation(summary = "创建变更申请")
    @PreAuthorize("@ss.hasPermission('qms:document:change')")
    public CommonResult<Long> createChangeRequest(@Valid @RequestBody QmsDocumentChangeRequestSaveReqVO createReqVO) {
        return success(changeRequestService.createChangeRequest(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新变更申请")
    @PreAuthorize("@ss.hasPermission('qms:document:change')")
    public CommonResult<Boolean> updateChangeRequest(@Valid @RequestBody QmsDocumentChangeRequestSaveReqVO updateReqVO) {
        changeRequestService.updateChangeRequest(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除变更申请")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('qms:document:change')")
    public CommonResult<Boolean> deleteChangeRequest(@RequestParam("id") Long id) {
        changeRequestService.deleteChangeRequest(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得变更申请")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:document:query')")
    public CommonResult<QmsDocumentChangeRequestRespVO> getChangeRequest(@RequestParam("id") Long id) {
        QmsDocumentChangeRequestDO changeRequest = changeRequestService.getChangeRequest(id);
        return success(BeanUtils.toBean(changeRequest, QmsDocumentChangeRequestRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得变更申请分页")
    @PreAuthorize("@ss.hasPermission('qms:document:query')")
    public CommonResult<PageResult<QmsDocumentChangeRequestRespVO>> getChangeRequestPage(@Valid QmsDocumentChangeRequestPageReqVO pageReqVO) {
        PageResult<QmsDocumentChangeRequestDO> pageResult = changeRequestService.getChangeRequestPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, QmsDocumentChangeRequestRespVO.class));
    }

    @PutMapping("/approve")
    @Operation(summary = "审核通过变更申请", description = "通过后自动创建新版本文档（修订/新增）或将原文档置为已作废（作废）")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:document:approve')")
    @ElectronicSignature(meaning = "文件变更申请批准", requireReason = true)
    public CommonResult<Boolean> approveChangeRequest(@RequestParam("id") Long id) {
        changeRequestService.approveChangeRequest(id);
        return success(true);
    }

    @PutMapping("/reject")
    @Operation(summary = "审核驳回变更申请")
    @PreAuthorize("@ss.hasPermission('qms:document:approve')")
    public CommonResult<Boolean> rejectChangeRequest(@Valid @RequestBody QmsDocumentChangeRequestRejectReqVO reqVO) {
        changeRequestService.rejectChangeRequest(reqVO.getId(), reqVO.getReason());
        return success(true);
    }

    @GetMapping("/list-by-document")
    @Operation(summary = "获得文档关联的变更申请列表")
    @Parameter(name = "documentId", description = "受控文档 ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:document:query')")
    public CommonResult<List<QmsDocumentChangeRequestRespVO>> getChangeRequestListByDocumentId(@RequestParam("documentId") Long documentId) {
        List<QmsDocumentChangeRequestDO> list = changeRequestService.getChangeRequestListByDocumentId(documentId);
        return success(BeanUtils.toBean(list, QmsDocumentChangeRequestRespVO.class));
    }

}
