package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.standardcost.ErpStandardCostPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.standardcost.ErpStandardCostRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.standardcost.ErpStandardCostSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.cost.ErpStandardCostDO;
import cn.iocoder.yudao.module.erp.service.finance.cost.ErpStandardCostService;
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

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;

@Tag(name = "管理后台 - ERP 标准成本")
@RestController
@RequestMapping("/erp/standard-cost")
@Validated
public class ErpStandardCostController {

    @Resource
    private ErpStandardCostService standardCostService;

    @PostMapping("/create")
    @Operation(summary = "创建标准成本")
    @PreAuthorize("@ss.hasPermission('erp:standard-cost:create')")
    public CommonResult<Long> createStandardCost(@Valid @RequestBody ErpStandardCostSaveReqVO createReqVO) {
        return success(standardCostService.createStandardCost(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新标准成本")
    @PreAuthorize("@ss.hasPermission('erp:standard-cost:update')")
    public CommonResult<Boolean> updateStandardCost(@Valid @RequestBody ErpStandardCostSaveReqVO updateReqVO) {
        standardCostService.updateStandardCost(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除标准成本")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:standard-cost:delete')")
    public CommonResult<Boolean> deleteStandardCost(@RequestParam("id") Long id) {
        standardCostService.deleteStandardCost(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得标准成本")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:standard-cost:query')")
    public CommonResult<ErpStandardCostRespVO> getStandardCost(@RequestParam("id") Long id) {
        ErpStandardCostDO standardCost = standardCostService.getStandardCost(id);
        return success(BeanUtils.toBean(standardCost, ErpStandardCostRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得标准成本分页")
    @PreAuthorize("@ss.hasPermission('erp:standard-cost:query')")
    public CommonResult<PageResult<ErpStandardCostRespVO>> getStandardCostPage(@Valid ErpStandardCostPageReqVO pageReqVO) {
        PageResult<ErpStandardCostDO> pageResult = standardCostService.getStandardCostPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpStandardCostRespVO.class));
    }

    @GetMapping("/effective-list")
    @Operation(summary = "获得产品在某天生效的标准成本列表")
    @PreAuthorize("@ss.hasPermission('erp:standard-cost:query')")
    public CommonResult<List<ErpStandardCostRespVO>> getEffectiveStandardCostList(
            @RequestParam("productId") Long productId,
            @RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        List<ErpStandardCostDO> list = standardCostService.getEffectiveStandardCostList(productId, date);
        return success(convertList(list, item -> BeanUtils.toBean(item, ErpStandardCostRespVO.class)));
    }

}
