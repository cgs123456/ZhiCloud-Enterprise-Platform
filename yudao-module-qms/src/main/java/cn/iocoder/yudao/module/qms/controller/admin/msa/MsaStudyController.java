package cn.iocoder.yudao.module.qms.controller.admin.msa;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.msa.vo.*;
import cn.iocoder.yudao.module.qms.dal.dataobject.msa.MsaMeasurementDO;
import cn.iocoder.yudao.module.qms.dal.dataobject.msa.MsaStudyDO;
import cn.iocoder.yudao.module.qms.service.msa.MsaStudyService;
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
 * QMS MSA 测量系统分析 Controller
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - QMS MSA 测量系统分析")
@RestController
@RequestMapping("/qms/msa")
@Validated
public class MsaStudyController {

    @Resource
    private MsaStudyService msaStudyService;

    @PostMapping("/create")
    @Operation(summary = "创建 MSA 研究")
    @PreAuthorize("@ss.hasPermission('qms:msa:create')")
    public CommonResult<Long> createMsaStudy(@Valid @RequestBody MsaStudySaveReqVO createReqVO) {
        return success(msaStudyService.createMsaStudy(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新 MSA 研究")
    @PreAuthorize("@ss.hasPermission('qms:msa:update')")
    public CommonResult<Boolean> updateMsaStudy(@Valid @RequestBody MsaStudySaveReqVO updateReqVO) {
        msaStudyService.updateMsaStudy(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除 MSA 研究")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('qms:msa:delete')")
    public CommonResult<Boolean> deleteMsaStudy(@RequestParam("id") Long id) {
        msaStudyService.deleteMsaStudy(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得 MSA 研究")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:msa:query')")
    public CommonResult<MsaStudyRespVO> getMsaStudy(@RequestParam("id") Long id) {
        MsaStudyDO msaStudy = msaStudyService.getMsaStudy(id);
        return success(BeanUtils.toBean(msaStudy, MsaStudyRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得 MSA 研究分页")
    @PreAuthorize("@ss.hasPermission('qms:msa:query')")
    public CommonResult<PageResult<MsaStudyRespVO>> getMsaStudyPage(@Valid MsaStudyPageReqVO pageReqVO) {
        PageResult<MsaStudyDO> pageResult = msaStudyService.getMsaStudyPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, MsaStudyRespVO.class));
    }

    @PostMapping("/measurement/save")
    @Operation(summary = "保存/录入测量数据")
    @PreAuthorize("@ss.hasPermission('qms:msa:update')")
    public CommonResult<Long> saveMeasurement(@Valid @RequestBody MsaMeasurementSaveReqVO reqVO) {
        return success(msaStudyService.saveMeasurement(reqVO));
    }

    @GetMapping("/measurement/list")
    @Operation(summary = "获取测量数据")
    @Parameter(name = "studyId", description = "研究 ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:msa:query')")
    public CommonResult<List<MsaMeasurementRespVO>> getMeasurementData(@RequestParam("studyId") Long studyId) {
        List<MsaMeasurementDO> list = msaStudyService.getMeasurementData(studyId);
        return success(BeanUtils.toBean(list, MsaMeasurementRespVO.class));
    }

    @GetMapping("/calculate-gage-rr")
    @Operation(summary = "计算 GR&R（均值极差法 Xbar-R）")
    @Parameter(name = "studyId", description = "研究 ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:msa:query')")
    public CommonResult<MsaGageRRRespVO> calculateGageRR(@RequestParam("studyId") Long studyId) {
        return success(msaStudyService.calculateGageRR(studyId));
    }

}
