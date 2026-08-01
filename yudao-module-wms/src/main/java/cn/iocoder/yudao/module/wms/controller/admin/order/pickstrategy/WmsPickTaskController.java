package cn.iocoder.yudao.module.wms.controller.admin.order.pickstrategy;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.wms.controller.admin.order.pickstrategy.vo.WmsPickTaskPageReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.order.pickstrategy.vo.WmsPickTaskRespVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.item.WmsItemDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.item.WmsItemSkuDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.pickstrategy.WmsPickTaskDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.shipment.WmsShipmentOrderDO;
import cn.iocoder.yudao.module.wms.dal.mysql.order.shipment.WmsShipmentOrderMapper;
import cn.iocoder.yudao.module.wms.service.md.item.WmsItemService;
import cn.iocoder.yudao.module.wms.service.md.item.WmsItemSkuService;
import cn.iocoder.yudao.module.wms.service.order.pickstrategy.WmsPickStrategyService;
import cn.iocoder.yudao.module.wms.service.order.pickstrategy.WmsPickTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSetByFlatMap;

/**
 * WMS 拣货任务 Controller
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - WMS 拣货任务")
@RestController
@RequestMapping("/wms/pick-task")
@Validated
public class WmsPickTaskController {

    @Resource
    private WmsPickTaskService pickTaskService;
    @Resource
    private WmsPickStrategyService pickStrategyService;
    @Resource
    private WmsShipmentOrderMapper shipmentOrderMapper;
    @Resource
    private WmsItemSkuService itemSkuService;
    @Resource
    private WmsItemService itemService;
    @Resource
    private AdminUserApi adminUserApi;

    @GetMapping("/page")
    @Operation(summary = "获得拣货任务分页")
    @PreAuthorize("@ss.hasPermission('wms:pick-task:query')")
    public CommonResult<PageResult<WmsPickTaskRespVO>> getPickTaskPage(@Valid WmsPickTaskPageReqVO pageReqVO) {
        PageResult<WmsPickTaskDO> pageResult = pickTaskService.getPickTaskPage(pageReqVO);
        if (pageResult.getList().isEmpty()) {
            return success(new PageResult<>(Collections.emptyList(), pageResult.getTotal()));
        }
        return success(new PageResult<>(buildPickTaskRespVOList(pageResult.getList()), pageResult.getTotal()));
    }

    @PostMapping("/generate")
    @Operation(summary = "生成拣货任务")
    @Parameter(name = "shipmentOrderId", description = "出库单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('wms:pick-task:create')")
    public CommonResult<List<Long>> generatePickTasks(@RequestParam("shipmentOrderId") Long shipmentOrderId) {
        return success(pickStrategyService.generatePickTasks(shipmentOrderId));
    }

    @PutMapping("/confirm-pick")
    @Operation(summary = "确认拣货")
    @Parameter(name = "taskId", description = "拣货任务编号", required = true, example = "1024")
    @Parameter(name = "pickedQuantity", description = "已拣数量", required = true, example = "100.00")
    @PreAuthorize("@ss.hasPermission('wms:pick-task:update')")
    public CommonResult<Boolean> confirmPick(@RequestParam("taskId") Long taskId,
                                             @RequestParam("pickedQuantity") BigDecimal pickedQuantity) {
        pickTaskService.confirmPick(taskId, pickedQuantity);
        return success(true);
    }

    // ==================== 拼接 VO ====================

    private List<WmsPickTaskRespVO> buildPickTaskRespVOList(List<WmsPickTaskDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        Map<Long, WmsShipmentOrderDO> orderMap = shipmentOrderMapper.selectBatchIds(
                convertSet(list, WmsPickTaskDO::getShipmentOrderId)).stream()
                .collect(java.util.stream.Collectors.toMap(WmsShipmentOrderDO::getId, o -> o));
        Map<Long, WmsItemSkuDO> skuMap = itemSkuService.getItemSkuMap(
                convertSet(list, WmsPickTaskDO::getSkuId));
        Map<Long, WmsItemDO> itemMap = itemService.getItemMap(
                convertSet(skuMap.values(), WmsItemSkuDO::getItemId));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(convertSetByFlatMap(list,
                t -> Stream.of(t.getPickerUserId())));
        return BeanUtils.toBean(list, WmsPickTaskRespVO.class, vo -> {
            if (vo.getShipmentOrderId() != null && orderMap.containsKey(vo.getShipmentOrderId())) {
                vo.setShipmentOrderNo(orderMap.get(vo.getShipmentOrderId()).getNo());
            }
            if (vo.getSkuId() != null && skuMap.containsKey(vo.getSkuId())) {
                WmsItemSkuDO sku = skuMap.get(vo.getSkuId());
                vo.setSkuCode(sku.getCode()).setSkuName(sku.getName());
                if (sku.getItemId() != null && itemMap.containsKey(sku.getItemId())) {
                    vo.setProductName(itemMap.get(sku.getItemId()).getName());
                }
            }
            if (vo.getPickerUserId() != null && userMap.containsKey(vo.getPickerUserId())) {
                vo.setPickerUserName(userMap.get(vo.getPickerUserId()).getNickname());
            }
        });
    }

}
