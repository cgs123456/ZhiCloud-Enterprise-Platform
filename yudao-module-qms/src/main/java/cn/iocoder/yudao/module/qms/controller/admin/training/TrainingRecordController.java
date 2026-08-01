package cn.iocoder.yudao.module.qms.controller.admin.training;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.training.vo.TrainingRecordPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.training.vo.TrainingRecordRespVO;
import cn.iocoder.yudao.module.qms.controller.admin.training.vo.TrainingRecordSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.training.TrainingRecordDO;
import cn.iocoder.yudao.module.qms.service.training.TrainingRecordService;
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
 * QMS 培训记录 Controller
 *
 * @author yudao
 */
@Tag(name = "管理后台 - QMS 培训记录")
@RestController
@RequestMapping("/qms/training-record")
@Validated
public class TrainingRecordController {

    @Resource
    private TrainingRecordService trainingRecordService;

    @PostMapping("/create")
    @Operation(summary = "创建培训记录")
    @PreAuthorize("@ss.hasPermission('qms:training-record:create')")
    public CommonResult<Long> createTrainingRecord(@Valid @RequestBody TrainingRecordSaveReqVO createReqVO) {
        return success(trainingRecordService.createTrainingRecord(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新培训记录")
    @PreAuthorize("@ss.hasPermission('qms:training-record:update')")
    public CommonResult<Boolean> updateTrainingRecord(@Valid @RequestBody TrainingRecordSaveReqVO updateReqVO) {
        trainingRecordService.updateTrainingRecord(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除培训记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('qms:training-record:delete')")
    public CommonResult<Boolean> deleteTrainingRecord(@RequestParam("id") Long id) {
        trainingRecordService.deleteTrainingRecord(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得培训记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:training-record:query')")
    public CommonResult<TrainingRecordRespVO> getTrainingRecord(@RequestParam("id") Long id) {
        TrainingRecordDO trainingRecord = trainingRecordService.getTrainingRecord(id);
        return success(BeanUtils.toBean(trainingRecord, TrainingRecordRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得培训记录分页")
    @PreAuthorize("@ss.hasPermission('qms:training-record:query')")
    public CommonResult<PageResult<TrainingRecordRespVO>> getTrainingRecordPage(@Valid TrainingRecordPageReqVO pageReqVO) {
        PageResult<TrainingRecordDO> pageResult = trainingRecordService.getTrainingRecordPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, TrainingRecordRespVO.class));
    }

    @GetMapping("/list-by-plan")
    @Operation(summary = "获得培训计划关联的培训记录列表")
    @Parameter(name = "planId", description = "培训计划 ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:training-record:query')")
    public CommonResult<List<TrainingRecordRespVO>> getTrainingRecordListByPlanId(@RequestParam("planId") Long planId) {
        List<TrainingRecordDO> list = trainingRecordService.getTrainingRecordListByPlanId(planId);
        return success(BeanUtils.toBean(list, TrainingRecordRespVO.class));
    }

}