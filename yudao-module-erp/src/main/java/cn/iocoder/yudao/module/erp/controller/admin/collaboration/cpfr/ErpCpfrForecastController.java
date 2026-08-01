package cn.iocoder.yudao.module.erp.controller.admin.collaboration.cpfr;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.collaboration.cpfr.vo.ErpCpfrForecastPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.collaboration.cpfr.vo.ErpCpfrForecastRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.collaboration.cpfr.vo.ErpCpfrForecastSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.collaboration.cpfr.ErpCpfrForecastDO;
import cn.iocoder.yudao.module.erp.service.collaboration.cpfr.ErpCpfrForecastService;
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

@Tag(name = "管理后台 - ERP CPFR 联合计划预测补货")
@RestController
@RequestMapping("/erp/cpfr-forecast")
@Validated
public class ErpCpfrForecastController {

    @Resource
    private ErpCpfrForecastService cpfrForecastService;

    @PostMapping("/create")
    @Operation(summary = "创建预测")
    @PreAuthorize("@ss.hasPermission('erp:cpfr-forecast:create')")
    public CommonResult<Long> createForecast(@Valid @RequestBody ErpCpfrForecastSaveReqVO createReqVO) {
        return success(cpfrForecastService.createForecast(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新预测")
    @PreAuthorize("@ss.hasPermission('erp:cpfr-forecast:update')")
    public CommonResult<Boolean> updateForecast(@Valid @RequestBody ErpCpfrForecastSaveReqVO updateReqVO) {
        cpfrForecastService.updateForecast(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除预测")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:cpfr-forecast:delete')")
    public CommonResult<Boolean> deleteForecast(@RequestParam("id") Long id) {
        cpfrForecastService.deleteForecast(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得预测")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:cpfr-forecast:query')")
    public CommonResult<ErpCpfrForecastRespVO> getForecast(@RequestParam("id") Long id) {
        ErpCpfrForecastDO forecast = cpfrForecastService.getForecast(id);
        return success(BeanUtils.toBean(forecast, ErpCpfrForecastRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得预测分页")
    @PreAuthorize("@ss.hasPermission('erp:cpfr-forecast:query')")
    public CommonResult<PageResult<ErpCpfrForecastRespVO>> getForecastPage(
            @Valid ErpCpfrForecastPageReqVO pageReqVO) {
        PageResult<ErpCpfrForecastDO> pageResult = cpfrForecastService.getForecastPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpCpfrForecastRespVO.class));
    }

    @PostMapping("/calculate-deviation")
    @Operation(summary = "计算偏差率")
    @Parameter(name = "id", description = "预测编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:cpfr-forecast:update')")
    public CommonResult<Boolean> calculateDeviation(@RequestParam("id") Long id) {
        cpfrForecastService.calculateDeviation(id);
        return success(true);
    }

    @PostMapping("/check-exception")
    @Operation(summary = "检查异常，扫描预测偏差并创建异常记录")
    @PreAuthorize("@ss.hasPermission('erp:cpfr-forecast:query')")
    public CommonResult<List<Long>> checkException() {
        return success(cpfrForecastService.checkException());
    }

}
