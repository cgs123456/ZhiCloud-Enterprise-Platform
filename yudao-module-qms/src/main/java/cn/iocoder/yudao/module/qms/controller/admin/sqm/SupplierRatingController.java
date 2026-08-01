package cn.iocoder.yudao.module.qms.controller.admin.sqm;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.sqm.vo.SupplierRatingPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.sqm.vo.SupplierRatingRespVO;
import cn.iocoder.yudao.module.qms.controller.admin.sqm.vo.SupplierRatingSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.sqm.SupplierRatingDO;
import cn.iocoder.yudao.module.qms.enums.qms.SupplierGradeEnum;
import cn.iocoder.yudao.module.qms.service.sqm.SupplierRatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * QMS 供应商评级 Controller
 *
 * @author yudao
 */
@Tag(name = "管理后台 - QMS 供应商评级")
@RestController
@RequestMapping("/qms/supplier-rating")
@Validated
public class SupplierRatingController {

    @Resource
    private SupplierRatingService supplierRatingService;

    @PostMapping("/create")
    @Operation(summary = "创建供应商评级")
    @PreAuthorize("@ss.hasPermission('qms:supplier-rating:create')")
    public CommonResult<Long> createSupplierRating(@Valid @RequestBody SupplierRatingSaveReqVO createReqVO) {
        return success(supplierRatingService.createSupplierRating(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新供应商评级")
    @PreAuthorize("@ss.hasPermission('qms:supplier-rating:update')")
    public CommonResult<Boolean> updateSupplierRating(@Valid @RequestBody SupplierRatingSaveReqVO updateReqVO) {
        supplierRatingService.updateSupplierRating(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除供应商评级")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('qms:supplier-rating:delete')")
    public CommonResult<Boolean> deleteSupplierRating(@RequestParam("id") Long id) {
        supplierRatingService.deleteSupplierRating(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得供应商评级")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:supplier-rating:query')")
    public CommonResult<SupplierRatingRespVO> getSupplierRating(@RequestParam("id") Long id) {
        SupplierRatingDO supplierRating = supplierRatingService.getSupplierRating(id);
        return success(BeanUtils.toBean(supplierRating, SupplierRatingRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得供应商评级分页")
    @PreAuthorize("@ss.hasPermission('qms:supplier-rating:query')")
    public CommonResult<PageResult<SupplierRatingRespVO>> getSupplierRatingPage(@Valid SupplierRatingPageReqVO pageReqVO) {
        PageResult<SupplierRatingDO> pageResult = supplierRatingService.getSupplierRatingPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, SupplierRatingRespVO.class));
    }

    @GetMapping("/calculate-grade")
    @Operation(summary = "计算供应商等级", description = "根据 PPM、交期达成率、质量合格率计算 A/B/C/D 等级")
    @PreAuthorize("@ss.hasPermission('qms:supplier-rating:query')")
    public CommonResult<String> calculateGrade(@RequestParam("ppm") int ppm,
                                               @RequestParam("onTimeRate") BigDecimal onTimeRate,
                                               @RequestParam("qualityRate") BigDecimal qualityRate) {
        SupplierGradeEnum grade = supplierRatingService.calculateGrade(ppm, onTimeRate, qualityRate);
        return success(grade.getGrade());
    }

}