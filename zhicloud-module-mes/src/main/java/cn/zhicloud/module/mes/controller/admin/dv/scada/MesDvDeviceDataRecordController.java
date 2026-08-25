package cn.zhicloud.module.mes.controller.admin.dv.scada;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.mes.controller.admin.dv.scada.vo.MesDvDeviceDataRecordPageReqVO;
import cn.zhicloud.module.mes.controller.admin.dv.scada.vo.MesDvDeviceDataRecordRespVO;
import cn.zhicloud.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.zhicloud.module.mes.dal.dataobject.dv.scada.MesDvDeviceDataRecordDO;
import cn.zhicloud.module.mes.service.dv.machinery.MesDvMachineryService;
import cn.zhicloud.module.mes.service.dv.scada.MesDvDeviceDataCollectorService;
import cn.zhicloud.module.mes.service.dv.scada.MesDvDeviceDataRecordService;
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

import static cn.zhicloud.framework.common.pojo.CommonResult.success;
import static cn.zhicloud.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Tag(name = "管理后台 - MES 设备数据采集记录")
@RestController
@RequestMapping("/mes/dv-device-data")
@Validated
public class MesDvDeviceDataRecordController {

    @Resource
    private MesDvDeviceDataRecordService dataRecordService;
    @Resource
    private MesDvDeviceDataCollectorService collectorService;
    @Resource
    private MesDvMachineryService machineryService;

    @GetMapping("/page")
    @Operation(summary = "获得设备数据采集记录分页")
    @PreAuthorize("@ss.hasPermission('mes:dv-device-data:query')")
    public CommonResult<PageResult<MesDvDeviceDataRecordRespVO>> getDataRecordPage(
            @Valid MesDvDeviceDataRecordPageReqVO pageReqVO) {
        PageResult<MesDvDeviceDataRecordDO> pageResult = dataRecordService.getDataRecordPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, MesDvDeviceDataRecordRespVO.class));
    }

    @GetMapping("/list-by-machinery")
    @Operation(summary = "按设备与时间范围查询采集记录")
    @PreAuthorize("@ss.hasPermission('mes:dv-device-data:query')")
    public CommonResult<List<MesDvDeviceDataRecordRespVO>> listByMachinery(
            @RequestParam("machineryId") Long machineryId,
            @RequestParam("start") @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND) LocalDateTime start,
            @RequestParam("end") @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND) LocalDateTime end) {
        List<MesDvDeviceDataRecordDO> list = dataRecordService.getDataRecordListByTimeRange(machineryId, start, end);
        return success(BeanUtils.toBean(list, MesDvDeviceDataRecordRespVO.class));
    }

    @GetMapping("/latest")
    @Operation(summary = "获得设备最新一帧采集数据")
    @Parameter(name = "machineryId", description = "MES 设备编号", required = true, example = "100")
    @PreAuthorize("@ss.hasPermission('mes:dv-device-data:query')")
    public CommonResult<MesDvDeviceDataRecordRespVO> getLatest(@RequestParam("machineryId") Long machineryId) {
        MesDvDeviceDataRecordDO record = dataRecordService.getLatestDataRecord(machineryId);
        if (record == null) {
            return success(null);
        }
        MesDvDeviceDataRecordRespVO respVO = BeanUtils.toBean(record, MesDvDeviceDataRecordRespVO.class);
        MesDvMachineryDO machinery = machineryService.getMachinery(machineryId);
        if (machinery != null) {
            respVO.setMachineryName(machinery.getName());
        }
        return success(respVO);
    }

    @GetMapping("/get")
    @Operation(summary = "获得设备数据采集记录详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('mes:dv-device-data:query')")
    public CommonResult<MesDvDeviceDataRecordRespVO> getDataRecord(@RequestParam("id") Long id) {
        MesDvDeviceDataRecordDO record = dataRecordService.getDataRecord(id);
        if (record == null) {
            return success(null);
        }
        MesDvDeviceDataRecordRespVO respVO = BeanUtils.toBean(record, MesDvDeviceDataRecordRespVO.class);
        MesDvMachineryDO machinery = machineryService.getMachinery(record.getMachineryId());
        if (machinery != null) {
            respVO.setMachineryName(machinery.getName());
        }
        return success(respVO);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除设备数据采集记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:dv-device-data:delete')")
    public CommonResult<Boolean> deleteDataRecord(@RequestParam("id") Long id) {
        dataRecordService.deleteDataRecord(id);
        return success(true);
    }

    @PostMapping("/collect")
    @Operation(summary = "手动触发一次设备数据采集")
    @Parameter(name = "machineryId", description = "MES 设备编号", required = true, example = "100")
    @PreAuthorize("@ss.hasPermission('mes:dv-device-data:collect')")
    public CommonResult<Integer> collect(@RequestParam("machineryId") Long machineryId) {
        return success(collectorService.collectDeviceData(machineryId).size());
    }

    @PostMapping("/start")
    @Operation(summary = "开启自动采集")
    @PreAuthorize("@ss.hasPermission('mes:dv-device-data:collect')")
    public CommonResult<Boolean> startAuto() {
        collectorService.startAutoCollection();
        return success(true);
    }

    @PostMapping("/stop")
    @Operation(summary = "停止自动采集")
    @PreAuthorize("@ss.hasPermission('mes:dv-device-data:collect')")
    public CommonResult<Boolean> stopAuto() {
        collectorService.stopAutoCollection();
        return success(true);
    }

}
