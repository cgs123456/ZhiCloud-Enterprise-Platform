package cn.iocoder.yudao.module.qms.controller.admin.eightd;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.eightd.vo.EightDReportPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.eightd.vo.EightDReportRespVO;
import cn.iocoder.yudao.module.qms.controller.admin.eightd.vo.EightDReportSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.eightd.EightDReportDO;
import cn.iocoder.yudao.module.qms.service.eightd.EightDReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * QMS 8D 报告 Controller
 *
 * @author yudao
 */
@Tag(name = "管理后台 - QMS 8D 报告")
@RestController
@RequestMapping("/qms/eight-d")
@Validated
public class EightDReportController {

    @Resource
    private EightDReportService eightDReportService;

    @PostMapping("/create")
    @Operation(summary = "创建 8D 报告")
    @PreAuthorize("@ss.hasPermission('qms:eight-d:create')")
    public CommonResult<Long> createEightDReport(@Valid @RequestBody EightDReportSaveReqVO createReqVO) {
        return success(eightDReportService.createEightDReport(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新 8D 报告")
    @PreAuthorize("@ss.hasPermission('qms:eight-d:update')")
    public CommonResult<Boolean> updateEightDReport(@Valid @RequestBody EightDReportSaveReqVO updateReqVO) {
        eightDReportService.updateEightDReport(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除 8D 报告")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('qms:eight-d:delete')")
    public CommonResult<Boolean> deleteEightDReport(@RequestParam("id") Long id) {
        eightDReportService.deleteEightDReport(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得 8D 报告")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:eight-d:query')")
    public CommonResult<EightDReportRespVO> getEightDReport(@RequestParam("id") Long id) {
        EightDReportDO eightDReport = eightDReportService.getEightDReport(id);
        return success(BeanUtils.toBean(eightDReport, EightDReportRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得 8D 报告分页")
    @PreAuthorize("@ss.hasPermission('qms:eight-d:query')")
    public CommonResult<PageResult<EightDReportRespVO>> getEightDReportPage(@Valid EightDReportPageReqVO pageReqVO) {
        PageResult<EightDReportDO> pageResult = eightDReportService.getEightDReportPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, EightDReportRespVO.class));
    }

    @PutMapping("/advance-stage")
    @Operation(summary = "推进 8D 阶段", description = "将 8D 报告推进至下一阶段（D1->D2->...->D7）")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:eight-d:update')")
    public CommonResult<Boolean> advanceStage(@RequestParam("id") Long id) {
        eightDReportService.advanceStage(id);
        return success(true);
    }

    @PutMapping("/close")
    @Operation(summary = "关闭 8D 报告", description = "关闭 8D 报告，流转为 D8 团队表彰/关闭")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:eight-d:update')")
    public CommonResult<Boolean> closeEightDReport(@RequestParam("id") Long id) {
        eightDReportService.closeEightDReport(id);
        return success(true);
    }

}