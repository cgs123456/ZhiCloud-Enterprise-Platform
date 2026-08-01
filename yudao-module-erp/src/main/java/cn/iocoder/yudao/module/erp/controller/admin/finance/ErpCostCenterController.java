package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.costcenter.ErpCostCenterPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.costcenter.ErpCostCenterRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.costcenter.ErpCostCenterSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpCostCenterDO;
import cn.iocoder.yudao.module.erp.service.finance.ErpCostCenterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;

@Tag(name = "管理后台 - ERP 成本中心")
@RestController
@RequestMapping("/erp/cost-center")
@Validated
public class ErpCostCenterController {

    @Resource
    private ErpCostCenterService costCenterService;

    @PostMapping("/create")
    @Operation(summary = "创建成本中心")
    @PreAuthorize("@ss.hasPermission('erp:cost-center:create')")
    public CommonResult<Long> createCostCenter(@Valid @RequestBody ErpCostCenterSaveReqVO createReqVO) {
        return success(costCenterService.createCostCenter(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新成本中心")
    @PreAuthorize("@ss.hasPermission('erp:cost-center:update')")
    public CommonResult<Boolean> updateCostCenter(@Valid @RequestBody ErpCostCenterSaveReqVO updateReqVO) {
        costCenterService.updateCostCenter(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除成本中心")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:cost-center:delete')")
    public CommonResult<Boolean> deleteCostCenter(@RequestParam("id") Long id) {
        costCenterService.deleteCostCenter(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得成本中心")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:cost-center:query')")
    public CommonResult<ErpCostCenterRespVO> getCostCenter(@RequestParam("id") Long id) {
        ErpCostCenterDO costCenter = costCenterService.getCostCenter(id);
        return success(BeanUtils.toBean(costCenter, ErpCostCenterRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得成本中心分页")
    @PreAuthorize("@ss.hasPermission('erp:cost-center:query')")
    public CommonResult<PageResult<ErpCostCenterRespVO>> getCostCenterPage(@Valid ErpCostCenterPageReqVO pageReqVO) {
        PageResult<ErpCostCenterDO> pageResult = costCenterService.getCostCenterPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpCostCenterRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得成本中心列表", description = "用于前端构建树形结构")
    @PreAuthorize("@ss.hasPermission('erp:cost-center:query')")
    public CommonResult<List<ErpCostCenterRespVO>> getCostCenterList() {
        List<ErpCostCenterDO> list = costCenterService.getCostCenterList();
        return success(convertList(list, center -> BeanUtils.toBean(center, ErpCostCenterRespVO.class)));
    }

}
