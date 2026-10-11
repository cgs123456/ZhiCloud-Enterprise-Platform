package cn.zhicloud.module.hr.controller.admin.hrm.home;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.module.hr.controller.admin.hrm.home.vo.*;
import cn.zhicloud.module.hr.service.hrm.home.HrmHomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * HRM 工作台 Controller（HR 首页 + 团队工作台统计与日历）
 *
 * @author zhicloud
 */
@Tag(name = "管理后台 - HRM 工作台")
@RestController
@RequestMapping("/hrm/home")
@Validated
public class HrmHomeController {

    @Resource
    private HrmHomeService homeService;

    @GetMapping("/hr-statistics-summary")
    @Operation(summary = "获得 HR 首页统计汇总")
    @PreAuthorize("@ss.hasPermission('hrm:home:query')")
    public CommonResult<HrmHrHomeStatisticsRespVO> getHrStatisticsSummary() {
        return success(homeService.getHrStatisticsSummary());
    }

    @GetMapping("/hr-calendar")
    @Operation(summary = "获得 HR 工作台日历")
    @Parameter(name = "startDate", description = "开始日期（yyyy-MM-dd）", required = true)
    @Parameter(name = "endDate", description = "结束日期（yyyy-MM-dd）", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:home:query')")
    public CommonResult<List<HrmHomeCalendarItemVO>> getHrCalendar(@RequestParam("startDate") String startDate,
                                                                   @RequestParam("endDate") String endDate) {
        return success(homeService.getHrCalendar(startDate, endDate));
    }

    @GetMapping("/team-statistics-summary")
    @Operation(summary = "获得团队工作台统计汇总")
    @PreAuthorize("@ss.hasPermission('hrm:home:query')")
    public CommonResult<HrmTeamHomeStatisticsRespVO> getTeamStatisticsSummary() {
        return success(homeService.getTeamStatisticsSummary());
    }

    @GetMapping("/team-calendar")
    @Operation(summary = "获得团队工作台日历")
    @Parameter(name = "startDate", description = "开始日期（yyyy-MM-dd）", required = true)
    @Parameter(name = "endDate", description = "结束日期（yyyy-MM-dd）", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:home:query')")
    public CommonResult<List<HrmHomeCalendarItemVO>> getTeamCalendar(@RequestParam("startDate") String startDate,
                                                                     @RequestParam("endDate") String endDate) {
        return success(homeService.getTeamCalendar(startDate, endDate));
    }

}
