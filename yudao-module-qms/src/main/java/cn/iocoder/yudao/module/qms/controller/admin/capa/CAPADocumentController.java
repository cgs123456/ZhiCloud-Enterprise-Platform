package cn.iocoder.yudao.module.qms.controller.admin.capa;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.capa.vo.CAPADocumentPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.capa.vo.CAPADocumentRespVO;
import cn.iocoder.yudao.module.qms.controller.admin.capa.vo.CAPADocumentSaveReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.capa.vo.CAPAStageTransitionReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.capa.vo.CAPAVerificationReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.capa.CAPADocumentDO;
import cn.iocoder.yudao.module.qms.framework.electronicsignature.ElectronicSignature;
import cn.iocoder.yudao.module.qms.service.capa.CAPADocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - QMS CAPA 文档")
@RestController
@RequestMapping("/qms/capa")
@Validated
public class CAPADocumentController {

    @Resource
    private CAPADocumentService capaDocumentService;

    @PostMapping("/create")
    @Operation(summary = "创建 CAPA 文档")
    @PreAuthorize("@ss.hasPermission('qms:capa:create')")
    public CommonResult<Long> createCAPADocument(@Valid @RequestBody CAPADocumentSaveReqVO createReqVO) {
        return success(capaDocumentService.createCAPADocument(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新 CAPA 文档")
    @PreAuthorize("@ss.hasPermission('qms:capa:update')")
    public CommonResult<Boolean> updateCAPADocument(@Valid @RequestBody CAPADocumentSaveReqVO updateReqVO) {
        capaDocumentService.updateCAPADocument(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除 CAPA 文档")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('qms:capa:delete')")
    public CommonResult<Boolean> deleteCAPADocument(@RequestParam("id") Long id) {
        capaDocumentService.deleteCAPADocument(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得 CAPA 文档")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:capa:query')")
    public CommonResult<CAPADocumentRespVO> getCAPADocument(@RequestParam("id") Long id) {
        CAPADocumentDO capaDocument = capaDocumentService.getCAPADocument(id);
        return success(BeanUtils.toBean(capaDocument, CAPADocumentRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得 CAPA 文档分页")
    @PreAuthorize("@ss.hasPermission('qms:capa:query')")
    public CommonResult<PageResult<CAPADocumentRespVO>> getCAPADocumentPage(@Valid CAPADocumentPageReqVO pageReqVO) {
        PageResult<CAPADocumentDO> pageResult = capaDocumentService.getCAPADocumentPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, CAPADocumentRespVO.class));
    }

    @PutMapping("/close")
    @Operation(summary = "关闭 CAPA 文档", description = "仅在阶段为「有效性验证」且验证结果为「通过」时允许关闭")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:capa:close')")
    @ElectronicSignature(meaning = "CAPA 纠正预防措施关闭", requireReason = true)
    public CommonResult<Boolean> closeCAPADocument(@RequestParam("id") Long id) {
        capaDocumentService.closeCAPADocument(id);
        return success(true);
    }

    // ==================== P0-4 CAPA 全流程状态机 ====================

    @PutMapping("/transition")
    @Operation(summary = "CAPA 阶段流转", description = "前进或后退 1 步，前进时校验当前阶段必填字段")
    @PreAuthorize("@ss.hasPermission('qms:capa:transition')")
    public CommonResult<Boolean> transitionStage(@Valid @RequestBody CAPAStageTransitionReqVO reqVO) {
        capaDocumentService.transitionStage(reqVO);
        return success(true);
    }

    @PutMapping("/verify")
    @Operation(summary = "提交有效性验证结果", description = "仅在阶段为「有效性验证」时允许，验证不通过将自动回退到纠正措施阶段")
    @PreAuthorize("@ss.hasPermission('qms:capa:verify')")
    public CommonResult<Boolean> submitVerification(@Valid @RequestBody CAPAVerificationReqVO reqVO) {
        capaDocumentService.submitVerification(reqVO);
        return success(true);
    }

}
