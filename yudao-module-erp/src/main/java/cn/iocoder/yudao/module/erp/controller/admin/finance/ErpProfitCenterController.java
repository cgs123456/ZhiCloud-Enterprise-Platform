package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.profitcenter.ErpProfitCenterPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.profitcenter.ErpProfitCenterRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.profitcenter.ErpProfitCenterSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpProfitCenterDO;
import cn.iocoder.yudao.module.erp.service.finance.ErpProfitCenterService;
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

@Tag(name = "管理后台 - ERP 利润中心")
@RestController
@RequestMapping("/erp/profit-center")
@Validated
public class ErpProfitCenterController {

    @Resource
    private ErpProfitCenterService profitCenterService;

    @PostMapping("/create")
    @Operation(summary = "创建利润中心")
    @PreAuthorize("@ss.hasPermission('erp:profit-center:create')")
    public CommonResult<Long> createProfitCenter(@Valid @RequestBody ErpProfitCenterSaveReqVO createReqVO) {
        return success(profitCenterService.createProfitCenter(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新利润中心")
    @PreAuthorize("@ss.hasPermission('erp:profit-center:update')")
    public CommonResult<Boolean> updateProfitCenter(@Valid @RequestBody ErpProfitCenterSaveReqVO updateReqVO) {
        profitCenterService.updateProfitCenter(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除利润中心")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:profit-center:delete')")
    public CommonResult<Boolean> deleteProfitCenter(@RequestParam("id") Long id) {
        profitCenterService.deleteProfitCenter(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得利润中心")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:profit-center:query')")
    public CommonResult<ErpProfitCenterRespVO> getProfitCenter(@RequestParam("id") Long id) {
        ErpProfitCenterDO profitCenter = profitCenterService.getProfitCenter(id);
        return success(BeanUtils.toBean(profitCenter, ErpProfitCenterRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得利润中心分页")
    @PreAuthorize("@ss.hasPermission('erp:profit-center:query')")
    public CommonResult<PageResult<ErpProfitCenterRespVO>> getProfitCenterPage(@Valid ErpProfitCenterPageReqVO pageReqVO) {
        PageResult<ErpProfitCenterDO> pageResult = profitCenterService.getProfitCenterPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpProfitCenterRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得利润中心列表", description = "用于前端构建树形结构")
    @PreAuthorize("@ss.hasPermission('erp:profit-center:query')")
    public CommonResult<List<ErpProfitCenterRespVO>> getProfitCenterList() {
        List<ErpProfitCenterDO> list = profitCenterService.getProfitCenterList();
        return success(convertList(list, center -> BeanUtils.toBean(center, ErpProfitCenterRespVO.class)));
    }

}
