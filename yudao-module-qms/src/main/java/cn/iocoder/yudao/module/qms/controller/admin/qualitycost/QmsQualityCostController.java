package cn.iocoder.yudao.module.qms.controller.admin.qualitycost;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.qualitycost.vo.QmsQualityCostPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.qualitycost.vo.QmsQualityCostRespVO;
import cn.iocoder.yudao.module.qms.controller.admin.qualitycost.vo.QmsQualityCostSaveReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.qualitycost.vo.QualityCostSummaryRespVO;
import cn.iocoder.yudao.module.qms.controller.admin.qualitycost.vo.QualityCostTrendRespVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.qualitycost.QmsQualityCostDO;
import cn.iocoder.yudao.module.qms.service.qualitycost.QmsQualityCostService;
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
 * QMS 质量成本 Controller（PAIF 模型）
 *
 * @author yudao
 */
@Tag(name = "管理后台 - QMS 质量成本")
@RestController
@RequestMapping("/qms/quality-cost")
@Validated
public class QmsQualityCostController {

    @Resource
    private QmsQualityCostService qualityCostService;

    @PostMapping("/create")
    @Operation(summary = "创建质量成本记录")
    @PreAuthorize("@ss.hasPermission('qms:quality-cost:create')")
    public CommonResult<Long> createQualityCost(@Valid @RequestBody QmsQualityCostSaveReqVO createReqVO) {
        return success(qualityCostService.createQualityCost(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新质量成本记录")
    @PreAuthorize("@ss.hasPermission('qms:quality-cost:update')")
    public CommonResult<Boolean> updateQualityCost(@Valid @RequestBody QmsQualityCostSaveReqVO updateReqVO) {
        qualityCostService.updateQualityCost(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除质量成本记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('qms:quality-cost:delete')")
    public CommonResult<Boolean> deleteQualityCost(@RequestParam("id") Long id) {
        qualityCostService.deleteQualityCost(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得质量成本记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:quality-cost:query')")
    public CommonResult<QmsQualityCostRespVO> getQualityCost(@RequestParam("id") Long id) {
        QmsQualityCostDO qualityCost = qualityCostService.getQualityCost(id);
        return success(BeanUtils.toBean(qualityCost, QmsQualityCostRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得质量成本分页")
    @PreAuthorize("@ss.hasPermission('qms:quality-cost:query')")
    public CommonResult<PageResult<QmsQualityCostRespVO>> getQualityCostPage(@Valid QmsQualityCostPageReqVO pageReqVO) {
        PageResult<QmsQualityCostDO> pageResult = qualityCostService.getQualityCostPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, QmsQualityCostRespVO.class));
    }

    @GetMapping("/summary")
    @Operation(summary = "获得质量成本汇总", description = "按 PAIF 四类汇总指定年月的质量成本，含金额、总计与占比")
    @PreAuthorize("@ss.hasPermission('qms:quality-cost:query')")
    public CommonResult<QualityCostSummaryRespVO> getQualityCostSummary(
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month) {
        return success(qualityCostService.getQualityCostSummary(year, month));
    }

    @GetMapping("/trend")
    @Operation(summary = "获得质量成本年度趋势", description = "按月份返回 PAIF 四类成本及总额趋势")
    @PreAuthorize("@ss.hasPermission('qms:quality-cost:query')")
    public CommonResult<QualityCostTrendRespVO> getQualityCostTrend(@RequestParam("year") Integer year) {
        return success(qualityCostService.getQualityCostTrend(year));
    }

    @GetMapping("/cumulative")
    @Operation(summary = "获得质量成本累计", description = "按 PAIF 四类累计指定年度区间月份的质量成本")
    @PreAuthorize("@ss.hasPermission('qms:quality-cost:query')")
    public CommonResult<QualityCostSummaryRespVO> getCumulativeQualityCost(
            @RequestParam("year") Integer year,
            @RequestParam("startMonth") Integer startMonth,
            @RequestParam("endMonth") Integer endMonth) {
        return success(qualityCostService.getCumulativeQualityCost(year, startMonth, endMonth));
    }

}