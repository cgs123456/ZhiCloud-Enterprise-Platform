package cn.iocoder.yudao.module.hr.controller.admin.performance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.hr.controller.admin.performance.vo.HrPerformanceDeptRankingRespVO;
import cn.iocoder.yudao.module.hr.controller.admin.performance.vo.HrPerformancePageReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.performance.vo.HrPerformanceRespVO;
import cn.iocoder.yudao.module.hr.controller.admin.performance.vo.HrPerformanceSaveReqVO;
import cn.iocoder.yudao.module.hr.dal.dataobject.performance.HrPerformanceDO;
import cn.iocoder.yudao.module.hr.service.performance.HrPerformanceService;
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

@Tag(name = "管理后台 - HR 绩效记录")
@RestController
@RequestMapping("/hr/performance")
@Validated
public class HrPerformanceController {

    @Resource
    private HrPerformanceService performanceService;

    @PostMapping("/create")
    @Operation(summary = "创建绩效记录")
    @PreAuthorize("@ss.hasPermission('hr:performance:create')")
    public CommonResult<Long> createPerformance(@Valid @RequestBody HrPerformanceSaveReqVO createReqVO) {
        return success(performanceService.createPerformance(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新绩效记录")
    @PreAuthorize("@ss.hasPermission('hr:performance:update')")
    public CommonResult<Boolean> updatePerformance(@Valid @RequestBody HrPerformanceSaveReqVO updateReqVO) {
        performanceService.updatePerformance(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除绩效记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:performance:delete')")
    public CommonResult<Boolean> deletePerformance(@RequestParam("id") Long id) {
        performanceService.deletePerformance(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得绩效记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:performance:query')")
    public CommonResult<HrPerformanceRespVO> getPerformance(@RequestParam("id") Long id) {
        HrPerformanceDO performance = performanceService.getPerformance(id);
        return success(BeanUtils.toBean(performance, HrPerformanceRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得绩效记录分页")
    @PreAuthorize("@ss.hasPermission('hr:performance:query')")
    public CommonResult<PageResult<HrPerformanceRespVO>> getPerformancePage(@Valid HrPerformancePageReqVO pageReqVO) {
        PageResult<HrPerformanceDO> pageResult = performanceService.getPerformancePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HrPerformanceRespVO.class));
    }

    @GetMapping("/list-by-period")
    @Operation(summary = "按周期查询绩效记录")
    @Parameter(name = "period", description = "考核周期", required = true, example = "2024Q1")
    @PreAuthorize("@ss.hasPermission('hr:performance:query')")
    public CommonResult<List<HrPerformanceRespVO>> getPerformanceListByPeriod(@RequestParam("period") String period) {
        List<HrPerformanceDO> list = performanceService.getPerformanceListByPeriod(period);
        return success(BeanUtils.toBean(list, HrPerformanceRespVO.class));
    }

    @GetMapping("/dept-ranking")
    @Operation(summary = "获得部门绩效排名")
    @Parameter(name = "deptId", description = "部门编号", required = true, example = "2048")
    @Parameter(name = "period", description = "考核周期", required = true, example = "2024Q1")
    @PreAuthorize("@ss.hasPermission('hr:performance:query')")
    public CommonResult<List<HrPerformanceDeptRankingRespVO>> getDepartmentRanking(
            @RequestParam("deptId") Long deptId,
            @RequestParam("period") String period) {
        return success(performanceService.getDepartmentRanking(deptId, period));
    }

}