package cn.iocoder.yudao.module.mes.controller.admin.pro.rework;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.rework.vo.MesProReworkOrderCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.rework.vo.MesProReworkOrderPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.rework.vo.MesProReworkOrderRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.rework.vo.MesProReworkOrderSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.rework.MesProReworkOrderDO;
import cn.iocoder.yudao.module.mes.service.pro.rework.MesProReworkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES 返工工单")
@RestController
@RequestMapping("/mes/pro/rework-order")
@Validated
public class MesProReworkOrderController {

    @Resource
    private MesProReworkOrderService reworkOrderService;

    @PostMapping("/create")
    @Operation(summary = "基于原工单创建返工工单")
    @PreAuthorize("@ss.hasPermission('mes:pro:rework-order:create')")
    public CommonResult<Long> createReworkOrder(@Valid @RequestBody MesProReworkOrderCreateReqVO createReqVO) {
        return success(reworkOrderService.createReworkOrder(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新返工工单")
    @PreAuthorize("@ss.hasPermission('mes:pro:rework-order:update')")
    public CommonResult<Boolean> updateReworkOrder(@Valid @RequestBody MesProReworkOrderSaveReqVO updateReqVO) {
        reworkOrderService.updateReworkOrder(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除返工工单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro:rework-order:delete')")
    public CommonResult<Boolean> deleteReworkOrder(@RequestParam("id") Long id) {
        reworkOrderService.deleteReworkOrder(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得返工工单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('mes:pro:rework-order:query')")
    public CommonResult<MesProReworkOrderRespVO> getReworkOrder(@RequestParam("id") Long id) {
        MesProReworkOrderDO reworkOrder = reworkOrderService.getReworkOrder(id);
        if (reworkOrder == null) {
            return success(null);
        }
        return success(buildReworkOrderRespVOList(ListUtil.of(reworkOrder)).get(0));
    }

    @GetMapping("/page")
    @Operation(summary = "获得返工工单分页")
    @PreAuthorize("@ss.hasPermission('mes:pro:rework-order:query')")
    public CommonResult<PageResult<MesProReworkOrderRespVO>> getReworkOrderPage(@Valid MesProReworkOrderPageReqVO pageReqVO) {
        PageResult<MesProReworkOrderDO> pageResult = reworkOrderService.getReworkOrderPage(pageReqVO);
        return success(new PageResult<>(buildReworkOrderRespVOList(pageResult.getList()), pageResult.getTotal()));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出返工工单 Excel")
    @PreAuthorize("@ss.hasPermission('mes:pro:rework-order:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportReworkOrderExcel(@Valid MesProReworkOrderPageReqVO pageReqVO,
                                      HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<MesProReworkOrderDO> list = reworkOrderService.getReworkOrderPage(pageReqVO).getList();
        List<MesProReworkOrderRespVO> voList = buildReworkOrderRespVOList(list);
        ExcelUtils.write(response, "返工工单.xls", "数据", MesProReworkOrderRespVO.class, voList);
    }

    @PutMapping("/start")
    @Operation(summary = "开工返工")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro:rework-order:update')")
    public CommonResult<Boolean> startRework(@RequestParam("id") Long id) {
        reworkOrderService.startRework(id);
        return success(true);
    }

    @PutMapping("/complete")
    @Operation(summary = "完工返工")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro:rework-order:update')")
    public CommonResult<Boolean> completeRework(@RequestParam("id") Long id) {
        reworkOrderService.completeRework(id);
        return success(true);
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消返工")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro:rework-order:update')")
    public CommonResult<Boolean> cancelRework(@RequestParam("id") Long id) {
        reworkOrderService.cancelRework(id);
        return success(true);
    }

    // ==================== 拼接 VO ====================

    private List<MesProReworkOrderRespVO> buildReworkOrderRespVOList(List<MesProReworkOrderDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 返工工单主表已冗余产品编码/名称/原工单号等字段，无需再拼接关联数据
        return BeanUtils.toBean(list, MesProReworkOrderRespVO.class);
    }

}
