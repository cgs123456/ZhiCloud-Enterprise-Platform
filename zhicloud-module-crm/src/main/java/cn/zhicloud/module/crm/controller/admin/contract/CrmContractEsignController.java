package cn.zhicloud.module.crm.controller.admin.contract;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.module.crm.controller.admin.contract.vo.esign.CrmContractEsignRespVO;
import cn.zhicloud.module.crm.controller.admin.contract.vo.esign.CrmEsignCallbackReqVO;
import cn.zhicloud.module.crm.service.contract.esign.CrmContractEsignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - CRM 合同电子签")
@RestController
@RequestMapping("/crm/contract-esign")
@Validated
public class CrmContractEsignController {

    @Resource
    private CrmContractEsignService contractEsignService;

    @PostMapping("/init")
    @Operation(summary = "发起电子签")
    @Parameter(name = "contractId", description = "合同编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('crm:contract:update')")
    public CommonResult<String> initEsign(@RequestParam("contractId") Long contractId) {
        return success(contractEsignService.initEsign(contractId));
    }

    @GetMapping("/status")
    @Operation(summary = "查询签署状态")
    @Parameter(name = "esignTaskId", description = "电子签任务 ID", required = true, example = "ESIGN1690000000000")
    @PreAuthorize("@ss.hasPermission('crm:contract:query')")
    public CommonResult<CrmContractEsignRespVO> getEsignStatus(@RequestParam("esignTaskId") String esignTaskId) {
        return success(contractEsignService.getEsignStatus(esignTaskId));
    }

    @GetMapping("/download")
    @Operation(summary = "下载已签署合同")
    @Parameter(name = "contractId", description = "合同编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('crm:contract:query')")
    public CommonResult<CrmContractEsignRespVO> downloadSignedContract(@RequestParam("contractId") Long contractId) {
        return success(contractEsignService.downloadSignedContract(contractId));
    }

    @PostMapping("/callback")
    @Operation(summary = "电子签回调")
    @PreAuthorize("@ss.hasPermission('crm:contract:update')")
    public CommonResult<Boolean> handleEsignCallback(@Valid @RequestBody CrmEsignCallbackReqVO req) {
        contractEsignService.handleEsignCallback(req);
        return success(true);
    }

}
