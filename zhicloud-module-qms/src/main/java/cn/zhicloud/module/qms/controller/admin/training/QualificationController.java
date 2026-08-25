package cn.zhicloud.module.qms.controller.admin.training;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.qms.controller.admin.training.vo.QualificationPageReqVO;
import cn.zhicloud.module.qms.controller.admin.training.vo.QualificationRespVO;
import cn.zhicloud.module.qms.controller.admin.training.vo.QualificationSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.training.QualificationDO;
import cn.zhicloud.module.qms.service.training.QualificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * QMS 岗位资格 Controller
 *
 * @author zhicloud
 */
@Tag(name = "管理后台 - QMS 岗位资格")
@RestController
@RequestMapping("/qms/qualification")
@Validated
public class QualificationController {

    @Resource
    private QualificationService qualificationService;

    @PostMapping("/create")
    @Operation(summary = "创建岗位资格")
    @PreAuthorize("@ss.hasPermission('qms:qualification:create')")
    public CommonResult<Long> createQualification(@Valid @RequestBody QualificationSaveReqVO createReqVO) {
        return success(qualificationService.createQualification(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新岗位资格")
    @PreAuthorize("@ss.hasPermission('qms:qualification:update')")
    public CommonResult<Boolean> updateQualification(@Valid @RequestBody QualificationSaveReqVO updateReqVO) {
        qualificationService.updateQualification(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除岗位资格")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('qms:qualification:delete')")
    public CommonResult<Boolean> deleteQualification(@RequestParam("id") Long id) {
        qualificationService.deleteQualification(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得岗位资格")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:qualification:query')")
    public CommonResult<QualificationRespVO> getQualification(@RequestParam("id") Long id) {
        QualificationDO qualification = qualificationService.getQualification(id);
        return success(BeanUtils.toBean(qualification, QualificationRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得岗位资格分页")
    @PreAuthorize("@ss.hasPermission('qms:qualification:query')")
    public CommonResult<PageResult<QualificationRespVO>> getQualificationPage(@Valid QualificationPageReqVO pageReqVO) {
        PageResult<QualificationDO> pageResult = qualificationService.getQualificationPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, QualificationRespVO.class));
    }

    @GetMapping("/expiring-list")
    @Operation(summary = "获得即将到期的资格列表", description = "查询到期日早于等于指定日期的资格列表，用于到期预警")
    @Parameter(name = "expireDate", description = "到期日阈值", required = true, example = "2024-12-31")
    @PreAuthorize("@ss.hasPermission('qms:qualification:query')")
    public CommonResult<List<QualificationRespVO>> getExpiringQualificationList(
            @RequestParam("expireDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate expireDate) {
        List<QualificationDO> list = qualificationService.getExpiringQualificationList(expireDate);
        return success(BeanUtils.toBean(list, QualificationRespVO.class));
    }

}