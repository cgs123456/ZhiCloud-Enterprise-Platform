package cn.zhicloud.module.erp.controller.admin.stock.vmi;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.stock.vmi.vo.ErpVmiInventoryPageReqVO;
import cn.zhicloud.module.erp.controller.admin.stock.vmi.vo.ErpVmiInventoryRespVO;
import cn.zhicloud.module.erp.controller.admin.stock.vmi.vo.ErpVmiInventorySaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.stock.vmi.ErpVmiInventoryDO;
import cn.zhicloud.module.erp.service.stock.vmi.ErpVmiInventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP VMI 供应商管理库存")
@RestController
@RequestMapping("/erp/vmi-inventory")
@Validated
public class ErpVmiInventoryController {

    @Resource
    private ErpVmiInventoryService vmiInventoryService;

    @PostMapping("/create")
    @Operation(summary = "创建 VMI 库存")
    @PreAuthorize("@ss.hasPermission('erp:vmi-inventory:create')")
    public CommonResult<Long> createVmiInventory(@Valid @RequestBody ErpVmiInventorySaveReqVO createReqVO) {
        return success(vmiInventoryService.createVmiInventory(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新 VMI 库存")
    @PreAuthorize("@ss.hasPermission('erp:vmi-inventory:update')")
    public CommonResult<Boolean> updateVmiInventory(@Valid @RequestBody ErpVmiInventorySaveReqVO updateReqVO) {
        vmiInventoryService.updateVmiInventory(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除 VMI 库存")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:vmi-inventory:delete')")
    public CommonResult<Boolean> deleteVmiInventory(@RequestParam("id") Long id) {
        vmiInventoryService.deleteVmiInventory(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得 VMI 库存")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:vmi-inventory:query')")
    public CommonResult<ErpVmiInventoryRespVO> getVmiInventory(@RequestParam("id") Long id) {
        ErpVmiInventoryDO inventory = vmiInventoryService.getVmiInventory(id);
        return success(BeanUtils.toBean(inventory, ErpVmiInventoryRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得 VMI 库存分页")
    @PreAuthorize("@ss.hasPermission('erp:vmi-inventory:query')")
    public CommonResult<PageResult<ErpVmiInventoryRespVO>> getVmiInventoryPage(@Valid ErpVmiInventoryPageReqVO pageReqVO) {
        PageResult<ErpVmiInventoryDO> pageResult = vmiInventoryService.getVmiInventoryPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpVmiInventoryRespVO.class));
    }

    @GetMapping("/check-replenishment")
    @Operation(summary = "检查补货点，返回需要补货的库存列表")
    @PreAuthorize("@ss.hasPermission('erp:vmi-inventory:query')")
    public CommonResult<List<ErpVmiInventoryRespVO>> checkReplenishment() {
        List<ErpVmiInventoryDO> list = vmiInventoryService.checkReplenishment();
        return success(BeanUtils.toBean(list, ErpVmiInventoryRespVO.class));
    }

}
