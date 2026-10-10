package cn.zhicloud.module.fms.controller.admin.config;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.module.fms.controller.admin.config.vo.financeparameter.FmsFinanceParameterRespVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.financeparameter.FmsFinanceParameterSaveReqVO;
import cn.zhicloud.module.fms.service.config.FmsFinanceParameterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - FMS 账套参数")
@RestController
@RequestMapping("/fms/config/finance-parameter")
@Validated
public class FmsFinanceParameterController {

    @Resource
    private FmsFinanceParameterService financeParameterService;

    @GetMapping("/get")
    @Operation(summary = "获得账套参数")
    @Parameter(name = "accountSetId", description = "账套编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('fms:config:finance-parameter:update')")
    public CommonResult<FmsFinanceParameterRespVO> getFinanceParameter(@RequestParam("accountSetId") Long accountSetId) {
        return success(financeParameterService.getFinanceParameter(accountSetId));
    }

    @PutMapping("/update")
    @Operation(summary = "更新账套参数")
    @PreAuthorize("@ss.hasPermission('fms:config:finance-parameter:update')")
    public CommonResult<Boolean> updateFinanceParameter(@Valid @RequestBody FmsFinanceParameterSaveReqVO reqVO) {
        financeParameterService.updateFinanceParameter(reqVO);
        return success(true);
    }

}
