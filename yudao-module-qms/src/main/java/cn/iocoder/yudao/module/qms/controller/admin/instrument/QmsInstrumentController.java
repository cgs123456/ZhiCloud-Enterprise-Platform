package cn.iocoder.yudao.module.qms.controller.admin.instrument;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.instrument.vo.QmsInstrumentExpiringSoonRespVO;
import cn.iocoder.yudao.module.qms.controller.admin.instrument.vo.QmsInstrumentPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.instrument.vo.QmsInstrumentRespVO;
import cn.iocoder.yudao.module.qms.controller.admin.instrument.vo.QmsInstrumentSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.instrument.QmsInstrumentDO;
import cn.iocoder.yudao.module.qms.service.instrument.QmsInstrumentService;
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
 * QMS 计量器具台账 Controller
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - QMS 计量器具台账")
@RestController
@RequestMapping("/qms/instrument")
@Validated
public class QmsInstrumentController {

    @Resource
    private QmsInstrumentService instrumentService;

    @PostMapping("/create")
    @Operation(summary = "创建计量器具")
    @PreAuthorize("@ss.hasPermission('qms:instrument:create')")
    public CommonResult<Long> createInstrument(@Valid @RequestBody QmsInstrumentSaveReqVO createReqVO) {
        return success(instrumentService.createInstrument(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新计量器具")
    @PreAuthorize("@ss.hasPermission('qms:instrument:update')")
    public CommonResult<Boolean> updateInstrument(@Valid @RequestBody QmsInstrumentSaveReqVO updateReqVO) {
        instrumentService.updateInstrument(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除计量器具")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('qms:instrument:delete')")
    public CommonResult<Boolean> deleteInstrument(@RequestParam("id") Long id) {
        instrumentService.deleteInstrument(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得计量器具")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:instrument:query')")
    public CommonResult<QmsInstrumentRespVO> getInstrument(@RequestParam("id") Long id) {
        QmsInstrumentDO instrument = instrumentService.getInstrument(id);
        return success(BeanUtils.toBean(instrument, QmsInstrumentRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得计量器具分页")
    @PreAuthorize("@ss.hasPermission('qms:instrument:query')")
    public CommonResult<PageResult<QmsInstrumentRespVO>> getInstrumentPage(@Valid QmsInstrumentPageReqVO pageReqVO) {
        PageResult<QmsInstrumentDO> pageResult = instrumentService.getInstrumentPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, QmsInstrumentRespVO.class));
    }

    @GetMapping("/expiring-soon")
    @Operation(summary = "获得校准即将到期的器具列表", description = "返回在用状态且下次校准日期在指定天数内的器具")
    @Parameter(name = "withinDays", description = "未来天数", required = true, example = "30")
    @PreAuthorize("@ss.hasPermission('qms:instrument:query')")
    public CommonResult<List<QmsInstrumentExpiringSoonRespVO>> getExpiringSoonInstruments(
            @RequestParam("withinDays") int withinDays) {
        return success(instrumentService.getExpiringSoonInstruments(withinDays));
    }

    @GetMapping("/overdue")
    @Operation(summary = "获得已逾期未校准的器具列表", description = "返回在用状态且下次校准日期早于今天的器具")
    @PreAuthorize("@ss.hasPermission('qms:instrument:query')")
    public CommonResult<List<QmsInstrumentExpiringSoonRespVO>> getOverdueInstruments() {
        return success(instrumentService.getOverdueInstruments());
    }

}