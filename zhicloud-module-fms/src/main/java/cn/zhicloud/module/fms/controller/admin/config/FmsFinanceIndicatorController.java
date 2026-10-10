package cn.zhicloud.module.fms.controller.admin.config;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.fms.controller.admin.config.vo.financeindicator.FmsFinanceIndicatorRespVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.financeindicator.FmsFinanceIndicatorSaveReqVO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsFinanceIndicatorDO;
import cn.zhicloud.module.fms.service.config.FmsFinanceIndicatorService;
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

@Tag(name = "管理后台 - FMS 财务指标")
@RestController
@RequestMapping("/fms/config/finance-indicator")
@Validated
public class FmsFinanceIndicatorController {

    @Resource
    private FmsFinanceIndicatorService financeIndicatorService;

    @GetMapping("/list")
    @Operation(summary = "获得财务指标列表")
    @Parameter(name = "accountSetId", description = "账套编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('fms:config:finance-indicator:query')")
    public CommonResult<List<FmsFinanceIndicatorRespVO>> getFinanceIndicatorList(@RequestParam("accountSetId") Long accountSetId) {
        List<FmsFinanceIndicatorDO> list = financeIndicatorService.getFinanceIndicatorList(accountSetId);
        return success(BeanUtils.toBean(list, FmsFinanceIndicatorRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得财务指标")
    @Parameter(name = "accountSetId", description = "账套编号", required = true, example = "1")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('fms:config:finance-indicator:query')")
    public CommonResult<FmsFinanceIndicatorRespVO> getFinanceIndicator(@RequestParam("accountSetId") Long accountSetId,
                                             @RequestParam("id") Long id) {
        FmsFinanceIndicatorDO entity = financeIndicatorService.getFinanceIndicator(accountSetId, id);
        return success(BeanUtils.toBean(entity, FmsFinanceIndicatorRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "创建财务指标")
    @PreAuthorize("@ss.hasPermission('fms:config:finance-indicator:create')")
    public CommonResult<Long> createFinanceIndicator(@Valid @RequestBody FmsFinanceIndicatorSaveReqVO createReqVO) {
        return success(financeIndicatorService.createFinanceIndicator(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新财务指标")
    @PreAuthorize("@ss.hasPermission('fms:config:finance-indicator:update')")
    public CommonResult<Boolean> updateFinanceIndicator(@Valid @RequestBody FmsFinanceIndicatorSaveReqVO updateReqVO) {
        financeIndicatorService.updateFinanceIndicator(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除财务指标")
    @Parameter(name = "accountSetId", description = "账套编号", required = true, example = "1")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('fms:config:finance-indicator:delete')")
    public CommonResult<Boolean> deleteFinanceIndicator(@RequestParam("accountSetId") Long accountSetId,
                                            @RequestParam("id") Long id) {
        financeIndicatorService.deleteFinanceIndicator(accountSetId, id);
        return success(true);
    }

}
