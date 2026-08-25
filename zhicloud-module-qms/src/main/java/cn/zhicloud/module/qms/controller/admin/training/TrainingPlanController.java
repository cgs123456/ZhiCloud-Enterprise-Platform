package cn.zhicloud.module.qms.controller.admin.training;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.qms.controller.admin.training.vo.TrainingPlanPageReqVO;
import cn.zhicloud.module.qms.controller.admin.training.vo.TrainingPlanRespVO;
import cn.zhicloud.module.qms.controller.admin.training.vo.TrainingPlanSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.training.TrainingPlanDO;
import cn.zhicloud.module.qms.service.training.TrainingPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * QMS 培训计划 Controller
 *
 * @author zhicloud
 */
@Tag(name = "管理后台 - QMS 培训计划")
@RestController
@RequestMapping("/qms/training-plan")
@Validated
public class TrainingPlanController {

    @Resource
    private TrainingPlanService trainingPlanService;

    @PostMapping("/create")
    @Operation(summary = "创建培训计划")
    @PreAuthorize("@ss.hasPermission('qms:training-plan:create')")
    public CommonResult<Long> createTrainingPlan(@Valid @RequestBody TrainingPlanSaveReqVO createReqVO) {
        return success(trainingPlanService.createTrainingPlan(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新培训计划")
    @PreAuthorize("@ss.hasPermission('qms:training-plan:update')")
    public CommonResult<Boolean> updateTrainingPlan(@Valid @RequestBody TrainingPlanSaveReqVO updateReqVO) {
        trainingPlanService.updateTrainingPlan(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除培训计划")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('qms:training-plan:delete')")
    public CommonResult<Boolean> deleteTrainingPlan(@RequestParam("id") Long id) {
        trainingPlanService.deleteTrainingPlan(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得培训计划")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:training-plan:query')")
    public CommonResult<TrainingPlanRespVO> getTrainingPlan(@RequestParam("id") Long id) {
        TrainingPlanDO trainingPlan = trainingPlanService.getTrainingPlan(id);
        return success(BeanUtils.toBean(trainingPlan, TrainingPlanRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得培训计划分页")
    @PreAuthorize("@ss.hasPermission('qms:training-plan:query')")
    public CommonResult<PageResult<TrainingPlanRespVO>> getTrainingPlanPage(@Valid TrainingPlanPageReqVO pageReqVO) {
        PageResult<TrainingPlanDO> pageResult = trainingPlanService.getTrainingPlanPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, TrainingPlanRespVO.class));
    }

}