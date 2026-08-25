package cn.zhicloud.module.hr.controller.admin.contract;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.hr.controller.admin.contract.vo.HrContractPageReqVO;
import cn.zhicloud.module.hr.controller.admin.contract.vo.HrContractRenewReqVO;
import cn.zhicloud.module.hr.controller.admin.contract.vo.HrContractRespVO;
import cn.zhicloud.module.hr.controller.admin.contract.vo.HrContractSaveReqVO;
import cn.zhicloud.module.hr.dal.dataobject.contract.HrContractDO;
import cn.zhicloud.module.hr.service.contract.HrContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - HR 合同管理")
@RestController
@RequestMapping("/hr/contract")
@Validated
public class HrContractController {

    @Resource
    private HrContractService contractService;

    @PostMapping("/create")
    @Operation(summary = "创建合同")
    @PreAuthorize("@ss.hasPermission('hr:contract:create')")
    public CommonResult<Long> createContract(@Valid @RequestBody HrContractSaveReqVO createReqVO) {
        return success(contractService.createContract(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新合同")
    @PreAuthorize("@ss.hasPermission('hr:contract:update')")
    public CommonResult<Boolean> updateContract(@Valid @RequestBody HrContractSaveReqVO updateReqVO) {
        contractService.updateContract(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除合同")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:contract:delete')")
    public CommonResult<Boolean> deleteContract(@RequestParam("id") Long id) {
        contractService.deleteContract(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得合同")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:contract:query')")
    public CommonResult<HrContractRespVO> getContract(@RequestParam("id") Long id) {
        HrContractDO contract = contractService.getContract(id);
        return success(BeanUtils.toBean(contract, HrContractRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得合同分页")
    @PreAuthorize("@ss.hasPermission('hr:contract:query')")
    public CommonResult<PageResult<HrContractRespVO>> getContractPage(@Valid HrContractPageReqVO pageReqVO) {
        PageResult<HrContractDO> pageResult = contractService.getContractPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HrContractRespVO.class));
    }

    @PutMapping("/renew")
    @Operation(summary = "合同续签")
    @PreAuthorize("@ss.hasPermission('hr:contract:update')")
    public CommonResult<Long> renewContract(@Valid @RequestBody HrContractRenewReqVO reqVO) {
        return success(contractService.renewContract(reqVO));
    }

    @PutMapping("/terminate")
    @Operation(summary = "合同终止")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:contract:update')")
    public CommonResult<Boolean> terminateContract(@RequestParam("id") Long id) {
        contractService.terminateContract(id);
        return success(true);
    }

    @GetMapping("/expiring")
    @Operation(summary = "获得即将到期合同列表")
    @Parameter(name = "days", description = "天数(默认 30)", example = "30")
    @PreAuthorize("@ss.hasPermission('hr:contract:query')")
    public CommonResult<List<HrContractRespVO>> getExpiringContracts(@RequestParam(value = "days", defaultValue = "30") int days) {
        List<HrContractDO> list = contractService.getExpiringContracts(days);
        return success(BeanUtils.toBean(list, HrContractRespVO.class));
    }

}