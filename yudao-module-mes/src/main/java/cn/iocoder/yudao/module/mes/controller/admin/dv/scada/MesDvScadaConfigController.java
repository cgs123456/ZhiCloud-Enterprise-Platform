package cn.iocoder.yudao.module.mes.controller.admin.dv.scada;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.dv.scada.vo.MesDvScadaConfigPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.dv.scada.vo.MesDvScadaConfigRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.dv.scada.vo.MesDvScadaConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.scada.MesDvScadaConfigDO;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryService;
import cn.iocoder.yudao.module.mes.service.dv.scada.MesDvScadaConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES SCADA 设备配置")
@RestController
@RequestMapping("/mes/dv-scada-config")
@Validated
public class MesDvScadaConfigController {

    @Resource
    private MesDvScadaConfigService scadaConfigService;
    @Resource
    private MesDvMachineryService machineryService;

    @PostMapping("/create")
    @Operation(summary = "创建 SCADA 设备配置")
    @PreAuthorize("@ss.hasPermission('mes:dv-scada-config:create')")
    public CommonResult<Long> createScadaConfig(@Valid @RequestBody MesDvScadaConfigSaveReqVO createReqVO) {
        return success(scadaConfigService.createScadaConfig(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新 SCADA 设备配置")
    @PreAuthorize("@ss.hasPermission('mes:dv-scada-config:update')")
    public CommonResult<Boolean> updateScadaConfig(@Valid @RequestBody MesDvScadaConfigSaveReqVO updateReqVO) {
        scadaConfigService.updateScadaConfig(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除 SCADA 设备配置")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:dv-scada-config:delete')")
    public CommonResult<Boolean> deleteScadaConfig(@RequestParam("id") Long id) {
        scadaConfigService.deleteScadaConfig(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得 SCADA 设备配置")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('mes:dv-scada-config:query')")
    public CommonResult<MesDvScadaConfigRespVO> getScadaConfig(@RequestParam("id") Long id) {
        MesDvScadaConfigDO config = scadaConfigService.getScadaConfig(id);
        if (config == null) {
            return success(null);
        }
        MesDvScadaConfigRespVO respVO = BeanUtils.toBean(config, MesDvScadaConfigRespVO.class);
        MesDvMachineryDO machinery = machineryService.getMachinery(config.getMachineryId());
        if (machinery != null) {
            respVO.setMachineryName(machinery.getName());
        }
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得 SCADA 设备配置分页")
    @PreAuthorize("@ss.hasPermission('mes:dv-scada-config:query')")
    public CommonResult<PageResult<MesDvScadaConfigRespVO>> getScadaConfigPage(@Valid MesDvScadaConfigPageReqVO pageReqVO) {
        PageResult<MesDvScadaConfigDO> pageResult = scadaConfigService.getScadaConfigPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, MesDvScadaConfigRespVO.class));
    }

}
