package cn.zhicloud.module.fms.controller.admin.config;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.module.fms.controller.admin.config.vo.accountuser.FmsAccountUserRespVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.accountuser.FmsAccountUserUpdateReqVO;
import cn.zhicloud.module.fms.service.config.FmsAccountUserService;
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
import static cn.zhicloud.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - FMS 账套成员")
@RestController
@RequestMapping("/fms/config/account-user")
@Validated
public class FmsAccountUserController {

    @Resource
    private FmsAccountUserService accountUserService;

    @GetMapping("/list")
    @Operation(summary = "获得账套成员列表")
    @Parameter(name = "accountSetId", description = "账套编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('fms:config:account-set:authorize')")
    public CommonResult<List<FmsAccountUserRespVO>> getAccountUserList(@RequestParam("accountSetId") Long accountSetId) {
        return success(accountUserService.getAccountUserList(accountSetId));
    }

    @PutMapping("/update")
    @Operation(summary = "修改账套成员")
    @PreAuthorize("@ss.hasPermission('fms:config:account-set:authorize')")
    public CommonResult<Boolean> updateAccountUserList(@Valid @RequestBody FmsAccountUserUpdateReqVO reqVO) {
        accountUserService.updateAccountUserList(reqVO);
        return success(true);
    }

    @PutMapping("/update-default-status")
    @Operation(summary = "设置默认账套")
    @Parameter(name = "accountSetId", description = "账套编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('fms:config:account-set:query')")
    public CommonResult<Boolean> updateDefaultStatus(@RequestParam("accountSetId") Long accountSetId) {
        accountUserService.updateDefaultStatus(getLoginUserId(), accountSetId);
        return success(true);
    }

}
