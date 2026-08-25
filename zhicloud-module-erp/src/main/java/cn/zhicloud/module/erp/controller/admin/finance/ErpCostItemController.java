package cn.zhicloud.module.erp.controller.admin.finance;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.finance.vo.costitem.ErpCostItemPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.costitem.ErpCostItemRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.costitem.ErpCostItemSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.cost.ErpCostItemDO;
import cn.zhicloud.module.erp.service.finance.cost.ErpCostItemService;
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
import static cn.zhicloud.framework.common.util.collection.CollectionUtils.convertList;

@Tag(name = "管理后台 - ERP 成本项目")
@RestController
@RequestMapping("/erp/cost-item")
@Validated
public class ErpCostItemController {

    @Resource
    private ErpCostItemService costItemService;

    @PostMapping("/create")
    @Operation(summary = "创建成本项目")
    @PreAuthorize("@ss.hasPermission('erp:cost-item:create')")
    public CommonResult<Long> createCostItem(@Valid @RequestBody ErpCostItemSaveReqVO createReqVO) {
        return success(costItemService.createCostItem(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新成本项目")
    @PreAuthorize("@ss.hasPermission('erp:cost-item:update')")
    public CommonResult<Boolean> updateCostItem(@Valid @RequestBody ErpCostItemSaveReqVO updateReqVO) {
        costItemService.updateCostItem(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除成本项目")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:cost-item:delete')")
    public CommonResult<Boolean> deleteCostItem(@RequestParam("id") Long id) {
        costItemService.deleteCostItem(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得成本项目")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:cost-item:query')")
    public CommonResult<ErpCostItemRespVO> getCostItem(@RequestParam("id") Long id) {
        ErpCostItemDO costItem = costItemService.getCostItem(id);
        return success(BeanUtils.toBean(costItem, ErpCostItemRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得成本项目分页")
    @PreAuthorize("@ss.hasPermission('erp:cost-item:query')")
    public CommonResult<PageResult<ErpCostItemRespVO>> getCostItemPage(@Valid ErpCostItemPageReqVO pageReqVO) {
        PageResult<ErpCostItemDO> pageResult = costItemService.getCostItemPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpCostItemRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得启用的成本项目列表")
    @PreAuthorize("@ss.hasPermission('erp:cost-item:query')")
    public CommonResult<List<ErpCostItemRespVO>> getCostItemList(@RequestParam(value = "status", required = false) Integer status) {
        List<ErpCostItemDO> list = costItemService.getCostItemListByStatus(status);
        return success(convertList(list, item -> BeanUtils.toBean(item, ErpCostItemRespVO.class)));
    }

}
