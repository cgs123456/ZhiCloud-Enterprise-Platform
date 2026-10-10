package cn.zhicloud.module.fms.controller.admin.config;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.fms.controller.admin.config.vo.auxiliary.FmsAuxiliaryTypeRespVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.auxiliary.FmsAuxiliaryTypeSaveReqVO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsAuxiliaryTypeDO;
import cn.zhicloud.module.fms.service.config.FmsAuxiliaryTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - FMS 辅助核算类别")
@RestController
@RequestMapping("/fms/config/auxiliary-type")
@Validated
public class FmsAuxiliaryTypeController {

    @Resource
    private FmsAuxiliaryTypeService auxiliaryTypeService;

    @GetMapping("/list")
    @Operation(summary = "获得辅助核算类别列表")
    @Parameter(name = "accountSetId", description = "账套编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('fms:config:auxiliary:query')")
    public CommonResult<List<FmsAuxiliaryTypeRespVO>> getAuxiliaryTypeList(@RequestParam("accountSetId") Long accountSetId) {
        List<FmsAuxiliaryTypeDO> list = auxiliaryTypeService.getAuxiliaryTypeList(accountSetId);
        return success(BeanUtils.toBean(list, FmsAuxiliaryTypeRespVO.class));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得辅助核算类别精简列表")
    @Parameter(name = "accountSetId", description = "账套编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('fms:config:auxiliary:query')")
    public CommonResult<List<FmsAuxiliaryTypeRespVO>> getAuxiliaryTypeSimpleList(@RequestParam("accountSetId") Long accountSetId) {
        List<FmsAuxiliaryTypeDO> list = auxiliaryTypeService.getAuxiliaryTypeList(accountSetId);
        return success(BeanUtils.toBean(list, FmsAuxiliaryTypeRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "创建辅助核算类别")
    @PreAuthorize("@ss.hasPermission('fms:config:auxiliary:create')")
    public CommonResult<Long> createAuxiliaryType(@Valid @RequestBody FmsAuxiliaryTypeSaveReqVO createReqVO) {
        return success(auxiliaryTypeService.createAuxiliaryType(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新辅助核算类别")
    @PreAuthorize("@ss.hasPermission('fms:config:auxiliary:update')")
    public CommonResult<Boolean> updateAuxiliaryType(@Valid @RequestBody FmsAuxiliaryTypeSaveReqVO updateReqVO) {
        auxiliaryTypeService.updateAuxiliaryType(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除辅助核算类别")
    @Parameter(name = "accountSetId", description = "账套编号", required = true, example = "1")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('fms:config:auxiliary:delete')")
    public CommonResult<Boolean> deleteAuxiliaryType(@RequestParam("accountSetId") Long accountSetId,
                                            @RequestParam("id") Long id) {
        auxiliaryTypeService.deleteAuxiliaryType(accountSetId, id);
        return success(true);
    }

}
