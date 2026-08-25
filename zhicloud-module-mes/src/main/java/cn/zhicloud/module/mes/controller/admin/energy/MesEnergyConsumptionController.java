package cn.zhicloud.module.mes.controller.admin.energy;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.mes.controller.admin.energy.vo.MesEnergyConsumptionPageReqVO;
import cn.zhicloud.module.mes.controller.admin.energy.vo.MesEnergyConsumptionRespVO;
import cn.zhicloud.module.mes.controller.admin.energy.vo.MesEnergyConsumptionSaveReqVO;
import cn.zhicloud.module.mes.dal.dataobject.energy.MesEnergyConsumptionDO;
import cn.zhicloud.module.mes.service.energy.MesEnergyConsumptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * MES 能源消耗 Controller
 *
 * @author 智云
 */
@Tag(name = "管理后台 - MES 能源消耗")
@RestController
@RequestMapping("/mes/energy-consumption")
public class MesEnergyConsumptionController {

    @Resource
    private MesEnergyConsumptionService energyConsumptionService;

    @PostMapping("/create")
    @Operation(summary = "创建能源消耗记录")
    @PreAuthorize("@ss.hasPermission('mes:energy:create')")
    public CommonResult<Long> createEnergyConsumption(@Valid @RequestBody MesEnergyConsumptionSaveReqVO createReqVO) {
        return success(energyConsumptionService.createEnergyConsumption(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新能源消耗记录")
    @PreAuthorize("@ss.hasPermission('mes:energy:update')")
    public CommonResult<Boolean> updateEnergyConsumption(@Valid @RequestBody MesEnergyConsumptionSaveReqVO updateReqVO) {
        energyConsumptionService.updateEnergyConsumption(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除能源消耗记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:energy:delete')")
    public CommonResult<Boolean> deleteEnergyConsumption(@RequestParam("id") Long id) {
        energyConsumptionService.deleteEnergyConsumption(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取能源消耗记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:energy:query')")
    public CommonResult<MesEnergyConsumptionRespVO> getEnergyConsumption(@RequestParam("id") Long id) {
        MesEnergyConsumptionDO energyConsumption = energyConsumptionService.getEnergyConsumption(id);
        return success(BeanUtils.toBean(energyConsumption, MesEnergyConsumptionRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获取能源消耗记录分页")
    @PreAuthorize("@ss.hasPermission('mes:energy:query')")
    public CommonResult<PageResult<MesEnergyConsumptionRespVO>> getEnergyConsumptionPage(
            @Valid MesEnergyConsumptionPageReqVO pageReqVO) {
        PageResult<MesEnergyConsumptionDO> pageResult = energyConsumptionService.getEnergyConsumptionPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, MesEnergyConsumptionRespVO.class));
    }

    @GetMapping("/summary")
    @Operation(summary = "获取车间能源消耗汇总（按能源类型分组）")
    @PreAuthorize("@ss.hasPermission('mes:energy:query')")
    public CommonResult<Map<Integer, BigDecimal>> getEnergySummary(
            @RequestParam("workshopId") Long workshopId,
            @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return success(energyConsumptionService.getEnergySummaryByWorkshop(workshopId, startDate, endDate));
    }

    @GetMapping("/list")
    @Operation(summary = "获取车间能源消耗明细列表")
    @PreAuthorize("@ss.hasPermission('mes:energy:query')")
    public CommonResult<List<MesEnergyConsumptionRespVO>> getEnergyList(
            @RequestParam("workshopId") Long workshopId,
            @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        List<MesEnergyConsumptionDO> list = energyConsumptionService.getEnergyConsumptionList(workshopId, startDate, endDate);
        return success(BeanUtils.toBean(list, MesEnergyConsumptionRespVO.class));
    }

}
