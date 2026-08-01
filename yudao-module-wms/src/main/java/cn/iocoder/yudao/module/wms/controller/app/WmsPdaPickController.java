package cn.iocoder.yudao.module.wms.controller.app;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.wms.controller.app.vo.WmsPdaConfirmPackReqVO;
import cn.iocoder.yudao.module.wms.controller.app.vo.WmsPdaConfirmPickReqVO;
import cn.iocoder.yudao.module.wms.controller.app.vo.WmsPdaMyPickTaskRespVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.item.WmsItemDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.item.WmsItemSkuDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.pickstrategy.WmsPickTaskDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.shipment.WmsShipmentOrderDO;
import cn.iocoder.yudao.module.wms.dal.mysql.order.shipment.WmsShipmentOrderMapper;
import cn.iocoder.yudao.module.wms.service.md.item.WmsItemService;
import cn.iocoder.yudao.module.wms.service.md.item.WmsItemSkuService;
import cn.iocoder.yudao.module.wms.service.order.pickstrategy.WmsPickTaskService;
import cn.iocoder.yudao.module.wms.service.order.shipment.WmsShipmentOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

/**
 * WMS PDA 拣货作业 Controller
 *
 * @author 芋道源码
 */
@Tag(name = "用户 App - WMS PDA 拣货作业")
@RestController
@RequestMapping("/wms-api/pick")
@Validated
@PermitAll
public class WmsPdaPickController {

    @Resource
    private WmsPickTaskService pickTaskService;
    @Resource
    private WmsShipmentOrderService shipmentOrderService;
    @Resource
    private WmsShipmentOrderMapper shipmentOrderMapper;
    @Resource
    private WmsItemSkuService itemSkuService;
    @Resource
    private WmsItemService itemService;

    @GetMapping("/my-pick-tasks")
    @Operation(summary = "我的拣货任务")
    @Parameter(name = "pickerUserId", description = "拣货员用户编号", required = true, example = "1")
    public CommonResult<List<WmsPdaMyPickTaskRespVO>> getMyPickTasks(
            @RequestParam("pickerUserId") Long pickerUserId) {
        List<WmsPickTaskDO> list = pickTaskService.getMyPickTasks(pickerUserId);
        if (CollUtil.isEmpty(list)) {
            return success(Collections.emptyList());
        }
        Map<Long, WmsShipmentOrderDO> orderMap = shipmentOrderMapper.selectBatchIds(
                convertSet(list, WmsPickTaskDO::getShipmentOrderId)).stream()
                .collect(java.util.stream.Collectors.toMap(WmsShipmentOrderDO::getId, o -> o));
        Map<Long, WmsItemSkuDO> skuMap = itemSkuService.getItemSkuMap(
                convertSet(list, WmsPickTaskDO::getSkuId));
        Map<Long, WmsItemDO> itemMap = itemService.getItemMap(
                convertSet(skuMap.values(), WmsItemSkuDO::getItemId));
        List<WmsPdaMyPickTaskRespVO> respVOs = new ArrayList<>();
        for (WmsPickTaskDO task : list) {
            WmsPdaMyPickTaskRespVO vo = BeanUtils.toBean(task, WmsPdaMyPickTaskRespVO.class);
            if (task.getShipmentOrderId() != null && orderMap.containsKey(task.getShipmentOrderId())) {
                vo.setShipmentOrderNo(orderMap.get(task.getShipmentOrderId()).getNo());
            }
            WmsItemSkuDO sku = skuMap.get(task.getSkuId());
            if (sku != null) {
                vo.setSkuCode(sku.getCode()).setSkuName(sku.getName());
                WmsItemDO item = itemMap.get(sku.getItemId());
                if (item != null) {
                    vo.setProductName(item.getName());
                }
            }
            respVOs.add(vo);
        }
        return success(respVOs);
    }

    @PostMapping("/confirm-pick")
    @Operation(summary = "确认拣货")
    public CommonResult<Boolean> confirmPick(@Valid @RequestBody WmsPdaConfirmPickReqVO reqVO) {
        pickTaskService.confirmPick(reqVO.getTaskId(), reqVO.getPickedQuantity());
        return success(true);
    }

    @PostMapping("/confirm-pack")
    @Operation(summary = "确认打包")
    public CommonResult<Boolean> confirmPack(@Valid @RequestBody WmsPdaConfirmPackReqVO reqVO) {
        shipmentOrderService.pack(reqVO.getShipmentOrderId());
        return success(true);
    }

}
