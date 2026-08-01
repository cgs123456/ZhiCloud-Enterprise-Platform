package cn.iocoder.yudao.module.mes.controller.admin.pro.aps;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.aps.vo.MesProApsGenerateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.aps.vo.MesProApsPlanPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.aps.vo.MesProApsPlanRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.aps.vo.MesProApsRescheduleReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.aps.vo.MesProApsWorkstationLoadRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.aps.MesProApsPlanDO;
import cn.iocoder.yudao.module.mes.service.pro.aps.MesProApsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Tag(name = "管理后台 - MES APS 高级排产")
@RestController
@RequestMapping("/mes/pro/aps")
@Validated
public class MesProApsController {

    @Resource
    private MesProApsService apsService;

    @PostMapping("/generate")
    @Operation(summary = "生成排产")
    @PreAuthorize("@ss.hasPermission('mes:pro-aps:generate')")
    public CommonResult<List<MesProApsPlanRespVO>> generateSchedule(@Valid @RequestBody MesProApsGenerateReqVO reqVO) {
        List<MesProApsPlanDO> list = apsService.generateSchedule(reqVO);
        return success(BeanUtils.toBean(list, MesProApsPlanRespVO.class));
    }

    @GetMapping("/plan/list")
    @Operation(summary = "排产计划列表")
    @PreAuthorize("@ss.hasPermission('mes:pro-aps:query')")
    public CommonResult<PageResult<MesProApsPlanRespVO>> getApsPlanPage(@Valid MesProApsPlanPageReqVO pageReqVO) {
        PageResult<MesProApsPlanDO> pageResult = apsService.getApsPlanPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, MesProApsPlanRespVO.class));
    }

    @GetMapping("/plan/get")
    @Operation(summary = "获得排产计划")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('mes:pro-aps:query')")
    public CommonResult<MesProApsPlanRespVO> getApsPlan(@RequestParam("id") Long id) {
        MesProApsPlanDO plan = apsService.getApsPlan(id);
        return success(BeanUtils.toBean(plan, MesProApsPlanRespVO.class));
    }

    @PutMapping("/plan/reschedule")
    @Operation(summary = "重排产")
    @PreAuthorize("@ss.hasPermission('mes:pro-aps:update')")
    public CommonResult<Boolean> reschedule(@Valid @RequestBody MesProApsRescheduleReqVO reqVO) {
        apsService.reschedule(reqVO.getPlanId(), reqVO.getNewStartTime());
        return success(true);
    }

    @GetMapping("/workstation-load")
    @Operation(summary = "工位负荷")
    @PreAuthorize("@ss.hasPermission('mes:pro-aps:query')")
    public CommonResult<List<MesProApsWorkstationLoadRespVO>> getWorkstationLoad(
            @RequestParam("workstationId") Long workstationId,
            @RequestParam("startDate") @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND) LocalDateTime startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND) LocalDateTime endDate) {
        List<MesProApsPlanDO> list = apsService.getWorkstationLoad(workstationId, startDate, endDate);
        return success(BeanUtils.toBean(list, MesProApsWorkstationLoadRespVO.class));
    }

}
