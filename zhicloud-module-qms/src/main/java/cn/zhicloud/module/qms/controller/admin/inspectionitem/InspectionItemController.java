package cn.zhicloud.module.qms.controller.admin.inspectionitem;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.qms.controller.admin.inspectionitem.vo.InspectionItemPageReqVO;
import cn.zhicloud.module.qms.controller.admin.inspectionitem.vo.InspectionItemRespVO;
import cn.zhicloud.module.qms.controller.admin.inspectionitem.vo.InspectionItemSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.inspectionitem.InspectionItemDO;
import cn.zhicloud.module.qms.service.inspectionitem.InspectionItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - QMS 检验项目")
@RestController
@RequestMapping("/qms/inspection-item")
@Validated
public class InspectionItemController {

    @Resource
    private InspectionItemService inspectionItemService;

    @PostMapping("/create")
    @Operation(summary = "创建检验项目")
    @PreAuthorize("@ss.hasPermission('qms:inspection-item:create')")
    public CommonResult<Long> createInspectionItem(@Valid @RequestBody InspectionItemSaveReqVO createReqVO) {
        return success(inspectionItemService.createInspectionItem(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新检验项目")
    @PreAuthorize("@ss.hasPermission('qms:inspection-item:update')")
    public CommonResult<Boolean> updateInspectionItem(@Valid @RequestBody InspectionItemSaveReqVO updateReqVO) {
        inspectionItemService.updateInspectionItem(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除检验项目")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('qms:inspection-item:delete')")
    public CommonResult<Boolean> deleteInspectionItem(@RequestParam("id") Long id) {
        inspectionItemService.deleteInspectionItem(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得检验项目")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:inspection-item:query')")
    public CommonResult<InspectionItemRespVO> getInspectionItem(@RequestParam("id") Long id) {
        InspectionItemDO inspectionItem = inspectionItemService.getInspectionItem(id);
        return success(BeanUtils.toBean(inspectionItem, InspectionItemRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得检验项目分页")
    @PreAuthorize("@ss.hasPermission('qms:inspection-item:query')")
    public CommonResult<PageResult<InspectionItemRespVO>> getInspectionItemPage(@Valid InspectionItemPageReqVO pageReqVO) {
        PageResult<InspectionItemDO> pageResult = inspectionItemService.getInspectionItemPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, InspectionItemRespVO.class));
    }

}
