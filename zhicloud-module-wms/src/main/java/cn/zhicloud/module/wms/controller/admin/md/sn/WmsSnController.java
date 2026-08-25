package cn.zhicloud.module.wms.controller.admin.md.sn;

import cn.hutool.core.collection.CollUtil;
import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.wms.controller.admin.md.sn.vo.WmsSnGenerateReqVO;
import cn.zhicloud.module.wms.controller.admin.md.sn.vo.WmsSnPageReqVO;
import cn.zhicloud.module.wms.controller.admin.md.sn.vo.WmsSnRespVO;
import cn.zhicloud.module.wms.controller.admin.md.sn.vo.WmsSnSaveReqVO;
import cn.zhicloud.module.wms.controller.admin.md.sn.vo.WmsSnTraceRespVO;
import cn.zhicloud.module.wms.dal.dataobject.md.item.WmsItemDO;
import cn.zhicloud.module.wms.dal.dataobject.md.sn.WmsSnDO;
import cn.zhicloud.module.wms.service.md.item.WmsItemService;
import cn.zhicloud.module.wms.service.md.sn.WmsSnService;
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

import static cn.zhicloud.framework.common.pojo.CommonResult.success;
import static cn.zhicloud.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "管理后台 - WMS 序列号")
@RestController
@RequestMapping("/wms/sn")
@Validated
public class WmsSnController {

    @Resource
    private WmsSnService snService;
    @Resource
    private WmsItemService itemService;

    @PostMapping("/generate")
    @Operation(summary = "批量生成序列号")
    @PreAuthorize("@ss.hasPermission('wms:sn:create')")
    public CommonResult<List<WmsSnRespVO>> generateSnList(@Valid @RequestBody WmsSnGenerateReqVO reqVO) {
        List<WmsSnDO> list = snService.generateSnList(reqVO);
        return success(buildRespVOList(list));
    }

    @PostMapping("/create")
    @Operation(summary = "创建序列号")
    @PreAuthorize("@ss.hasPermission('wms:sn:create')")
    public CommonResult<Long> createSn(@Valid @RequestBody WmsSnSaveReqVO createReqVO) {
        return success(snService.createSn(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新序列号")
    @PreAuthorize("@ss.hasPermission('wms:sn:update')")
    public CommonResult<Boolean> updateSn(@Valid @RequestBody WmsSnSaveReqVO updateReqVO) {
        snService.updateSn(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除序列号")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('wms:sn:delete')")
    public CommonResult<Boolean> deleteSn(@RequestParam("id") Long id) {
        snService.deleteSn(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得序列号")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('wms:sn:query')")
    public CommonResult<WmsSnRespVO> getSn(@RequestParam("id") Long id) {
        WmsSnDO sn = snService.getSn(id);
        return success(buildRespVO(sn));
    }

    @GetMapping("/get-by-sn")
    @Operation(summary = "按序列号字符串获取")
    @Parameter(name = "sn", description = "序列号", required = true, example = "SN20260730001")
    @PreAuthorize("@ss.hasPermission('wms:sn:query')")
    public CommonResult<WmsSnRespVO> getSnBySn(@RequestParam("sn") String sn) {
        WmsSnDO snDO = snService.getSnBySn(sn);
        return success(buildRespVO(snDO));
    }

    @GetMapping("/page")
    @Operation(summary = "获得序列号分页")
    @PreAuthorize("@ss.hasPermission('wms:sn:query')")
    public CommonResult<PageResult<WmsSnRespVO>> getSnPage(@Valid WmsSnPageReqVO pageReqVO) {
        PageResult<WmsSnDO> pageResult = snService.getSnPage(pageReqVO);
        return success(new PageResult<>(buildRespVOList(pageResult.getList()), pageResult.getTotal()));
    }

    @PutMapping("/bind")
    @Operation(summary = "序列号入库绑定")
    @PreAuthorize("@ss.hasPermission('wms:sn:update')")
    public CommonResult<Boolean> bindInventory(@RequestParam("snId") Long snId,
                                                @RequestParam(value = "inventoryId", required = false) Long inventoryId,
                                                @RequestParam(value = "batchId", required = false) Long batchId,
                                                @RequestParam(value = "warehouseId", required = false) Long warehouseId,
                                                @RequestParam(value = "zoneId", required = false) Long zoneId,
                                                @RequestParam(value = "locationId", required = false) Long locationId,
                                                @RequestParam(value = "inboundOrderId", required = false) Long inboundOrderId) {
        snService.bindInventory(snId, inventoryId, batchId, warehouseId, zoneId, locationId, inboundOrderId);
        return success(true);
    }

    @PutMapping("/ship")
    @Operation(summary = "序列号出库解绑")
    @PreAuthorize("@ss.hasPermission('wms:sn:update')")
    public CommonResult<Boolean> unbindAndShip(@RequestParam("snId") Long snId,
                                                @RequestParam("outboundOrderId") Long outboundOrderId) {
        snService.unbindAndShip(snId, outboundOrderId);
        return success(true);
    }

    @PutMapping("/return")
    @Operation(summary = "序列号退货")
    @PreAuthorize("@ss.hasPermission('wms:sn:update')")
    public CommonResult<Boolean> returnSn(@RequestParam("snId") Long snId,
                                          @RequestParam(value = "warehouseId", required = false) Long warehouseId,
                                          @RequestParam(value = "locationId", required = false) Long locationId) {
        snService.returnSn(snId, warehouseId, locationId);
        return success(true);
    }

    @GetMapping("/trace")
    @Operation(summary = "序列号追溯")
    @Parameter(name = "snId", description = "序列号编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('wms:sn:query')")
    public CommonResult<WmsSnTraceRespVO> trace(@RequestParam("snId") Long snId) {
        return success(snService.trace(snId));
    }

    // ==================== 拼接 VO ====================

    private WmsSnRespVO buildRespVO(WmsSnDO sn) {
        if (sn == null) {
            return null;
        }
        WmsSnRespVO vo = BeanUtils.toBean(sn, WmsSnRespVO.class);
        fillProductInfo(vo, sn.getProductId());
        return vo;
    }

    private List<WmsSnRespVO> buildRespVOList(List<WmsSnDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        Map<Long, WmsItemDO> itemMap = itemService.getItemMap(
                convertSet(list, WmsSnDO::getProductId));
        List<WmsSnRespVO> voList = BeanUtils.toBean(list, WmsSnRespVO.class);
        for (WmsSnRespVO vo : voList) {
            WmsItemDO item = itemMap.get(vo.getProductId());
            if (item != null) {
                vo.setProductCode(item.getCode());
                vo.setProductName(item.getName());
            }
        }
        return voList;
    }

    private void fillProductInfo(WmsSnRespVO vo, Long productId) {
        if (productId == null) {
            return;
        }
        WmsItemDO item = itemService.getItem(productId);
        if (item != null) {
            vo.setProductCode(item.getCode());
            vo.setProductName(item.getName());
        }
    }

}