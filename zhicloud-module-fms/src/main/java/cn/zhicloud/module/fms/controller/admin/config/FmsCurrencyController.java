package cn.zhicloud.module.fms.controller.admin.config;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.fms.controller.admin.config.vo.currency.FmsCurrencyRespVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.currency.FmsCurrencySaveReqVO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsCurrencyDO;
import cn.zhicloud.module.fms.service.config.FmsCurrencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - FMS 币别")
@RestController
@RequestMapping("/fms/config/currency")
@Validated
public class FmsCurrencyController {

    @Resource
    private FmsCurrencyService currencyService;

    @GetMapping("/list")
    @Operation(summary = "获得币别列表")
    @Parameter(name = "accountSetId", description = "账套编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('fms:config:currency:query')")
    public CommonResult<List<FmsCurrencyRespVO>> getCurrencyList(@RequestParam("accountSetId") Long accountSetId) {
        List<FmsCurrencyDO> list = currencyService.getCurrencyList(accountSetId);
        return success(BeanUtils.toBean(list, FmsCurrencyRespVO.class));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得币别精简列表")
    @Parameter(name = "accountSetId", description = "账套编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('fms:config:currency:query')")
    public CommonResult<List<FmsCurrencyRespVO>> getCurrencySimpleList(@RequestParam("accountSetId") Long accountSetId) {
        List<FmsCurrencyDO> list = currencyService.getCurrencyList(accountSetId);
        return success(BeanUtils.toBean(list, FmsCurrencyRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "创建币别")
    @PreAuthorize("@ss.hasPermission('fms:config:currency:create')")
    public CommonResult<Long> createCurrency(@Valid @RequestBody FmsCurrencySaveReqVO createReqVO) {
        return success(currencyService.createCurrency(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新币别")
    @PreAuthorize("@ss.hasPermission('fms:config:currency:update')")
    public CommonResult<Boolean> updateCurrency(@Valid @RequestBody FmsCurrencySaveReqVO updateReqVO) {
        currencyService.updateCurrency(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除币别")
    @Parameter(name = "accountSetId", description = "账套编号", required = true, example = "1")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('fms:config:currency:delete')")
    public CommonResult<Boolean> deleteCurrency(@RequestParam("accountSetId") Long accountSetId,
                                            @RequestParam("id") Long id) {
        currencyService.deleteCurrency(accountSetId, id);
        return success(true);
    }

}
