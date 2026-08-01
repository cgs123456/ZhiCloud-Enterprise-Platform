package cn.iocoder.yudao.module.tms.controller.admin.freight;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.tms.controller.admin.freight.vo.*;
import cn.iocoder.yudao.module.tms.dal.dataobject.freight.TmsFreightDO;
import cn.iocoder.yudao.module.tms.service.freight.TmsFreightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - TMS 运费结算")
@RestController
@RequestMapping("/tms/freight")
@Validated
public class TmsFreightController {

    @Resource
    private TmsFreightService freightService;

    @PostMapping("/create")
    @Operation(summary = "创建运费结算单")
    @PreAuthorize("@ss.hasPermission('tms:freight:create')")
    public CommonResult<Long> createFreight(@Valid @RequestBody TmsFreightSaveReqVO createReqVO) {
        return success(freightService.createFreight(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新运费结算单")
    @PreAuthorize("@ss.hasPermission('tms:freight:update')")
    public CommonResult<Boolean> updateFreight(@Valid @RequestBody TmsFreightSaveReqVO updateReqVO) {
        freightService.updateFreight(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除运费结算单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('tms:freight:delete')")
    public CommonResult<Boolean> deleteFreight(@RequestParam("id") Long id) {
        freightService.deleteFreight(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得运费结算单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('tms:freight:query')")
    public CommonResult<TmsFreightRespVO> getFreight(@RequestParam("id") Long id) {
        TmsFreightDO freight = freightService.getFreight(id);
        return success(BeanUtils.toBean(freight, TmsFreightRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得运费结算单分页")
    @PreAuthorize("@ss.hasPermission('tms:freight:query')")
    public CommonResult<PageResult<TmsFreightRespVO>> getFreightPage(@Valid TmsFreightPageReqVO pageReqVO) {
        PageResult<TmsFreightDO> pageResult = freightService.getFreightPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, TmsFreightRespVO.class));
    }

    @PostMapping("/calculate")
    @Operation(summary = "根据运单信息自动计算运费")
    @PreAuthorize("@ss.hasPermission('tms:freight:query')")
    public CommonResult<TmsFreightCalculateRespVO> calculateFreight(
            @Valid @RequestBody TmsFreightCalculateReqVO calculateReqVO) {
        return success(freightService.calculateFreight(calculateReqVO));
    }

    @PostMapping("/audit")
    @Operation(summary = "审核运费结算单")
    @Parameter(name = "id", description = "编号", required = true)
    @Parameter(name = "pass", description = "是否通过", required = true)
    @PreAuthorize("@ss.hasPermission('tms:freight:audit')")
    public CommonResult<Boolean> auditFreight(
            @RequestParam("id") Long id,
            @RequestParam("pass") Boolean pass,
            @RequestParam(value = "reason", required = false) String reason) {
        freightService.auditFreight(id, pass, reason);
        return success(true);
    }

    @PostMapping("/settle")
    @Operation(summary = "结算运费结算单（审核通过后执行结算）")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('tms:freight:settle')")
    public CommonResult<Boolean> settleFreight(@RequestParam("id") Long id) {
        freightService.settleFreight(id);
        return success(true);
    }

}
