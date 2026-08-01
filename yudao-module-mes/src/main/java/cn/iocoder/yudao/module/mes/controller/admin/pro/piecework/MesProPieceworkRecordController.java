package cn.iocoder.yudao.module.mes.controller.admin.pro.piecework;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.piecework.vo.MesProPieceworkRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.piecework.vo.MesProPieceworkRecordRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.piecework.MesProPieceworkRecordDO;
import cn.iocoder.yudao.module.mes.service.pro.piecework.MesProPieceworkRecordService;
import cn.iocoder.yudao.module.mes.service.pro.piecework.MesProPieceworkSummaryDTO;
import cn.iocoder.yudao.module.mes.service.pro.piecework.MesProPieceworkSummaryService;
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

@Tag(name = "管理后台 - MES 计件工资明细")
@RestController
@RequestMapping("/mes/pro-piecework-record")
@Validated
public class MesProPieceworkRecordController {

    @Resource
    private MesProPieceworkRecordService pieceworkRecordService;
    @Resource
    private MesProPieceworkSummaryService pieceworkSummaryService;

    @GetMapping("/get")
    @Operation(summary = "获得计件工资明细")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('mes:pro-piecework-record:query')")
    public CommonResult<MesProPieceworkRecordRespVO> getPieceworkRecord(@RequestParam("id") Long id) {
        MesProPieceworkRecordDO record = pieceworkRecordService.getPieceworkRecord(id);
        return success(BeanUtils.toBean(record, MesProPieceworkRecordRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得计件工资明细分页")
    @PreAuthorize("@ss.hasPermission('mes:pro-piecework-record:query')")
    public CommonResult<PageResult<MesProPieceworkRecordRespVO>> getPieceworkRecordPage(@Valid MesProPieceworkRecordPageReqVO pageReqVO) {
        PageResult<MesProPieceworkRecordDO> pageResult = pieceworkRecordService.getPieceworkRecordPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, MesProPieceworkRecordRespVO.class));
    }

    @GetMapping("/summary-by-period")
    @Operation(summary = "按月份汇总计件工资（全员工）")
    @Parameter(name = "periodMonth", description = "月份（yyyyMM）", required = true, example = "202607")
    @PreAuthorize("@ss.hasPermission('mes:pro-piecework-record:query')")
    public CommonResult<List<MesProPieceworkSummaryDTO>> summaryByPeriod(@RequestParam("periodMonth") String periodMonth) {
        return success(pieceworkSummaryService.summaryByPeriod(periodMonth));
    }

    @GetMapping("/summary-by-user")
    @Operation(summary = "按员工 + 月份汇总计件工资")
    @Parameter(name = "feedbackUserId", description = "报工用户编号", required = true, example = "100")
    @Parameter(name = "periodMonth", description = "月份（yyyyMM）", required = true, example = "202607")
    @PreAuthorize("@ss.hasPermission('mes:pro-piecework-record:query')")
    public CommonResult<MesProPieceworkSummaryDTO> summaryByUserAndPeriod(
            @RequestParam("feedbackUserId") Long feedbackUserId,
            @RequestParam("periodMonth") String periodMonth) {
        return success(pieceworkSummaryService.summaryByUserAndPeriod(feedbackUserId, periodMonth));
    }

}
