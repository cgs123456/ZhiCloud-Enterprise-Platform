package cn.iocoder.yudao.module.qms.controller.admin.inspectionorder;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.inspectionorder.vo.FqcInspectionOrderCreateReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.inspectionorder.vo.InspectionOrderPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.inspectionorder.vo.InspectionOrderRespVO;
import cn.iocoder.yudao.module.qms.controller.admin.inspectionorder.vo.InspectionOrderSaveReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.inspectionrecord.vo.InspectionRecordSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.inspectionorder.InspectionOrderDO;
import cn.iocoder.yudao.module.qms.enums.qms.InspectionTypeEnum;
import cn.iocoder.yudao.module.qms.service.inspectionorder.InspectionOrderService;
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

@Tag(name = "管理后台 - QMS 检验单")
@RestController
@RequestMapping("/qms/inspection-order")
@Validated
public class InspectionOrderController {

    @Resource
    private InspectionOrderService inspectionOrderService;

    @PostMapping("/create")
    @Operation(summary = "创建检验单")
    @PreAuthorize("@ss.hasPermission('qms:inspection-order:create')")
    public CommonResult<Long> createInspectionOrder(@Valid @RequestBody InspectionOrderSaveReqVO createReqVO) {
        return success(inspectionOrderService.createInspectionOrder(createReqVO));
    }

    @PostMapping("/create-fqc")
    @Operation(summary = "创建 FQC 成品检验单", description = "检验类型固定为 FQC(35)，必须关联成品工单 ID 与产品 ID")
    @PreAuthorize("@ss.hasPermission('qms:inspection-order:create')")
    public CommonResult<Long> createFqcInspectionOrder(@Valid @RequestBody FqcInspectionOrderCreateReqVO createReqVO) {
        return success(inspectionOrderService.createFqcInspectionOrder(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新检验单")
    @PreAuthorize("@ss.hasPermission('qms:inspection-order:update')")
    public CommonResult<Boolean> updateInspectionOrder(@Valid @RequestBody InspectionOrderSaveReqVO updateReqVO) {
        inspectionOrderService.updateInspectionOrder(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除检验单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('qms:inspection-order:delete')")
    public CommonResult<Boolean> deleteInspectionOrder(@RequestParam("id") Long id) {
        inspectionOrderService.deleteInspectionOrder(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得检验单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:inspection-order:query')")
    public CommonResult<InspectionOrderRespVO> getInspectionOrder(@RequestParam("id") Long id) {
        InspectionOrderDO inspectionOrder = inspectionOrderService.getInspectionOrder(id);
        return success(BeanUtils.toBean(inspectionOrder, InspectionOrderRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得检验单分页")
    @PreAuthorize("@ss.hasPermission('qms:inspection-order:query')")
    public CommonResult<PageResult<InspectionOrderRespVO>> getInspectionOrderPage(@Valid InspectionOrderPageReqVO pageReqVO) {
        PageResult<InspectionOrderDO> pageResult = inspectionOrderService.getInspectionOrderPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, InspectionOrderRespVO.class));
    }

    @GetMapping("/page-fqc")
    @Operation(summary = "获得 FQC 成品检验单分页", description = "按检验类型 FQC(35) 筛选分页")
    @PreAuthorize("@ss.hasPermission('qms:inspection-order:query')")
    public CommonResult<PageResult<InspectionOrderRespVO>> getFqcInspectionOrderPage(@Valid InspectionOrderPageReqVO pageReqVO) {
        pageReqVO.setType(InspectionTypeEnum.FQC.getType());
        PageResult<InspectionOrderDO> pageResult = inspectionOrderService.getInspectionOrderPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, InspectionOrderRespVO.class));
    }

    @PostMapping("/submit")
    @Operation(summary = "提交检验结果", description = "提交检验记录，系统自动计算检验单 PASS/FAIL 状态")
    @Parameter(name = "orderId", description = "检验单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('qms:inspection-order:submit')")
    public CommonResult<Boolean> submitInspection(@RequestParam("orderId") Long orderId,
                                                  @Valid @RequestBody List<InspectionRecordSaveReqVO> records) {
        inspectionOrderService.submitInspection(orderId, records);
        return success(true);
    }

}