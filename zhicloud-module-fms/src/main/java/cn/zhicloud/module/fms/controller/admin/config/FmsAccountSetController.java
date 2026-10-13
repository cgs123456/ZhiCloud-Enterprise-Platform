package cn.zhicloud.module.fms.controller.admin.config;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.fms.controller.admin.config.vo.accountset.FmsAccountSetInitializeReqVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.accountset.FmsAccountSetRespVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.accountset.FmsAccountBookRespVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.accountset.FmsAccountSetSaveReqVO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsAccountSetDO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsAccountUserDO;
import cn.zhicloud.module.fms.service.config.FmsAccountSetService;
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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;
import static cn.zhicloud.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - FMS 账套")
@RestController
@RequestMapping("/fms/config/account-set")
@Validated
public class FmsAccountSetController {

    @Resource
    private FmsAccountSetService accountSetService;
    @Resource
    private FmsAccountUserService accountUserService;
    @GetMapping("/list")
    @Operation(summary = "获得当前用户的账套列表")
    public CommonResult<List<FmsAccountSetRespVO>> getAccountSetList() {
        Long userId = getLoginUserId();
        List<FmsAccountSetDO> list = accountSetService.getAccountSetList(userId);
        List<FmsAccountSetRespVO> respList = BeanUtils.toBean(list, FmsAccountSetRespVO.class);
        // 组装当前用户在各账套的 level / defaultStatus / founder
        Map<Long, FmsAccountUserDO> memberMap = list.stream()
                .collect(Collectors.toMap(FmsAccountSetDO::getId,
                        accountSet -> accountUserService.getAccountUser(accountSet.getId(), userId),
                        (a, b) -> a));
        respList.forEach(item -> {
            FmsAccountUserDO member = memberMap.get(item.getId());
            item.setLevel(member == null ? null : member.getLevel());
            item.setDefaultStatus(member != null && Boolean.TRUE.equals(member.getDefaultStatus()));
            item.setFounder(item.getCreateTime() != null && member != null
                    && String.valueOf(userId).equals(member.getCreator()));
        });
        return success(respList);
    }

    @GetMapping("/get")
    @Operation(summary = "获得账套")
    @Parameter(name = "id", description = "账套编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('fms:config:account-set:query')")
    public CommonResult<FmsAccountSetRespVO> getAccountSet(@RequestParam("id") Long id) {
        FmsAccountSetDO accountSet = accountSetService.getAccountSet(id);
        return success(BeanUtils.toBean(accountSet, FmsAccountSetRespVO.class));
    }

    @GetMapping("/account-books")
    @Operation(summary = "获得账套关联的 ERP 账簿列表",
            description = "V191 打通『账套 → 账簿 → 凭证』层级后，账套详情页据此展示本账套挂了哪些账簿；跨模块只读查询，经 erp.api 边界包")
    @Parameter(name = "id", description = "账套编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('fms:config:account-set:query')")
    public CommonResult<List<FmsAccountBookRespVO>> getAccountBookList(@RequestParam("id") Long id) {
        List<FmsAccountBookRespVO> list = accountSetService.getAccountBookList(id);
        return success(list);
    }

    @PostMapping("/create")
    @Operation(summary = "创建账套")
    @PreAuthorize("@ss.hasPermission('fms:config:account-set:create')")
    public CommonResult<Long> createAccountSet(@Valid @RequestBody FmsAccountSetSaveReqVO createReqVO) {
        return success(accountSetService.createAccountSet(getLoginUserId(), createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新账套")
    @PreAuthorize("@ss.hasPermission('fms:config:account-set:update')")
    public CommonResult<Boolean> updateAccountSet(@Valid @RequestBody FmsAccountSetSaveReqVO updateReqVO) {
        accountSetService.updateAccountSet(getLoginUserId(), updateReqVO);
        return success(true);
    }

    @PutMapping("/initialize")
    @Operation(summary = "初始化账套")
    @PreAuthorize("@ss.hasPermission('fms:config:account-set:initialize')")
    public CommonResult<Boolean> initializeAccountSet(@Valid @RequestBody FmsAccountSetInitializeReqVO reqVO) {
        accountSetService.initializeAccountSet(reqVO);
        return success(true);
    }

}
