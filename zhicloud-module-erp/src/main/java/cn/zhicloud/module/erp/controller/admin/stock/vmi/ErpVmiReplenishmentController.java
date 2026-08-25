package cn.zhicloud.module.erp.controller.admin.stock.vmi;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.stock.vmi.vo.ErpVmiReplenishmentItemRespVO;
import cn.zhicloud.module.erp.controller.admin.stock.vmi.vo.ErpVmiReplenishmentPageReqVO;
import cn.zhicloud.module.erp.controller.admin.stock.vmi.vo.ErpVmiReplenishmentRespVO;
import cn.zhicloud.module.erp.dal.dataobject.stock.vmi.ErpVmiReplenishmentDO;
import cn.zhicloud.module.erp.dal.dataobject.stock.vmi.ErpVmiReplenishmentItemDO;
import cn.zhicloud.module.erp.service.stock.vmi.ErpVmiReplenishmentService;
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

@Tag(name = "管理后台 - ERP VMI 补货建议")
@RestController
@RequestMapping("/erp/vmi-replenishment")
@Validated
public class ErpVmiReplenishmentController {

    @Resource
    private ErpVmiReplenishmentService vmiReplenishmentService;

    @DeleteMapping("/delete")
    @Operation(summary = "删除 VMI 补货建议")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:vmi-replenishment:delete')")
    public CommonResult<Boolean> deleteVmiReplenishment(@RequestParam("id") Long id) {
        vmiReplenishmentService.deleteVmiReplenishment(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得 VMI 补货建议")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:vmi-replenishment:query')")
    public CommonResult<ErpVmiReplenishmentRespVO> getVmiReplenishment(@RequestParam("id") Long id) {
        ErpVmiReplenishmentDO replenishment = vmiReplenishmentService.getVmiReplenishment(id);
        ErpVmiReplenishmentRespVO respVO = BeanUtils.toBean(replenishment, ErpVmiReplenishmentRespVO.class);
        if (respVO != null) {
            List<ErpVmiReplenishmentItemDO> items = vmiReplenishmentService.getVmiReplenishmentItemList(id);
            respVO.setItems(BeanUtils.toBean(items, ErpVmiReplenishmentItemRespVO.class));
        }
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得 VMI 补货建议分页")
    @PreAuthorize("@ss.hasPermission('erp:vmi-replenishment:query')")
    public CommonResult<PageResult<ErpVmiReplenishmentRespVO>> getVmiReplenishmentPage(
            @Valid ErpVmiReplenishmentPageReqVO pageReqVO) {
        PageResult<ErpVmiReplenishmentDO> pageResult = vmiReplenishmentService.getVmiReplenishmentPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpVmiReplenishmentRespVO.class));
    }

    @PostMapping("/generate")
    @Operation(summary = "生成补货建议")
    @PreAuthorize("@ss.hasPermission('erp:vmi-replenishment:create')")
    public CommonResult<List<Long>> generateReplenishment() {
        return success(vmiReplenishmentService.generateReplenishment());
    }

    @PostMapping("/convert-to-purchase-order")
    @Operation(summary = "将补货建议转换为采购订单")
    @Parameter(name = "id", description = "补货建议编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:vmi-replenishment:update')")
    public CommonResult<String> convertToPurchaseOrder(@RequestParam("id") Long id) {
        return success(vmiReplenishmentService.convertToPurchaseOrder(id));
    }

}
