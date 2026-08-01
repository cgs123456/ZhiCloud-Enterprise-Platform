package cn.iocoder.yudao.module.qms.controller.admin.instrument;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.instrument.vo.QmsInstrumentCalibrationPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.instrument.vo.QmsInstrumentCalibrationRespVO;
import cn.iocoder.yudao.module.qms.controller.admin.instrument.vo.QmsInstrumentCalibrationSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.instrument.QmsInstrumentCalibrationDO;
import cn.iocoder.yudao.module.qms.service.instrument.QmsInstrumentCalibrationService;
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

/**
 * QMS 计量器具校准记录 Controller
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - QMS 计量器具校准记录")
@RestController
@RequestMapping("/qms/instrument-calibration")
@Validated
public class QmsInstrumentCalibrationController {

    @Resource
    private QmsInstrumentCalibrationService calibrationService;

    @PostMapping("/record")
    @Operation(summary = "记录校准", description = "新增校准记录并自动更新器具的 last/next_calibration_date；报废/封存状态器具不允许记录")
    @PreAuthorize("@ss.hasPermission('qms:instrument-calibration:create')")
    public CommonResult<Long> recordCalibration(@Valid @RequestBody QmsInstrumentCalibrationSaveReqVO createReqVO) {
        return success(calibrationService.recordCalibration(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新校准记录")
    @PreAuthorize("@ss.hasPermission('qms:instrument-calibration:update')")
    public CommonResult<Boolean> updateCalibration(@Valid @RequestBody QmsInstrumentCalibrationSaveReqVO updateReqVO) {
        calibrationService.updateCalibration(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除校准记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('qms:instrument-calibration:delete')")
    public CommonResult<Boolean> deleteCalibration(@RequestParam("id") Long id) {
        calibrationService.deleteCalibration(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得校准记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:instrument-calibration:query')")
    public CommonResult<QmsInstrumentCalibrationRespVO> getCalibration(@RequestParam("id") Long id) {
        QmsInstrumentCalibrationDO calibration = calibrationService.getCalibration(id);
        return success(BeanUtils.toBean(calibration, QmsInstrumentCalibrationRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得校准记录分页")
    @PreAuthorize("@ss.hasPermission('qms:instrument-calibration:query')")
    public CommonResult<PageResult<QmsInstrumentCalibrationRespVO>> getCalibrationPage(
            @Valid QmsInstrumentCalibrationPageReqVO pageReqVO) {
        PageResult<QmsInstrumentCalibrationDO> pageResult = calibrationService.getCalibrationPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, QmsInstrumentCalibrationRespVO.class));
    }

    @GetMapping("/list-by-instrument")
    @Operation(summary = "获得指定器具的校准记录列表")
    @Parameter(name = "instrumentId", description = "器具 ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:instrument-calibration:query')")
    public CommonResult<List<QmsInstrumentCalibrationRespVO>> getCalibrationListByInstrumentId(
            @RequestParam("instrumentId") Long instrumentId) {
        List<QmsInstrumentCalibrationDO> list = calibrationService.getCalibrationListByInstrumentId(instrumentId);
        return success(BeanUtils.toBean(list, QmsInstrumentCalibrationRespVO.class));
    }

}