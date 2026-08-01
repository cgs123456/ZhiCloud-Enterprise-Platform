package cn.iocoder.yudao.module.mes.controller.admin.dv.tp;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.dv.tp.vo.MesDvTpKpiCalculateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.dv.tp.vo.MesDvTpKpiRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.tp.MesDvTpKpiDO;
import cn.iocoder.yudao.module.mes.service.dv.tp.MesDvTpKpiService;
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

@Tag(name = "管理后台 - MES TPM KPI 指标")
@RestController
@RequestMapping("/mes/dv/tp-kpi")
@Validated
public class MesDvTpKpiController {

    @Resource
    private MesDvTpKpiService tpKpiService;

    @PostMapping("/calculate")
    @Operation(summary = "计算 TPM KPI")
    @PreAuthorize("@ss.hasPermission('mes:dv-tp-kpi:calculate')")
    public CommonResult<MesDvTpKpiRespVO> calculateKpi(@Valid @RequestBody MesDvTpKpiCalculateReqVO reqVO) {
        MesDvTpKpiDO kpi = tpKpiService.calculateKpi(reqVO.getEquipmentId(), reqVO.getPeriod());
        return success(BeanUtils.toBean(kpi, MesDvTpKpiRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得 TPM KPI 指标")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('mes:dv-tp-kpi:query')")
    public CommonResult<MesDvTpKpiRespVO> getTpKpi(@RequestParam("id") Long id) {
        MesDvTpKpiDO kpi = tpKpiService.getTpKpi(id);
        return success(BeanUtils.toBean(kpi, MesDvTpKpiRespVO.class));
    }

    @GetMapping("/history")
    @Operation(summary = "查询设备 KPI 历史")
    @PreAuthorize("@ss.hasPermission('mes:dv-tp-kpi:query')")
    public CommonResult<List<MesDvTpKpiRespVO>> getEquipmentKpiHistory(
            @RequestParam("equipmentId") Long equipmentId,
            @RequestParam("periods") List<String> periods) {
        List<MesDvTpKpiDO> list = tpKpiService.getEquipmentKpiHistory(equipmentId, periods);
        return success(BeanUtils.toBean(list, MesDvTpKpiRespVO.class));
    }

}