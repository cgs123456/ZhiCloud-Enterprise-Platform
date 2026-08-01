package cn.iocoder.yudao.module.qms.controller.admin.document;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.document.vo.*;
import cn.iocoder.yudao.module.qms.dal.dataobject.document.QmsDocumentDistributeDO;
import cn.iocoder.yudao.module.qms.service.document.QmsDocumentDistributeService;
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
 * QMS 文档分发记录 Controller
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - QMS 文档分发记录")
@RestController
@RequestMapping("/qms/document-distribute")
@Validated
public class QmsDocumentDistributeController {

    @Resource
    private QmsDocumentDistributeService distributeService;

    @PostMapping("/create")
    @Operation(summary = "创建分发记录", description = "仅在文档状态为「已发布」时允许")
    @PreAuthorize("@ss.hasPermission('qms:document:distribute')")
    public CommonResult<Long> distributeDocument(@Valid @RequestBody QmsDocumentDistributeSaveReqVO createReqVO) {
        return success(distributeService.distributeDocument(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新分发记录")
    @PreAuthorize("@ss.hasPermission('qms:document:distribute')")
    public CommonResult<Boolean> updateDistribute(@Valid @RequestBody QmsDocumentDistributeSaveReqVO updateReqVO) {
        distributeService.updateDistribute(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除分发记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('qms:document:distribute')")
    public CommonResult<Boolean> deleteDistribute(@RequestParam("id") Long id) {
        distributeService.deleteDistribute(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得分发记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:document:query')")
    public CommonResult<QmsDocumentDistributeRespVO> getDistribute(@RequestParam("id") Long id) {
        QmsDocumentDistributeDO distribute = distributeService.getDistribute(id);
        return success(BeanUtils.toBean(distribute, QmsDocumentDistributeRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得分发记录分页")
    @PreAuthorize("@ss.hasPermission('qms:document:query')")
    public CommonResult<PageResult<QmsDocumentDistributeRespVO>> getDistributePage(@Valid QmsDocumentDistributePageReqVO pageReqVO) {
        PageResult<QmsDocumentDistributeDO> pageResult = distributeService.getDistributePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, QmsDocumentDistributeRespVO.class));
    }

    @PutMapping("/return")
    @Operation(summary = "回收登记", description = "更新回收份数与回收日期")
    @PreAuthorize("@ss.hasPermission('qms:document:distribute')")
    public CommonResult<Boolean> returnDocument(@Valid @RequestBody QmsDocumentDistributeReturnReqVO reqVO) {
        distributeService.returnDocument(reqVO);
        return success(true);
    }

    @GetMapping("/list-by-document")
    @Operation(summary = "获得文档关联的分发记录列表")
    @Parameter(name = "documentId", description = "受控文档 ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:document:query')")
    public CommonResult<List<QmsDocumentDistributeRespVO>> getDistributeListByDocumentId(@RequestParam("documentId") Long documentId) {
        List<QmsDocumentDistributeDO> list = distributeService.getDistributeListByDocumentId(documentId);
        return success(BeanUtils.toBean(list, QmsDocumentDistributeRespVO.class));
    }

}
