package cn.iocoder.yudao.module.qms.controller.admin.ncr;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.ncr.vo.*;
import cn.iocoder.yudao.module.qms.dal.dataobject.ncr.NcrDocumentDO;
import cn.iocoder.yudao.module.qms.dal.dataobject.ncr.NcrMrbRecordDO;
import cn.iocoder.yudao.module.qms.service.ncr.NcrDocumentService;
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
 * QMS 不合格品报告 Controller
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - QMS 不合格品处理")
@RestController
@RequestMapping("/qms/ncr")
@Validated
public class NcrDocumentController {

    @Resource
    private NcrDocumentService ncrDocumentService;

    @PostMapping("/create")
    @Operation(summary = "创建 NCR 不合格品报告")
    @PreAuthorize("@ss.hasPermission('qms:ncr:create')")
    public CommonResult<Long> createNcrDocument(@Valid @RequestBody NcrDocumentSaveReqVO createReqVO) {
        return success(ncrDocumentService.createNcrDocument(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新 NCR 不合格品报告")
    @PreAuthorize("@ss.hasPermission('qms:ncr:update')")
    public CommonResult<Boolean> updateNcrDocument(@Valid @RequestBody NcrDocumentSaveReqVO updateReqVO) {
        ncrDocumentService.updateNcrDocument(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除 NCR 不合格品报告")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('qms:ncr:delete')")
    public CommonResult<Boolean> deleteNcrDocument(@RequestParam("id") Long id) {
        ncrDocumentService.deleteNcrDocument(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得 NCR 不合格品报告")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:ncr:query')")
    public CommonResult<NcrDocumentRespVO> getNcrDocument(@RequestParam("id") Long id) {
        NcrDocumentDO ncrDocument = ncrDocumentService.getNcrDocument(id);
        return success(BeanUtils.toBean(ncrDocument, NcrDocumentRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得 NCR 不合格品报告分页")
    @PreAuthorize("@ss.hasPermission('qms:ncr:query')")
    public CommonResult<PageResult<NcrDocumentRespVO>> getNcrDocumentPage(@Valid NcrDocumentPageReqVO pageReqVO) {
        PageResult<NcrDocumentDO> pageResult = ncrDocumentService.getNcrDocumentPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, NcrDocumentRespVO.class));
    }

    @PutMapping("/submit-mrb")
    @Operation(summary = "提交 MRB 评审", description = "仅在状态为「待处理」时允许，提交后流转为「MRB 评审中」")
    @Parameter(name = "ncrId", description = "NCR 编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:ncr:disposition')")
    public CommonResult<Boolean> submitForMrb(@RequestParam("ncrId") Long ncrId) {
        ncrDocumentService.submitForMrb(ncrId);
        return success(true);
    }

    @PutMapping("/record-disposition")
    @Operation(summary = "记录处置决议", description = "仅在状态为「MRB 评审中」时允许，记录 MRB 决议并流转为「已处置」")
    @PreAuthorize("@ss.hasPermission('qms:ncr:disposition')")
    public CommonResult<Boolean> recordDisposition(@Valid @RequestBody NcrDispositionReqVO reqVO) {
        ncrDocumentService.recordDisposition(reqVO);
        return success(true);
    }

    @PutMapping("/close")
    @Operation(summary = "关闭 NCR 报告", description = "仅在状态为「已处置」时允许，关闭后流转为「已关闭」")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:ncr:close')")
    public CommonResult<Boolean> closeNcrDocument(@RequestParam("id") Long id) {
        ncrDocumentService.closeNcrDocument(id);
        return success(true);
    }

    @GetMapping("/mrb-record/list")
    @Operation(summary = "获得 NCR 关联的 MRB 评审记录列表")
    @Parameter(name = "ncrId", description = "NCR 编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:ncr:query')")
    public CommonResult<List<NcrMrbRecordRespVO>> getMrbRecordList(@RequestParam("ncrId") Long ncrId) {
        List<NcrMrbRecordDO> list = ncrDocumentService.getMrbRecordListByNcrId(ncrId);
        return success(BeanUtils.toBean(list, NcrMrbRecordRespVO.class));
    }

}
