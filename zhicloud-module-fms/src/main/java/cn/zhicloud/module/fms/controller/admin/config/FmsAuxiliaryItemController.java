package cn.zhicloud.module.fms.controller.admin.config;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageParam;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.fms.controller.admin.config.vo.auxiliary.FmsAuxiliaryItemRespVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.auxiliary.FmsAuxiliaryItemSaveReqVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.auxiliary.FmsAuxiliaryItemPageReqVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.auxiliary.FmsAuxiliaryItemImportExcelVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.auxiliary.FmsAuxiliaryItemImportRespVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.auxiliary.FmsAuxiliaryItemStatusReqVO;
import cn.zhicloud.framework.excel.core.util.ExcelUtils;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsAuxiliaryItemDO;
import cn.zhicloud.module.fms.service.config.FmsAuxiliaryItemService;
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

@Tag(name = "管理后台 - FMS 辅助核算项目")
@RestController
@RequestMapping("/fms/config/auxiliary-item")
@Validated
public class FmsAuxiliaryItemController {

    @Resource
    private FmsAuxiliaryItemService auxiliaryItemService;

    @GetMapping("/list")
    @Operation(summary = "获得辅助核算项目列表")
    @Parameter(name = "accountSetId", description = "账套编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('fms:config:auxiliary:query')")
    public CommonResult<List<FmsAuxiliaryItemRespVO>> getAuxiliaryItemList(@RequestParam("accountSetId") Long accountSetId) {
        List<FmsAuxiliaryItemDO> list = auxiliaryItemService.getAuxiliaryItemList(accountSetId);
        return success(BeanUtils.toBean(list, FmsAuxiliaryItemRespVO.class));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得辅助核算项目精简列表")
    @Parameter(name = "accountSetId", description = "账套编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('fms:config:auxiliary:query')")
    public CommonResult<List<FmsAuxiliaryItemRespVO>> getAuxiliaryItemSimpleList(@RequestParam("accountSetId") Long accountSetId, @RequestParam("auxiliaryTypeId") Long auxiliaryTypeId) {
        List<FmsAuxiliaryItemDO> list = auxiliaryItemService.getAuxiliaryItemList(accountSetId);
        list = list.stream().filter(item -> auxiliaryTypeId.equals(item.getAuxiliaryTypeId())).toList();
        return success(BeanUtils.toBean(list, FmsAuxiliaryItemRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得辅助核算项目分页")
    @PreAuthorize("@ss.hasPermission('fms:config:auxiliary:query')")
    public CommonResult<PageResult<FmsAuxiliaryItemRespVO>> getAuxiliaryItemPage(@Valid FmsAuxiliaryItemPageReqVO pageReqVO) {
        PageResult<FmsAuxiliaryItemDO> pageResult = auxiliaryItemService.getAuxiliaryItemPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, FmsAuxiliaryItemRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "创建辅助核算项目")
    @PreAuthorize("@ss.hasPermission('fms:config:auxiliary:create')")
    public CommonResult<Long> createAuxiliaryItem(@Valid @RequestBody FmsAuxiliaryItemSaveReqVO createReqVO) {
        return success(auxiliaryItemService.createAuxiliaryItem(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新辅助核算项目")
    @PreAuthorize("@ss.hasPermission('fms:config:auxiliary:update')")
    public CommonResult<Boolean> updateAuxiliaryItem(@Valid @RequestBody FmsAuxiliaryItemSaveReqVO updateReqVO) {
        auxiliaryItemService.updateAuxiliaryItem(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除辅助核算项目")
    @Parameter(name = "accountSetId", description = "账套编号", required = true, example = "1")
    @Parameter(name = "ids", description = "编号列表，逗号分隔", required = true)
    @PreAuthorize("@ss.hasPermission('fms:config:auxiliary:delete')")
    public CommonResult<Boolean> deleteAuxiliaryItemList(@RequestParam("accountSetId") Long accountSetId,
                                              @RequestParam("ids") List<Long> ids) {
        auxiliaryItemService.deleteAuxiliaryItemList(accountSetId, ids);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "修改辅助核算项目状态")
    @PreAuthorize("@ss.hasPermission('fms:config:auxiliary:update')")
    public CommonResult<Boolean> updateAuxiliaryItemStatus(@Valid @RequestBody FmsAuxiliaryItemStatusReqVO reqVO) {
        auxiliaryItemService.updateAuxiliaryItemStatus(reqVO.getAccountSetId(), reqVO.getId(), reqVO.getStatus());
        return success(true);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出辅助核算项目 Excel")
    @PreAuthorize("@ss.hasPermission('fms:config:auxiliary:export')")
    public void exportAuxiliaryItemExcel(@Valid FmsAuxiliaryItemPageReqVO pageReqVO,
                              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<FmsAuxiliaryItemDO> list = auxiliaryItemService.getAuxiliaryItemPage(pageReqVO).getList();
        ExcelUtils.write(response, "辅助核算项目.xls", "数据", FmsAuxiliaryItemRespVO.class,
                BeanUtils.toBean(list, FmsAuxiliaryItemRespVO.class));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得辅助核算项目导入模板")
    public void importTemplate(@RequestParam(value = "type", required = false) Integer type,
                               HttpServletResponse response) throws IOException {
        List<FmsAuxiliaryItemImportExcelVO> list = Collections.singletonList(
                FmsAuxiliaryItemImportExcelVO.builder().code("A001").name("示例项目").auxiliaryTypeId(1L)
                        .status(0).remark("示例").specification("").unit("").build());
        ExcelUtils.write(response, "辅助核算项目导入模板.xls", "辅助核算项目列表", FmsAuxiliaryItemImportExcelVO.class, list);
    }

    @PostMapping("/import")
    @Operation(summary = "导入辅助核算项目")
    @Parameter(name = "file", description = "Excel 文件", required = true)
    @PreAuthorize("@ss.hasPermission('fms:config:auxiliary:import')")
    public CommonResult<FmsAuxiliaryItemImportRespVO> importExcel(
            @RequestParam("accountSetId") Long accountSetId,
            @RequestParam("auxiliaryTypeId") Long auxiliaryTypeId,
            @RequestParam("file") MultipartFile file) throws Exception {
        List<FmsAuxiliaryItemImportExcelVO> list = ExcelUtils.read(file, FmsAuxiliaryItemImportExcelVO.class);
        return success(auxiliaryItemService.importAuxiliaryItemList(accountSetId, auxiliaryTypeId, list, false));
    }

}
