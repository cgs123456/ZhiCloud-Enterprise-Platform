package cn.iocoder.yudao.module.wms.controller.admin.order.check;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.wms.controller.admin.order.check.vo.cycle.WmsCheckCyclePlanPageReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.order.check.vo.cycle.WmsCheckCyclePlanRespVO;
import cn.iocoder.yudao.module.wms.controller.admin.order.check.vo.cycle.WmsCheckCyclePlanSaveReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.warehouse.WmsWarehouseDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.check.WmsCheckCyclePlanDO;
import cn.iocoder.yudao.module.wms.service.md.warehouse.WmsWarehouseService;
import cn.iocoder.yudao.module.wms.service.order.check.WmsCheckCyclePlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

/**
 * WMS 循环盘点计划 Controller
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - WMS 循环盘点计划")
@RestController
@RequestMapping("/wms/check-cycle-plan")
@Validated
public class WmsCheckCyclePlanController {

    @Resource
    private WmsCheckCyclePlanService checkCyclePlanService;
    @Resource
    private WmsWarehouseService warehouseService;

    @PostMapping("/create")
    @Operation(summary = "创建循环盘点计划")
    @PreAuthorize("@ss.hasPermission('wms:check-cycle-plan:create')")
    public CommonResult<Long> create(@Valid @RequestBody WmsCheckCyclePlanSaveReqVO createReqVO) {
        return success(checkCyclePlanService.createCheckCyclePlan(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新循环盘点计划")
    @PreAuthorize("@ss.hasPermission('wms:check-cycle-plan:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody WmsCheckCyclePlanSaveReqVO updateReqVO) {
        checkCyclePlanService.updateCheckCyclePlan(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除循环盘点计划")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('wms:check-cycle-plan:delete')")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        checkCyclePlanService.deleteCheckCyclePlan(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得循环盘点计划")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('wms:check-cycle-plan:query')")
    public CommonResult<WmsCheckCyclePlanRespVO> get(@RequestParam("id") Long id) {
        WmsCheckCyclePlanDO plan = checkCyclePlanService.getCheckCyclePlan(id);
        return success(buildRespVO(plan));
    }

    @GetMapping("/page")
    @Operation(summary = "获得循环盘点计划分页")
    @PreAuthorize("@ss.hasPermission('wms:check-cycle-plan:query')")
    public CommonResult<PageResult<WmsCheckCyclePlanRespVO>> page(@Valid WmsCheckCyclePlanPageReqVO pageReqVO) {
        PageResult<WmsCheckCyclePlanDO> pageResult = checkCyclePlanService.getCheckCyclePlanPage(pageReqVO);
        return success(new PageResult<>(buildRespVOList(pageResult.getList()), pageResult.getTotal()));
    }

    private WmsCheckCyclePlanRespVO buildRespVO(WmsCheckCyclePlanDO plan) {
        if (plan == null) {
            return null;
        }
        List<WmsCheckCyclePlanRespVO> list = buildRespVOList(Collections.singletonList(plan));
        return list.isEmpty() ? null : list.get(0);
    }

    private List<WmsCheckCyclePlanRespVO> buildRespVOList(List<WmsCheckCyclePlanDO> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, WmsWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(
                convertSet(list, WmsCheckCyclePlanDO::getWarehouseId));
        return BeanUtils.toBean(list, WmsCheckCyclePlanRespVO.class, vo ->
                MapUtils.findAndThen(warehouseMap, vo.getWarehouseId(),
                        warehouse -> vo.setWarehouseName(warehouse.getName())));
    }

}