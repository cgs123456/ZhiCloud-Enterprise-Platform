package cn.iocoder.yudao.module.qms.controller.admin.fmea;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.fmea.vo.FmeaDocumentPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.fmea.vo.FmeaDocumentRespVO;
import cn.iocoder.yudao.module.qms.controller.admin.fmea.vo.FmeaDocumentSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.fmea.FmeaDocumentDO;
import cn.iocoder.yudao.module.qms.service.fmea.FmeaDocumentService;
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
 * QMS FMEA 文档 Controller
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - QMS FMEA 失效模式分析")
@RestController
@RequestMapping("/qms/fmea")
@Validated
public class FmeaDocumentController {

    @Resource
    private FmeaDocumentService fmeaDocumentService;

    @PostMapping("/create")
    @Operation(summary = "创建 FMEA 文档")
    @PreAuthorize("@ss.hasPermission('qms:fmea:create')")
    public CommonResult<Long> createFmeaDocument(@Valid @RequestBody FmeaDocumentSaveReqVO createReqVO) {
        return success(fmeaDocumentService.createFmeaDocument(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新 FMEA 文档")
    @PreAuthorize("@ss.hasPermission('qms:fmea:update')")
    public CommonResult<Boolean> updateFmeaDocument(@Valid @RequestBody FmeaDocumentSaveReqVO updateReqVO) {
        fmeaDocumentService.updateFmeaDocument(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除 FMEA 文档")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('qms:fmea:delete')")
    public CommonResult<Boolean> deleteFmeaDocument(@RequestParam("id") Long id) {
        fmeaDocumentService.deleteFmeaDocument(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得 FMEA 文档")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:fmea:query')")
    public CommonResult<FmeaDocumentRespVO> getFmeaDocument(@RequestParam("id") Long id) {
        FmeaDocumentDO fmeaDocument = fmeaDocumentService.getFmeaDocument(id);
        return success(BeanUtils.toBean(fmeaDocument, FmeaDocumentRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得 FMEA 文档分页")
    @PreAuthorize("@ss.hasPermission('qms:fmea:query')")
    public CommonResult<PageResult<FmeaDocumentRespVO>> getFmeaDocumentPage(@Valid FmeaDocumentPageReqVO pageReqVO) {
        PageResult<FmeaDocumentDO> pageResult = fmeaDocumentService.getFmeaDocumentPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, FmeaDocumentRespVO.class));
    }

}
